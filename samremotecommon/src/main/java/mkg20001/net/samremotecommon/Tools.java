package mkg20001.net.samremotecommon;

import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import net.nodestyle.helper.Array;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Tools {
    public static ColorFilter filter() {
        int iColor = Color.parseColor("#FFFFFF");

        int red   = (iColor & 0xFF0000) / 0xFFFF;
        int green = (iColor & 0xFF00) / 0xFF;
        int blue  = iColor & 0xFF;

        float[] matrix = { 0, 0, 0, 0, red,
                0, 0, 0, 0, green,
                0, 0, 0, 0, blue,
                0, 0, 0, 1, 0 };

        return new ColorMatrixColorFilter(matrix);
    }
    static String base64(String s) {
        if (s==null) s=""; //eq to null
        return Base64.encodeToString(s.getBytes(), Base64.DEFAULT);
    }
    static int base64len(String s) {
        return base64(s).length();
    }
    static String chr(int c) {
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
        //if (BuildConfig.DEBUG)
        Log.d("SamRemote",s);
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

    private static final int NB_THREADS = 25;
    private static final Array ips=new Array();

    static String[] doScan(String subnet) {
        ips.clear();
        Tools.log("Start scanning on subnet "+subnet);

        ExecutorService executor = Executors.newFixedThreadPool(NB_THREADS);
        for(int dest=0; dest<255; dest++) {
            String host = subnet + dest;
            executor.execute(pingRunnable(host));
        }

        Tools.log("Waiting for executor to terminate...");
        executor.shutdown();
        try { executor.awaitTermination(60*1000, TimeUnit.MILLISECONDS); } catch (InterruptedException ignored) { }

        Tools.log("Scan finished");

        String[] res=new String[ips.length];

        Integer i=0;

        for (java.lang.Object o:ips.getItems()) {
            res[i]=(String) o;
            i++;
        }

        return res;
    }

    private static Runnable pingRunnable(final String host) {
        return new Runnable() {
            public void run() {
                Tools.log( "Pinging " + host + "...");
                try {
                    InetAddress inet = InetAddress.getByName(host);
                    boolean reachable = inet.isReachable(1000);
                    if (reachable) ips.push(host);
                    Tools.log( "=> Result: " + (reachable ? "reachable" : "not reachable"));
                } catch (UnknownHostException e) {
                    Log.e("SamRemote", "Not found", e);
                } catch (IOException e) {
                    Log.e("SamRemote", "IO Error", e);
                }
            }
        };
    }
}
