package com.desko27.littlespaceships;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;

public class Common {

    public static int random(int min, int max) {

        Random rand = new Random();
        return rand.nextInt((max - min) + 1) + min;

    }

    public static double get_distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1));
    }
    public static double get_angle(double x1, double y1, double x2, double y2) {
        return Math.atan2(y2 - y1, x2 - x1);
    }

    public static Boolean loadBoolean(Context context) {

        try {

            FileInputStream fis = context.openFileInput("boolean.ser");
            ObjectInputStream ois = new ObjectInputStream(fis);

            @SuppressWarnings("unchecked")
            Boolean var = (Boolean) ois.readObject();

            ois.close();
            fis.close();

            return var;

        } catch (IOException ioe) {

            System.out.println("Cannot open the file");
            ioe.printStackTrace();

        } catch (ClassNotFoundException c) {

            System.out.println("Class not found");
            c.printStackTrace();
        }

        return null;

    }

    public static boolean saveBoolean(Context context, Boolean var) {

        try
        {
            FileOutputStream fos = context.openFileOutput("boolean.ser", Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(var);
            oos.close();
            fos.close();
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
            return false;
        }

        return true;
    }

}
