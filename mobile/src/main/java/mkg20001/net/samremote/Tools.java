package mkg20001.net.samremote;

import android.os.Build;
import android.util.Base64;
import android.util.Log;

public class Tools {
    public static String base64(String s) {
        if (s==null) s=""; //eq to null
        return Base64.encodeToString(s.getBytes(), Base64.DEFAULT);
    }
    public static int base64len(String s) {
        return base64(s).length();
    }
    public static String chr(int c) {
        return Character.toString((char)c);
    }
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    public static void log(String s) {
        // do something for a debug build
        if (BuildConfig.DEBUG) Log.d("SamRemote",s);
    }

    public static void log2(String s) {
        // always print - like error or key send
        System.out.println(s);
    }


    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }
}
