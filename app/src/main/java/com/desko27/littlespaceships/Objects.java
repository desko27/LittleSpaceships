package com.desko27.littlespaceships;

import android.app.AlertDialog;
import android.content.DialogInterface;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

class Title extends Object {

    public boolean go_out = false;

    public Title(Room r, int x, int y) {
        super(r, x, y, r.sprites.get("title"));
        alarm.put("out", 180);
    }

    @Override
    public void step() {

        // go right
        if (go_out) x += 30;

        if (x > (r.room_width + (this.sprite.get_width() / 2)))
            destroy();
    }

    @SuppressWarnings("unused")
    public void alarm_out() {
        go_out = true;
    }

}

class Subtitle extends Object {

    public boolean go_out = false;

    public Subtitle(Room r, int x, int y) {
        super(r, x, y, r.sprites.get(r.kamikaze_mode ? "subtitle_kamikaze" : "subtitle_normal"));
        alarm.put("out", 180);
    }

    @Override
    public void step() {

        // go right
        if (go_out) x -= 30;

        if (x < -this.sprite.get_width() / 2)
            destroy();
    }

    @SuppressWarnings("unused")
    public void alarm_out() {
        go_out = true;
    }

}

class Controller extends Object {

    public static int difficulty_max = 300, difficulty_alarm = 60, next_enemy_alarm_min = 40;
    public double difficulty = 100;
    public boolean enemy2_appear = false, enemy3_appear = false;
    public int bonus_count = 0, asteroid_count = 0;

    public Controller(Room r) {
        super(r, 0, 0, null);

        // bonus alarm
        alarm.put("bonus", 6200);
        alarm.put("cure", 9000);
        alarm.put("shield", 11000);

        // first alarms
        if (!r.kamikaze_mode) alarm.put("enemy", 50);
        alarm.put("difficulty_increment", 0);

        // kamikaze mode - lots of enemies
        if (r.kamikaze_mode) {
            alarm.put("kamikaze_mode", 150);
            alarm.put("shield", 20);
        }
    }

    @Override
    public void step() {

        if (difficulty > 150 && !enemy2_appear) {
            enemy2_appear = true;
            alarm.put("enemy2", Common.random(0, 600));
        }

        if (difficulty > 225 && !enemy3_appear) {
            enemy3_appear = true;
            alarm.put("enemy3", Common.random(0, 300));
        }
    }

    @SuppressWarnings("unused")
    public void alarm_kamikaze_mode() {

        int qty = 25;
        for (int i = 0; i < qty; i++)
            r.instance_create(new Enemy1(r, difficulty, i * (r.room_width / qty), -32));
        alarm.put("kamikaze_mode", 20);
    }

    @SuppressWarnings("unused")
    public void alarm_difficulty_increment() {

        difficulty += 1;
        alarm.put("difficulty_increment", difficulty_alarm);

    }

    @SuppressWarnings("unused")
    public void alarm_bonus() {

        r.instance_create(new Bonus(r, Common.random(32, r.room_width - 32), -32));
        bonus_count++;

        if (bonus_count < 3)
            alarm.put("bonus", 5300);
        else {

            // all bonus released
            // starts the most difficult part!
            alarm.put("asteroid", 120);

        }

    }

    @SuppressWarnings("unused")
    public void alarm_cure() {

        r.instance_create(new Cure(r, Common.random(32, r.room_width - 32), -32));
        alarm.put("cure", 5500);

    }

    @SuppressWarnings("unused")
    public void alarm_shield() {

        r.instance_create(new Shield(r, Common.random(32, r.room_width - 32), -32));
        alarm.put("shield", 6200);

    }

    @SuppressWarnings("unused")
    public void alarm_enemy() {

        r.instance_create(new Enemy1(r, difficulty, Common.random(32, r.room_width - 32), -32));
        int next_enemy = Math.max(difficulty_max - (int) difficulty, next_enemy_alarm_min);

        // call the alarm again
        alarm.put("enemy", next_enemy);

        // call an enemy pair, >100 difficulty, also chance increases with difficulty
        if (difficulty > 100 && Common.random(0, 2*difficulty_max) < difficulty)
            alarm.put("enemy_pair",
                    Common.random(
                        20,
                        Math.min(60, Math.max(next_enemy, next_enemy_alarm_min - 1))));

    }

    // an additional enemy who will appear together with a recent spawned one
    @SuppressWarnings("unused")
    public void alarm_enemy_pair() {

        r.instance_create(new Enemy1(r, difficulty, Common.random(32, r.room_width - 32), -32));
    }

    @SuppressWarnings("unused")
    public void alarm_enemy2() {

        r.instance_create(new Enemy2(r, difficulty, Common.random(50, r.room_width - 50), -32));
        alarm.put("enemy2",
                Common.random(
                        Math.max(30, 300 - (int) (difficulty / 2)),
                        Math.max(250, 700 - (int) (difficulty * 1.5))));

    }

    @SuppressWarnings("unused")
    public void alarm_enemy3() {

        r.instance_create(new Enemy3(r, Common.random(128, r.room_width - 128), -32));
        alarm.put("enemy3",
                Common.random(
                        Math.max(30, 400 - (int) (difficulty / 2)),
                        Math.max(300, 900 - (int) (difficulty * 1.25))));

    }

    @SuppressWarnings("unused")
    public void alarm_asteroid() {

        r.instance_create(new Asteroid(r, Common.random(32, r.room_width - 32), -128));
        alarm.put("asteroid", Math.max(25, 350 - (10 * asteroid_count)));
        asteroid_count++;

    }

}

class Player extends Object {

    public int level = 1, max_hp = 100, hp, max_shield = 1000, shield, score = 0,
            dead_explosions = 0, easy_fire_speed = 9;
    public int limit_top, limit_right, limit_bottom, limit_left;
    public double mov_x, mov_y, speed, speed_base = 9;
    public boolean again_to_fire = false, dead = false;
    public LinkedHashMap<String, Integer> ranks = new LinkedHashMap<>();

    public Player(Room r, int x, int y) {
        super(r, x, y, r.sprites.get("player"));
        hp = max_hp;

        if (r.kamikaze_mode) level = 4;

        // gratifications for game over
        ranks.put(r.getResources().getString(R.string.rank_d), 100);
        ranks.put(r.getResources().getString(R.string.rank_c), 500);
        ranks.put(r.getResources().getString(R.string.rank_b), 1000);
        ranks.put(r.getResources().getString(R.string.rank_a), 2000);
        ranks.put(r.getResources().getString(R.string.rank_s), 5000);
        ranks.put(r.getResources().getString(R.string.rank_ss), 7500);
        ranks.put(r.getResources().getString(R.string.rank_sss), 10000);

        // limits where player can move to
        limit_right = r.room_width - sprite.get_offset_x();
        limit_left = sprite.get_offset_x();
        limit_top = sprite.get_height();
        limit_bottom = r.room_height - sprite.get_height();

        // the speed at we can move the spaceship
        speed = speed_base;
    }

    public void fire() {

        if (level == 1)
            // single simple bullet
            r.instance_create(new Bullet(this, r, (int) x, (int) y - 32));

        else if (level == 2) {
            // double simple bullet
            r.instance_create(new Bullet(this, r, (int) x + 12, (int) y - 32));
            r.instance_create(new Bullet(this, r, (int) x - 12, (int) y - 32));
        } else if (level == 3) {
            // triple energy bullet, 3 frontal directions
            r.instance_create(new Bullet(this, r, (int) x + 12, (int) y - 32, 45));
            r.instance_create(new Bullet(this, r, (int) x, (int) y - 32, 90));
            r.instance_create(new Bullet(this, r, (int) x - 12, (int) y - 32, 135));
        } else if (level >= 4) {
            // full 7 energy bullets, 8 directions
            for (int i = 1; i < 8; i++)
                r.instance_create(new Bullet(this, r,
                        (int) (x + Math.cos(Math.toRadians(i * 22.5)) * 32),
                        (int) (y - Math.sin(Math.toRadians(i * 22.5)) * 32),
                        i * 22.5));
        }

        r.play_sound("bullet");
    }

    @Override
    public void step() {

        if (dead) return;

        // controlling player
        double target_x, target_y;
        if (r.started_touch) {

            target_x = r.touch_x;
            target_y = r.touch_y - 96;

        } else {

            // still not touched
            target_x = x;
            target_y = y;
        }

        // apply movement
        double distance = Common.get_distance(x, y, target_x, target_y);
        double angle = Common.get_angle(x, y, target_x, target_y);
        x += mov_x = Math.cos(angle) * (distance / (200 / speed));
        y += mov_y = Math.sin(angle) * (distance / (200 / speed));

        // limit the zone
        if (x > limit_right) x = limit_right;
        if (x < limit_left) x = limit_left;
        if (y > limit_bottom) y = limit_bottom;
        if (y < limit_top) y = limit_top;

        // firing bullets
        if (r.easy_fire_mode) {

            if (r.touch_hold && !alarm.containsKey("easy_fire") &&
                    !alarm.containsKey("easy_fire_delay")) {

                // immediate shoot if no movement
                if (Math.abs(mov_x) < 10 && Math.abs(mov_y) < 10) {
                    fire();
                    alarm.put("easy_fire_delay", easy_fire_speed);
                }

                alarm.put("easy_fire", easy_fire_speed);
            }

        } else {

            if (r.touch_pressed && again_to_fire) {

                fire();
                again_to_fire = false;

            } else if (r.touch_pressed) {

                // fire on next press
                again_to_fire = true;
                alarm.put("doublepress_timeframe", 20);

            }
        }

        // decrease shield if wielded
        if (shield > 0) shield--;

    }

    @Override
    public void post_step() {

        if (dead) return;

        // deactivate touch_pressed detected events
        if (r.touch_pressed) r.touch_pressed = false;

        // check hp and die
        if (hp <= 0) die();

    }

    @SuppressWarnings("unused")
    public void alarm_doublepress_timeframe() {
        again_to_fire = false;
    }

    @SuppressWarnings("unused")
    public void alarm_easy_fire() {
        if (dead) return;

        if (r.touch_hold) {
            fire();
            alarm.put("easy_fire_delay", easy_fire_speed);
            alarm.put("easy_fire", easy_fire_speed);
        }
    }

    @SuppressWarnings("unused")
    public void alarm_easy_fire_delay() {}

    @SuppressWarnings("unused")
    public void collision_Enemy1(Enemy1 other) {

        if (shield <= 0) hp -= 20;
        r.play_sound("hit");
        other.die();
    }

    @SuppressWarnings("unused")
    public void collision_Enemy2(Enemy2 other) {

        if (shield <= 0) hp -= 20;
        r.play_sound("hit");
        other.die();
    }

    @SuppressWarnings("unused")
    public void collision_Enemy3(Enemy3 other) {

        if (shield <= 0) hp -= 30;
        r.play_sound("hit");
        other.die();
    }

    @SuppressWarnings("unused")
    public void collision_BulletEnemy(Object other) {

        if (shield <= 0) hp -= 5;
        other.destroy();
    }

    @SuppressWarnings("unused")
    public void collision_Asteroid(Asteroid other) {

        // shield is the only way to destroy an asteroid
        if (shield <= 0) die();
        other.die();

    }

    public void die() {

        if (dead) return;

        hp = 0;
        dead = true;
        sprite = null;

        // start dead chain
        alarm.put("explosions", 0);

    }

    @SuppressWarnings("unused")
    public void alarm_explosions() {

        r.instance_create(new GameEffect(r,
                (int) x - 48 + Common.random(0, 96),
                (int) y - 48 + Common.random(0, 96),
                Room.EXPLOSION));

        r.play_sound("explosion");
        dead_explosions++;

        if (dead_explosions < 7) alarm.put("explosions", 12);
        else alarm.put("game_over", 240);

    }

    @SuppressWarnings("unused")
    public void alarm_game_over() {

        r.pause(true);

        // calc rank
        String rank = "", last = "";
        if (!r.kamikaze_mode) {
            for (Map.Entry e : ranks.entrySet()) {

                if (score < (int) e.getValue()) {
                    rank = e.getKey().toString();
                    break;
                }
                last = e.getKey().toString();
            }
            if (rank.equals("")) rank = last;
        }
        else
            rank = r.getResources().getString(R.string.this_is_kamikaze);

        // show game over dialog
        game_over_dialog(rank);

    }

    public void game_over_dialog(final String rank) {

        new AlertDialog.Builder(r.getContext())
                .setTitle("Game Over")
                .setMessage(r.getResources().getString(R.string.rank) + ": " + rank +
                        "\n" + r.getResources().getString(R.string.your_score) + ": " +
                        String.valueOf(score) + " " + r.getResources().getString(R.string.points))
                .setPositiveButton(r.getResources().getString(R.string.normal_mode),
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        // restart game
                        r.restart(false);
                    }
                })
                .setNeutralButton(r.getResources().getString(R.string.kamikaze_mode),
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        // restart game - kamikaze mode
                        r.restart(true);
                    }
                })
                .setNegativeButton(r.getResources().getString(R.string.set_controls),
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        // select fire mode
                        new AlertDialog.Builder(r.getContext())
                                .setTitle(r.getResources().getString(R.string.controls))
                                .setMessage(r.getResources().getString(R.string.select_fire_mode))
                                .setPositiveButton(r.getResources().getString(R.string.easy),
                                        new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {

                                        // easy - press and hold mode
                                        r.easy_fire_mode = true;
                                        Common.saveBoolean(r.getContext(), true);
                                        game_over_dialog(rank);
                                    }
                                })
                                .setNegativeButton(r.getResources().getString(R.string.hard),
                                        new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {

                                        // hard - double tap mode
                                        r.easy_fire_mode = false;
                                        Common.saveBoolean(r.getContext(), false);
                                        game_over_dialog(rank);
                                    }
                                })
                                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {

                                        // do nothing
                                        game_over_dialog(rank);
                                    }
                                })
                                .show();

                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {

                        // restart game
                        r.restart(false);
                    }
                })
                .show();

    }

}

class PlayerShield extends Object {

    public String sprite_id = "player_shield";
    public PlayerShield(Room r, int x, int y) {
        super(r ,x, y, null);
    }

    @Override
    public void post_step() {

        if (r.player.shield > 0) {

            // follow player
            x = r.player.x;
            y = r.player.y;

            // set sprite
            if (sprite == null)
                sprite = r.sprites.get(sprite_id);

        } else
            if (sprite != null) sprite = null;
    }

}

class Bullet extends Object {

    public int level, damage;
    public double angle, speed, speed_plus, speed_base = 14;

    public Bullet(Player p, Room r, int x, int y, double angle) {
        super(r, x, y, null);
        this.level = p.level;
        this.angle = angle;

        ArrayList<Sprite> level_sprites = new ArrayList<>();
        level_sprites.add(r.sprites.get("bullet")); // level 1
        level_sprites.add(r.sprites.get("bullet")); // level 2
        level_sprites.add(r.sprites.get("bullet_energy1")); // level 3
        level_sprites.add(r.sprites.get("bullet_energy2")); // level 4

        // sprite depending on level
        sprite = level <= level_sprites.size() ? level_sprites.get(level-1)
                : level_sprites.get(level_sprites.size()-1);

        // more level is more damage
        damage = level > 2 ? level : 1;

        // give extra speed when player is moving forward
        speed_plus = p.mov_y < 0 ? -p.mov_y/2 : 0;
        speed = speed_base + level + speed_plus;
    }

    // no angle provided if not necessary, 90 by default
    public Bullet(Player p, Room r, int x, int y) {
        this(p, r, x, y, 90);
    }

    @Override
    public void step() {

        // bullet speed
        x += Math.cos(Math.toRadians(angle)) * speed;
        y -= Math.sin(Math.toRadians(angle)) * speed;

        // destroy the instance when it's out of sight
        if (y < -32 || y > r.room_height + 32
                || x < -32 || x > r.room_width + 32)
            destroy();

    }
}

class BonusParent extends Object {

    public int speed;
    public String sprite_id;

    @Override
    public void step() {

        // going down
        y += speed;

        // destroy the instance when it's out of sight
        if (y > r.room_height + 32)
            destroy();

    }

    public BonusParent(Room r, int x, int y, String sprite_id, int speed) {
        super(r, x, y, r.sprites.get(sprite_id));
        this.speed = speed;
        this.sprite_id = sprite_id;

        // play sound when appears
        r.play_sound("bonus_spotted");
        alarm.put("blink", Room.BLINK_ALARM);
    }

    @SuppressWarnings("unused")
    public void alarm_blink() {
        if (sprite.equals(r.sprites.get(sprite_id))) sprite = r.sprites.get("bonus_blink");
        else sprite = r.sprites.get(sprite_id);
        alarm.put("blink", Room.BLINK_ALARM);
    }

}

class Bonus extends BonusParent {

    public Bonus(Room r, int x, int y) {
        super(r, x, y, "bonus", 4);
    }

    @SuppressWarnings("unused")
    public void collision_Player(Player other) {

        r.play_sound("bonus_taken");
        other.level += 1;
        destroy();

    }

}

class Cure extends BonusParent {

    public Cure(Room r, int x, int y) {
        super(r, x, y, "cure", 4);
    }

    @SuppressWarnings("unused")
    public void collision_Player(Player other) {

        r.play_sound("bonus_taken");
        if (other.hp < other.max_hp) {

            other.hp += other.max_hp / 2;
            if (other.hp > other.max_hp)
                other.hp = other.max_hp;
        }
        destroy();

    }

}

class Shield extends BonusParent {

    public Shield(Room r, int x, int y) {
        super(r, x, y, "shield", 4);
    }

    @SuppressWarnings("unused")
    public void collision_Player(Player other) {

        r.play_sound("bonus_taken");
        other.shield = other.max_shield;
        destroy();

    }

}

class Enemy1 extends Object {

    public int hp, speed, points = 10;

    public Enemy1(Room r, double difficulty, int x, int y) {
        super(r, x, y, null);

        if (difficulty < 200) {
            sprite = r.sprites.get("enemy1");
            speed = Common.random(5, 7);
            hp = 2;
        } else if (difficulty < 300) {
            sprite = r.sprites.get("enemy1_advanced");
            speed = Common.random(6, 8);
            hp = 4;
        } else {
            sprite = r.sprites.get("enemy1_advanced2");
            speed = Common.random(7, 9);
            hp = 8;
        }
    }

    @Override
    public void step() {

        // enemy speed
        y += speed;

        // destroy the instance when it's out of sight
        if (y > r.room_height + 32)
            destroy();

    }

    @SuppressWarnings("unused")
    public void collision_Bullet(Bullet other) {

        hp -= other.damage;
        other.destroy();

        if (hp <= 0) die();
        else r.play_sound("hit");

    }

    public void die() {
        r.player.score += points;
        r.instance_create(new GameEffect(r, (int)x, (int)y, Room.EXPLOSION));
        r.play_sound("explosion");
        destroy();
    }

}

class Enemy2 extends Object {

    public int hp, points = 25, fire_count = 0, speed_plus = 0;
    public boolean down = true, right;

    public Enemy2(Room r, double difficulty, int x, int y) {
        super(r, x, y, null);
        alarm.put("stop", Common.random(60, 180));

        if (difficulty < 250) {
            sprite = r.sprites.get("enemy2");
            hp = 5;
        } else {
            sprite = r.sprites.get("enemy2_advanced");
            hp = 10;
        }

        // determine right/left
        right = x < r.room_width / 2;
    }

    @Override
    public void step() {

        // enemy speed
        if (down) y += 4 + speed_plus;
        else if (right) x += 7;
        else x -= 7;

        // interrupt vertical stop if out of screen
        if (x > r.room_width - 48 || x < 48) alarm_continue();

        // destroy the instance when it's out of sight
        if (y > r.room_height + 32)
            destroy();

    }

    @SuppressWarnings("unused")
    public void alarm_stop() {

        alarm.put("fire", Common.random(5, 10));

        down = false;
        alarm.put("continue", Common.random(20, 60));

    }

    @SuppressWarnings("unused")
    public void alarm_fire() {

        // don't shoot if player is dead
        if (r.player.dead) return;

        r.instance_create(new BulletEnemy(r, (int) x, (int) y + 32));
        if (fire_count < 3) {
            alarm.put("fire", Common.random(2, 6));
            fire_count++;
        }

    }

    @SuppressWarnings("unused")
    public void alarm_continue() {

        down = true;
        speed_plus = 7;

    }

    @SuppressWarnings("unused")
    public void collision_Bullet(Bullet other) {

        hp -= other.damage;
        other.destroy();

        if (hp <= 0) die();
        else r.play_sound("hit");

    }

    public void die() {
        r.player.score += points;
        r.instance_create(new GameEffect(r, (int) x, (int) y, Room.EXPLOSION));
        r.play_sound("explosion");
        destroy();
    }

}

class Enemy3 extends Object {

    public int hp = 6, points = 50;
    public int speed = 14;

    public Enemy3(Room r, int x, int y) {
        super(r, x, y, r.sprites.get("enemy3"));
    }

    @Override
    public void step() {

        // enemy speed
        y += speed;

        // destroy the instance when it's out of sight
        if (y > r.room_height + 32)
            destroy();

    }

    @SuppressWarnings("unused")
    public void collision_Bullet(Bullet other) {

        hp -= other.damage;
        other.destroy();

        if (hp <= 0) die();
        else r.play_sound("hit");

    }

    public void die() {
        r.player.score += points;
        r.instance_create(new GameEffect(r, (int) x, (int) y, Room.EXPLOSION));
        r.play_sound("explosion");
        destroy();
    }

}

class BulletEnemy extends Object {

    public int speed = 15;
    public double angle;

    public BulletEnemy(Room r, int x, int y) {
        super(r, x, y, r.sprites.get("bullet_enemy"));

        // target to the player
        this.angle = Common.get_angle(x, y, r.player.x, r.player.y);
    }

    @Override
    public void step() {

        // movement
        x += Math.cos(angle) * speed;
        y += Math.sin(angle) * speed;

        // destroy the instance when it's out of sight
        if (y < -32 || y > r.room_height + 32
                || x < -32 || x > r.room_width + 32)
            destroy();

    }

}

class Asteroid extends Object {

    public int speed = 17, points = 100;

    public Asteroid(Room r, int x, int y) {
        super(r, x, y, r.sprites.get("asteroid" + String.valueOf(Common.random(0, 5))));
    }

    @Override
    public void step() {

        // enemy speed
        y += speed;

        // destroy the instance when it's out of sight
        if (y > r.room_height + 128)
            destroy();

    }

    @SuppressWarnings("unused")
    public void collision_Bullet(Object other) {

        // cannot take down asteroids with bullets
        other.destroy();
        r.play_sound("hit");
    }

    public void die() {
        r.player.score += points;
        r.instance_create(new GameEffect(r, (int)x, (int)y, Room.EXPLOSION));
        r.play_sound("explosion");
        destroy();
    }

}

class GameEffect extends Object {

    public ArrayList<ArrayList<Sprite>> effects = new ArrayList<>();
    public ArrayList<Double> effects_speed = new ArrayList<>();
    public double frame = 0;
    public int selected = 0;

    public GameEffect(final Room r, int x, int y, int selected) {
        super(r, x, y, null);
        this.selected = selected;

        effects_speed.add(Room.EXPLOSION, 0.5);
        effects.add(Room.EXPLOSION,
            new ArrayList<Sprite>() {{
                add(r.sprites.get("explosion_0"));
                add(r.sprites.get("explosion_1"));
                add(r.sprites.get("explosion_2"));
                add(r.sprites.get("explosion_3"));
                add(r.sprites.get("explosion_4"));
                add(r.sprites.get("explosion_5"));
                add(r.sprites.get("explosion_6"));
                add(r.sprites.get("explosion_7"));
                add(r.sprites.get("explosion_8"));
            }}
        );
    }

    @Override
    public void step() {

        if (frame < effects.get(selected).size()) {
            sprite = effects.get(selected).get((int)Math.floor(frame));
            frame += effects_speed.get(selected);
        }
        else
            destroy();

    }

}