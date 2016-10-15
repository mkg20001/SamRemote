package mkg20001.net.samremotecommon;

import net.nodestyle.events.EventEmitter;
import net.nodestyle.events.EventListener;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

class RCKey {
    String k;
    public RCKey(String key) throws InvalidKey {
        if (!VerifyKey.verify(key)) throw new InvalidKey(key);
        this.k=key;
    }
    public String getFormatted() {
        return "KEY_"+k.toUpperCase();
    }
}

class VerifyKey {
    static String allkeys[]={
            "down","up","left","right", //direction
            "play","pause","ff","rewind","ff_","rewind_", //media
            "enter","exit","return", //controls
            "hdmi","source","menu","chlist", //tv stuff
            "volup","voldown","chup","chdown", //vol/ch
            "poweroff","poweron" //main keys
    };
    public static boolean verify(String k) {
        boolean found=false;
        for (int i = 0; i < VerifyKey.allkeys.length; i++) {
            if (VerifyKey.allkeys[i].equalsIgnoreCase(k)) found=true;
        }
        return found;
    }
}

class InvalidKey extends Exception {
    public String key;
    public String code;
    public InvalidKey(String s) {
        super("Invalid RC Key: "+s);
        this.key=s;
        this.code="EINVALIDKEY";
    }
}

public class RC {
    //Connect & Control the TV
    public boolean connected=false;
    EventEmitter event;
    String ip;
    String mac;
    String name;
    String target;
    int port=55000;
    int timeout=5000;
    String appString="iphone..iapp.samsung";
    String tvAppString="iphone.UN60D6000.iapp.samsung";
    public boolean connect(String ip) {
        Socket client;

        try{
            //Will throw connection refused
            Tools.log("Connecting to "+ip+"...");
            client = new Socket(ip, port);
            PrintWriter out = new PrintWriter(client.getOutputStream(), true);
            //Send the chunks
            out.println(firstPacket());
            out.println(secondPacket("KEY_TEST"));
            client.setSoTimeout(timeout);
            client.close();
            //if ok
            connected=true;
            target=ip;
        } catch(UnknownHostException e) {
            Tools.log("Unkown Host: "+ip);
            connected=false;
        } catch(IOException e) {
            Tools.log("Offline - Go to next host...");
            connected=false;
        }
        return connected;
    }
    private String firstPacket() {
        String message = Tools.chr(0x64) +
                Tools.chr(0x00) +
                Tools.chr(Tools.base64len(ip)) +
                Tools.chr(0x00) +
                Tools.base64(ip) +
                Tools.chr(Tools.base64len(mac)) +
                Tools.chr(0x00) +
                Tools.base64(mac) +
                Tools.chr(Tools.base64len(name)) +
                Tools.chr(0x00) +
                Tools.base64(name);

        return Tools.chr(0x00) +
                Tools.chr(appString.length()) +
                Tools.chr(0x00) +
                appString +
                Tools.chr(message.length()) +
                Tools.chr(0x00) +
                message;
    }
    private String secondPacket(String command) {
        String message = Tools.chr(0x00) +
                Tools.chr(0x00) +
                Tools.chr(0x00) +
                Tools.chr(Tools.base64len(command)) +
                Tools.chr(0x00) +
                Tools.base64(command);

        return Tools.chr(0x00) +
                Tools.chr(tvAppString.length()) +
                Tools.chr(0x00) +
                tvAppString +
                Tools.chr(message.length()) +
                Tools.chr(0x00) +
                message;
    }
    public boolean send(RCKey k) {
        final String key=k.getFormatted();
        if (!connected) {
            Tools.log("Cannot send "+key+" because: Offline!");
            event.emit("search");
            return connected;
        }
        Tools.log("Sending "+key+"...");
        new Thread(new Runnable() {
            public void run() {
                conn(key,target);
            }
        }).start();
        return true;
    }
    private Socket conn(String command,String ip) {
        Socket client;

        try{
            //Will throw connection refused
            client = new Socket(ip, port);
            PrintWriter out = new PrintWriter(client.getOutputStream(), true);
            //Send the chunks
            out.println(firstPacket()+secondPacket(command));
            client.setSoTimeout(timeout);
            client.close();
        } catch(UnknownHostException e) {
            Tools.log("Disconnected: Unkown Host");
            onDisconnect(e);
        } catch(IOException e) {
            Tools.log("No I/O - Offline?");
            onDisconnect(e);
        }
        return null;
    }

    private void onDisconnect(Exception e) {
        Tools.log("E: "+e.getMessage());
        connected=false;
        event.emit("search");
    }

    public RC(String ip,String mac,String name,EventEmitter cb) {
        this.ip=ip;
        this.mac=mac;
        this.name=name;
        this.event =cb;
        event.on("keysend", new EventListener() {
            @Override
            public void onEvent(Object... objects) {
                try {
                    send(new RCKey((String) objects[0]));
                } catch(Exception e) {
                    Tools.log("E: "+e.toString());
                }
            }
        });
    }
}
