package com.desko27.littlespaceships;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
class Sprite {

    public double scale;
    public int id, original_offset_x, original_offset_y;
    public Bitmap bitmap, screen_bitmap;
    public Room r;

    public Sprite(Room r, int id, int offset_x, int offset_y, double scale, boolean offset_center) {

        this.r = r;
        this.id = id;
        this.scale = scale;
        updateBitmap(id);

        if (offset_center) {
            this.original_offset_x = bitmap.getWidth() / 2;
            this.original_offset_y = bitmap.getHeight() / 2;
        } else {
            this.original_offset_x = offset_x;
            this.original_offset_y = offset_y;
        }

    }
    public Sprite(Room r, int id, boolean offset_center, double scale) {
        this(r, id, 0, 0, scale, offset_center);
    }
    public Sprite(Room r, int id, boolean offset_center) {
        this(r, id, 0, 0, 1, offset_center);
    }
    public Sprite(Room r, int id, int offset_x, int offset_y, double scale) {
        this(r, id, offset_x, offset_y, scale, false);
    }
    public Sprite(Room r, int id, int offset_x, int offset_y) {
        this(r, id, offset_x, offset_y, 1);
    }
    public Sprite(Room r, int id) {
        this(r, id, 0, 0, 1);
    }

    public void updateBitmap(int id) {

        // get non-resized bitmap
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        bitmap = BitmapFactory.decodeResource(r.getResources(), id, o);

        // get scaled bitmap
        screen_bitmap = Bitmap.createScaledBitmap(
                bitmap,
                r.room2screen(get_width()),
                r.room2screen(get_height()),
                false);

    }

    public int get_width() {
        return (int) (scale * bitmap.getWidth());
    }
    public int get_height() {
        return (int) (scale * bitmap.getHeight());
    }
    public int get_offset_x() {
        return (int)(original_offset_x * scale);
    }
    public int get_offset_y() {
        return (int)(original_offset_y * scale);
    }

}

class Object {

    public static int TOP = 0, RIGHT = 1, BOTTOM = 2, LEFT = 3;

    public Sprite sprite;
    public double x, y;

    public Room r;
    public HashMap<String,Integer> alarm = new HashMap<>();
    public HashMap<Method,Class> collision_methods = new HashMap<>();

    public Object(Room r, int x, int y, Sprite sprite) {
        this.r = r;
        this.x = x;
        this.y = y;
        this.sprite = sprite;

        // don't use reflection if it has been done for this class before
        if (r.collision_methods.containsKey(this.getClass()))
            collision_methods = r.collision_methods.get(this.getClass());

        // use reflection to detect collision methods for the first time with this class
        else {
            for (Method method : this.getClass().getMethods()) {
                if (method.getName().startsWith("collision_")) {

                    String[] method_name = method.getName().split("_");
                    try {

                        // try to get the target class for the detected collision method
                        // example: collision_Player -> `Player`
                        collision_methods.put(method,
                                Class.forName(this.getClass().getPackage().toString().split(" ")[1] +
                                        "." + method_name[1]));

                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }

            // save methods list in a reusable place
            r.collision_methods.put(this.getClass(), collision_methods);
        }
    }

    public void step() {}
    public void post_step() {}
    public void destroy() { r.instance_destroy(this); }

    public void do_alarms() {

        // every entry may be re-added immediately after its method execution, so it's safe
        // to iterate over a copy of the hashmap
        HashMap<String,Integer> local_alarm = new HashMap<>(alarm);

        // iterate through the hashmap
        for (Map.Entry a : local_alarm.entrySet()) {
            if ((int) a.getValue() <= 0) {

                // remove the executed alarm
                alarm.remove(a.getKey().toString());

                // execute alarm method
                Method method;
                //noinspection TryWithIdenticalCatches
                try {
                    method = this.getClass().getMethod("alarm_" + a.getKey());
                    method.invoke(this);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }

            } else

                // decrease the alarm timer
                alarm.put(a.getKey().toString(), (int) a.getValue() - 1);
        }

    }

    public void do_collisions() {

        // iterate over every collision method
        for (Map.Entry e : collision_methods.entrySet()) {

            Method method = (Method) e.getKey();
            Class other_class = (Class) e.getValue();

            // do nothing & next method if there's no existing instances
            if (!r.instancesByClass.containsKey(other_class)) continue;

            for (Object other : r.instancesByClass.get(other_class)) {

                // no sprite means no box and no collisions
                if (this.sprite == null || other.sprite == null) continue;

                // check if collision happens
                int[] this_box = get_box();
                int[] other_box = other.get_box();

                if (this_box[RIGHT] > other_box[LEFT] && this_box[LEFT] < other_box[RIGHT]
                        && this_box[BOTTOM] > other_box[TOP] && this_box[TOP] < other_box[BOTTOM])
                {
                    // execute collision method with the instance
                    //noinspection TryWithIdenticalCatches
                    try {
                        method.invoke(this, other);
                    } catch (IllegalAccessException err) {
                        err.printStackTrace();
                    } catch (InvocationTargetException err) {
                        err.printStackTrace();
                    }
                }
            }

        }

    }

    public int[] get_box() {

        int[] box = new int[4];
        box[TOP] = (int) y - sprite.get_offset_y();
        box[RIGHT] = (int) x - sprite.get_offset_x() + sprite.get_width();
        box[LEFT] = (int) x - sprite.get_offset_x();
        box[BOTTOM] = (int) y - sprite.get_offset_y() + sprite.get_height();

        return box;
    }

}