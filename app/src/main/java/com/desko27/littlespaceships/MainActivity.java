package com.desko27.littlespaceships;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;


public class MainActivity extends Activity {

    // frames per second
    int fps = 60;

    public Room r;
    public Handler h;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (r == null) {

            // create room
            r = (Room) findViewById(R.id.room);
            ViewTreeObserver obs = r.getViewTreeObserver();
            obs.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {

                    // hmm, this is executing twice

                    // required for setting scale
                    r.width = r.getWidth();
                    r.height = r.getHeight();
                }
            });

            // ask for fire mode
            r.easy_fire_mode = Common.loadBoolean(r.getContext());
            if (r.easy_fire_mode == null) {

                new AlertDialog.Builder(r.getContext())
                        .setTitle(r.getResources().getString(R.string.controls))
                        .setMessage(r.getResources().getString(R.string.select_fire_mode))
                        .setPositiveButton(r.getResources().getString(R.string.easy),
                                new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                // easy - press and hold mode
                                r.easy_fire_mode = true;
                                Common.saveBoolean(r.getContext(), true);

                                // start the game loop
                                h = new Handler();
                                h.postDelayed(GameLoop, 1000);
                            }
                        })
                        .setNegativeButton(r.getResources().getString(R.string.hard),
                                new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                // hard - double tap mode
                                r.easy_fire_mode = false;
                                Common.saveBoolean(r.getContext(), false);

                                // start the game loop
                                h = new Handler();
                                h.postDelayed(GameLoop, 1000);
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {

                                // easy - press and hold mode
                                r.easy_fire_mode = true;
                                Common.saveBoolean(r.getContext(), true);

                                // start the game loop
                                h = new Handler();
                                h.postDelayed(GameLoop, 1000);
                            }
                        })
                        .show();

            } else {

                // start the game loop
                h = new Handler();
                h.postDelayed(GameLoop, 1000);

            }
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (r.player == null || r.player.dead || r.paused) return;
        r.pause(true);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (r.player == null || r.player.dead || !r.paused) return;
        r.pause(false);
    }

    public Runnable GameLoop = new Runnable() {
        @Override
        public void run() {

            if (r.restart) r.start();
            if (!r.started) {

                // now game can start
                r.start();

                // remove loading message
                TextView loading = (TextView) findViewById(R.id.loading);
                ((RelativeLayout)loading.getParent()).removeView(loading);

            }

            if (r.paused) {
                // check again
                h.postDelayed(GameLoop, 10);
                return;
            }

            // elapsed time - start
            long start = System.nanoTime();

            // run all the internal methods of every instance
            for (Object instance : r.instances) {

                instance.step();
                instance.post_step();
                instance.do_alarms();
                instance.do_collisions();
            }

            // create and destroy instances
            for (Object instance : r.create_instances) {
                r.instances.add(instance);
                if (r.instancesByClass.containsKey(instance.getClass()))
                    r.instancesByClass.get(instance.getClass()).add(instance);
                else {
                    ArrayList<Object> l = new ArrayList<>();
                    l.add(instance);
                    r.instancesByClass.put(instance.getClass(), l);
                }
            }
            for (Object instance : r.destroy_instances) {
                r.instances.remove(instance);
                r.instancesByClass.get(instance.getClass()).remove(instance);
            }
            r.create_instances.clear();
            r.destroy_instances.clear();

            // redraw screen
            r.invalidate();

            // elapsed time - end
            long end = System.nanoTime();
            int elapsed_time = (int) (end - start) / 1000000;

            /*if (elapsed_time > 5*(1000 / fps)) // bad fps warning
                Log.w("BadFPS", "Elapsed Time -> " + String.valueOf(elapsed_time));*/

            // run the next frame
            h.postDelayed(GameLoop, Math.max(0, (1000 / fps) - elapsed_time));
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
