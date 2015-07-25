package com.desko27.littlespaceships;

import java.util.HashMap;

public class Sprites {

    public HashMap<String,Sprite> list = new HashMap<>();

    public Sprites(Room r) {

        list.put("title", new Sprite(r, R.drawable.title, true, 1.5));
        list.put("subtitle_normal", new Sprite(r, R.drawable.subtitle_normal, true, 1.5));
        list.put("subtitle_kamikaze", new Sprite(r, R.drawable.subtitle_kamikaze, true, 1.5));

        list.put("player", new Sprite(r, R.drawable.player, true, 2));
        list.put("bullet", new Sprite(r, R.drawable.bullet, true, 2));
        list.put("bullet_energy1", new Sprite(r, R.drawable.bullet_energy, true, 3));
        list.put("bullet_energy2", new Sprite(r, R.drawable.bullet_energy, true, 4));
        list.put("bonus_blink", new Sprite(r, R.drawable.bonus_blink, true, 1.5));
        list.put("bonus", new Sprite(r, R.drawable.bonus, true, 1.5));
        list.put("cure", new Sprite(r, R.drawable.cure, true, 1.5));
        list.put("shield", new Sprite(r, R.drawable.shield, true, 1.5));
        list.put("player_shield", new Sprite(r, R.drawable.player_shield, true, 2));
        list.put("enemy1", new Sprite(r, R.drawable.enemy1, true, 2));
        list.put("enemy1_advanced", new Sprite(r, R.drawable.enemy1_advanced, true, 2));
        list.put("enemy1_advanced2", new Sprite(r, R.drawable.enemy1_advanced2, true, 2));
        list.put("enemy2", new Sprite(r, R.drawable.enemy2, true, 2));
        list.put("enemy2_advanced", new Sprite(r, R.drawable.enemy2_advanced, true, 2));
        list.put("enemy3", new Sprite(r, R.drawable.enemy3, true, 2));
        list.put("bullet_enemy", new Sprite(r, R.drawable.bullet_enemy, true, 2));

        list.put("asteroid1", new Sprite(r, R.drawable.asteroid1, true, 2));
        list.put("asteroid2", new Sprite(r, R.drawable.asteroid2, true, 2));
        list.put("asteroid3", new Sprite(r, R.drawable.asteroid3, true, 2));
        list.put("asteroid4", new Sprite(r, R.drawable.asteroid4, true, 2));
        list.put("asteroid5", new Sprite(r, R.drawable.asteroid5, true, 2));

        list.put("explosion_0", new Sprite(r, R.drawable.explosion_0, true, 2));
        list.put("explosion_1", new Sprite(r, R.drawable.explosion_1, true, 2));
        list.put("explosion_2", new Sprite(r, R.drawable.explosion_2, true, 2));
        list.put("explosion_3", new Sprite(r, R.drawable.explosion_3, true, 2));
        list.put("explosion_4", new Sprite(r, R.drawable.explosion_4, true, 2));
        list.put("explosion_5", new Sprite(r, R.drawable.explosion_5, true, 2));
        list.put("explosion_6", new Sprite(r, R.drawable.explosion_6, true, 2));
        list.put("explosion_7", new Sprite(r, R.drawable.explosion_7, true, 2));
        list.put("explosion_8", new Sprite(r, R.drawable.explosion_8, true, 2));

    }

    public Sprite get(String key) {
        return list.get(key);
    }

}
