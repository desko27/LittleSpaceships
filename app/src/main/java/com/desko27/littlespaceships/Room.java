package com.desko27.littlespaceships;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

public class Room extends View {

    // constants
    public static boolean debug_points = false, debug_boxes = false;
    public static int TOP = 0, RIGHT = 1, BOTTOM = 2, LEFT = 3;
    public static int EXPLOSION = 0, BLINK_ALARM = 12;

    // important booleans
    public Boolean easy_fire_mode;
    public boolean restart, started = false, started_touch = false, paused = false,
            kamikaze_mode = false;

    // loadable sprites & player instance
    public Sprites sprites;
    Player player;

    // arrays for all instances of game objects
    public final ArrayList<Object> instances = new ArrayList<>();
    public final ArrayList<Object> create_instances = new ArrayList<>();
    public final ArrayList<Object> destroy_instances = new ArrayList<>();
    public final HashMap<Class, ArrayList<Object>> instancesByClass = new HashMap<>();

    // collision engine - reusable detected methods per class
    public HashMap<Class, HashMap<Method,Class>> collision_methods = new HashMap<>();

    // the world/room properties
    public double room_scale;
    public int room_width = 720,
               room_height = 1230,
               width, height,
               background_speed = 3,
               background_position = 0;
    public Bitmap background = null;

    // interface properties
    public boolean touch_pressed = false, touch_hold = false;
    public int touch_x, touch_y;

    // draw variables
    public Paint paint = new Paint();
    public Rect rect = new Rect();

    // music & sound effects
    public MediaPlayer mediaPlayer;
    public int mediaPlayer_position = 0;
    public SoundPool soundPool;
    public HashMap<String, Integer> soundId = new HashMap<>();

    @SuppressWarnings("deprecation")
    public Room(Context context, AttributeSet attrs) {
        super(context, attrs);

        // load sound effects
        soundPool = new SoundPool(25, AudioManager.STREAM_MUSIC, 0);

        Class raw = R.raw.class;
        Field[] fields = raw.getFields();
        for (Field field : fields) {

            if (field.getName().equals("music")) continue;

            try {
                soundId.put(field.getName(), soundPool.load(getContext(), field.getInt(null), 1));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

    }
    public void start() {

        create_instances.clear();
        destroy_instances.clear();
        instances.clear();
        instancesByClass.clear();

        restart = false;
        paused = false;
        started = true;
        started_touch = false;

        // set scale
        /*double scale_x, scale_y;
        scale_x = (double) width  / (double) room_width;
        scale_y = (double) height / (double) room_height;
        room_scale = Math.min(scale_x, scale_y);*/

        // scaled witdh, adapted height for every device
        room_scale = (double) width  / (double) room_width;
        room_height = screen2room(height);

        // load sprites
        sprites = new Sprites(this);

        // starting instances
        instance_create(new Controller(this));
        player = new Player(this, room_width / 2, 2 * (room_height / 3));
        instance_create(player);
        instance_create(new PlayerShield(this, 0, 0));
        instance_create(new Title(this, room_width / 2, room_height / 3));
        instance_create(new Subtitle(this, room_width / 2, (room_height / 3) + 120));

        // load background
        // get non-resized background
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.background, o);

        // get scaled background
        background = Bitmap.createScaledBitmap(bitmap,
                room2screen(room_width), room2screen(room_height), false);

        // load music
        mediaPlayer = MediaPlayer.create(getContext(), R.raw.music);
        mediaPlayer.setLooping(true);
        mediaPlayer.setVolume(80, 80);
        mediaPlayer.start();

    }

    public void restart(boolean kamikaze_mode) {

        this.kamikaze_mode = kamikaze_mode;
        restart = true;
        mediaPlayer.release();
    }

    public void pause(boolean pause) {

        paused = pause;

        if (paused) {
            mediaPlayer.pause();
            mediaPlayer_position = mediaPlayer.getCurrentPosition();
        } else {
            mediaPlayer.seekTo(mediaPlayer_position);
            mediaPlayer.start();
        }

    }

    // instance creation/destruction global methods
    public void instance_destroy(Object instance) { destroy_instances.add(instance); }
    public Object instance_create(Object instance) {
        create_instances.add(instance);
        return instance;
    }

    public void play_sound(String id) {

        soundPool.play(soundId.get(id), 1, 1, 1, 0, 1);

    }

    // coordinates conversion functions
    public int room2screen(int room_coord) { return (int) (room_coord * room_scale); }
    public int screen2room(int screen_coord) { return (int) (screen_coord / room_scale); }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paint.setStrokeWidth(3);

        // background image
        if (background != null) {

            canvas.drawBitmap(background, 0, background_position, null);
            canvas.drawBitmap(background, 0, background_position - background.getHeight(), null);

            // background movement and loop
            background_position += background_speed;
            if (background_position > background.getHeight())
                background_position -= background.getHeight();
        }

        // flat background
        /*p.setColor(Color.BLACK);
        p.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawRect(new Rect(0, 0, room2screen(room_width),
                room2screen(room_height)), p);*/

        // print all the instances on the room
        for (Object instance : instances) {

            // don't draw null sprites
            if (instance.sprite == null) continue;

            // apply offset values
            int origin_x = (int) (instance.x - instance.sprite.get_offset_x());
            int origin_y = (int) (instance.y - instance.sprite.get_offset_y());

            // draw sprite
            canvas.drawBitmap(instance.sprite.screen_bitmap,
                    room2screen(origin_x), room2screen(origin_y), null);

            // debug - display x,y & offset points
            if (debug_points) {
                paint.setColor(Color.BLACK);
                canvas.drawCircle(room2screen(
                        (int) instance.x), room2screen((int) instance.y), 4, paint);
                paint.setColor(Color.RED);
                canvas.drawCircle(room2screen(origin_x), room2screen(origin_y), 4, paint);
            }

            // debug - display box
            if (debug_boxes) {
                int[] box = instance.get_box();
                paint.setColor(Color.WHITE);
                canvas.drawLine(room2screen(box[LEFT]), room2screen(box[TOP]),
                        room2screen(box[RIGHT]), room2screen(box[TOP]), paint);
                canvas.drawLine(room2screen(box[RIGHT]), room2screen(box[TOP]),
                        room2screen(box[RIGHT]), room2screen(box[BOTTOM]), paint);
                canvas.drawLine(room2screen(box[LEFT]), room2screen(box[BOTTOM]),
                        room2screen(box[RIGHT]), room2screen(box[BOTTOM]), paint);
                canvas.drawLine(room2screen(box[LEFT]), room2screen(box[TOP]),
                        room2screen(box[LEFT]), room2screen(box[BOTTOM]), paint);
            }

        }

        // get player life & score variables
        int hp = 100, max_hp = 100, score = 0, shield = 0, max_shield = 100;
        if (player != null) {
            hp = player.hp;
            max_hp = player.max_hp;
            score = player.score;
            shield = player.shield;
            max_shield = player.max_shield;
        }

        // print the interface
        if (started) {

            int margin_x = 16, margin_y = 10;

            // points text
            paint.setTextSize(35);
            paint.setFakeBoldText(true);
            paint.setColor(Color.WHITE);
            canvas.drawText(String.valueOf(score) + " " + getResources().getString(R.string.points),
                    margin_x, margin_y + paint.getTextSize(), paint);

            // background hp bar
            paint.setColor(Color.DKGRAY);
            rect.set(margin_x, margin_y + 50, margin_x + 200 + 5, margin_y + 50 + 10 + 5);
            canvas.drawRect(rect, paint);

            // color based on hp
            int hp_percent = (100 * hp) / max_hp;
            if (hp_percent <= 25) paint.setColor(Color.RED);
            else if (hp_percent <= 50) paint.setColor(Color.rgb(255, 170, 86));
            else paint.setColor(Color.WHITE);

            // actual hp bar
            rect.set(margin_x + 3, margin_y + 50 + 3,
                    margin_x + 2 * hp_percent, margin_y + 50 + 10);
            canvas.drawRect(rect, paint);

            // shield bar
            if (shield > 0) {

                // background
                paint.setColor(Color.DKGRAY);
                rect.set(margin_x, margin_y + 25 + 50,
                        margin_x + 200 + 5, margin_y + 25 + 50 + 10 + 5);
                canvas.drawRect(rect, paint);

                // color based on value
                int shield_percent = (100 * shield) / max_shield;
                if (shield_percent < 25) paint.setColor(Color.YELLOW);
                else paint.setColor(Color.rgb(117, 214, 254));

                // actual shield bar
                rect.set(margin_x + 3, margin_y + 25 + 50 + 3,
                        margin_x + 2 * shield_percent, margin_y + 25 + 50 + 10);
                canvas.drawRect(rect, paint);

            }
        }

    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {

        started_touch = true;

        // touch positions in the room
        touch_x = screen2room((int) event.getX());
        touch_y = screen2room((int) event.getY());

        // get a just "touch pressed" flag
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            touch_pressed = true;
            touch_hold = true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP)
            touch_hold = false;

        return true;

    }

}
