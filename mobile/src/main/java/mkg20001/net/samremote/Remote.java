package mkg20001.net.samremote;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import net.nodestyle.events.EventEmitter;
import net.nodestyle.events.EventListener;
import net.nodestyle.helper.Array;
import net.nodestyle.helper.Object;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import mkg20001.net.samremotecommon.PushButton;
import mkg20001.net.samremotecommon.RC;
import mkg20001.net.samremotecommon.RemoteHelper;
import mkg20001.net.samremotecommon.RemoteHelperView;
import mkg20001.net.samremotecommon.Tools;

public class Remote extends AppCompatActivity implements RemoteHelperView {

    /* implements */
    RC remote=null;
    public RC getRemote() {
        return remote;
    }
    Integer curState=0;
    boolean isOffline=true;
    @Override
    public void setOffline(boolean s) {
        isOffline=s;
    }

    TextView state;
    FloatingActionButton stateIcon;
    EventEmitter event=new EventEmitter();
    Object keys=new Object();
    Integer target=Build.VERSION.SDK_INT;
    boolean mplus=target>=23;

    private View.OnClickListener keyClick = new View.OnClickListener() {
        public void onClick(View v) {
            event.emit("keyclick."+v.getId());
            event.emit("keyclick",v);
        }
    };
    private View.OnClickListener stateClick = new View.OnClickListener() {
        public void onClick(View v) {
            if (isOffline) event.emit("search");
        }
    };

    public String getIPAddress() {
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    }

    public String getMACAddress() {
        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        return info.getMacAddress();
    }

    public String   s_dns1 ;
    public String   s_dns2;
    public String   s_gateway;
    public String   s_ipAddress;
    public String   s_leaseDuration;
    public String   s_netmask;
    public String   s_serverAddress;
    DhcpInfo d;
    WifiManager wifii;

    boolean isDebug=false;

    private void checkForDebugMode() {
        //isDebug = (Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID) == null);

        isDebug=false;
        isDebug=Build.FINGERPRINT.startsWith("generic");
        //String androidID = ...;
        //if(androidID == null || androidID.equals("9774D56D682E549C"))


        /*TelephonyManager man = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        if(man != null){
            String devId = man.getDeviceSoftwareVersion();
            isDebug = (devId == null);
        }*/
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_remote);

        checkForDebugMode();

        state=(TextView) findViewById(R.id.state);
        stateIcon=(FloatingActionButton) findViewById(R.id.stateFAB);
        stateIcon.setOnClickListener(stateClick);
        stateIcon.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (isOffline) return false;
                Intent intent = new Intent(Remote.this, AboutActivity.class);
                startActivity(intent);
                return true;
            }
        });
        event.on("startup", new EventListener() {
            @Override
            public void onEvent(java.lang.Object... objects) {
                Tools.log("Startup...");
                //Register RC
                remote = new RC(getIPAddress(), getMACAddress(), Tools.getDeviceName(),event);
                //Register buttons
                for (Integer i:new Integer[]{R.id.key_back,R.id.key_chlist,R.id.key_down,R.id.key_enter,R.id.key_exit,R.id.key_hdmi,R.id.key_left,R.id.key_menu,R.id.key_ok,R.id.key_poweroff,R.id.key_right,R.id.key_source,R.id.key_up}) {
                    View b=findViewById(i);
                    if (b==null) {
                        Tools.log("KEY ERROR - IS ZERO: "+i);
                    } else {
                        b.setOnClickListener(keyClick);
                    }
                }

                wifii= (WifiManager) getSystemService(Context.WIFI_SERVICE);
                d=wifii.getDhcpInfo();

                s_dns1="DNS 1: "+String.valueOf(d.dns1);
                s_dns2="DNS 2: "+String.valueOf(d.dns2);
                s_gateway="Default Gateway: "+String.valueOf(d.gateway);
                s_ipAddress="IP Address: "+String.valueOf(d.ipAddress);
                s_leaseDuration="Lease Time: "+String.valueOf(d.leaseDuration);
                s_netmask="Subnet Mask: "+String.valueOf(d.netmask);
                s_serverAddress="Server IP: "+String.valueOf(d.serverAddress);

                //display them
                Tools.log("Network Info\n"+s_dns1+"\n"+s_dns2+"\n"+s_gateway+"\n"+s_ipAddress+"\n"+s_leaseDuration+"\n"+s_netmask+"\n"+s_serverAddress);

                //Power
                new PushButton(R.id.key_poweroff,"poweroff",event);
                //Dir
                new PushButton(R.id.key_left,"left",event);
                new PushButton(R.id.key_right,"right",event);
                new PushButton(R.id.key_down,"down",event);
                new PushButton(R.id.key_up,"up",event);
                new PushButton(R.id.key_ok,"enter",event);
                //Main
                new PushButton(R.id.key_enter,"enter",event);
                new PushButton(R.id.key_back,"return",event);
                new PushButton(R.id.key_exit,"exit",event);
                //Special
                new PushButton(R.id.key_menu,"menu",event);
                new PushButton(R.id.key_hdmi,"hdmi",event);
                new PushButton(R.id.key_source,"source",event);
                event.emit("search");
            }
        });
        checkForDebugMode();
        event.on("search.dialog", new EventListener() {
            @Override
            public void onEvent(java.lang.Object... objects) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 1. Instantiate an AlertDialog.Builder with its constructor
                        AlertDialog.Builder builder = new AlertDialog.Builder(Remote.this);

                        // 2. Chain together various setter methods to set the dialog characteristics
                        builder.setMessage(R.string.not_found)
                                .setTitle(R.string.not_found_title);
                        // Add the buttons
                        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //close
                            }
                        });
                        builder.setNegativeButton(R.string.search, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                event.emit("search");
                            }
                        });

                        // Create the AlertDialog
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                });
            }
        });
        event.on("state.change", new EventListener() {
            @Override
            public void onEvent(final java.lang.Object... objects) {
                runOnUiThread(new Thread(new Runnable() {
                    private final TextView stat=state;
                    private final FloatingActionButton icon=stateIcon;
                    @Override
                    public void run() {
                        //icon.setForeground(getResources().getDrawable((int) objects[0])); min:23
                        if (!mplus) {
                            if (((Integer) objects[0]).toString().equalsIgnoreCase(((Integer) R.drawable.ic_remote).toString()))
                                objects[0] = R.drawable.ic_remote_svg; //fix ugly icon
                        }
                        Drawable draw=getResources().getDrawable((int) objects[0]);
                        if (mplus) {
                            icon.setForeground(draw);
                        } else {
                            icon.setImageDrawable(draw);
                        }
                        Tools.log("Image set to "+objects[0]);
                        stat.setText((int) objects[1]);
                        icon.setColorFilter(R.color.light);
                        curState++;
                    }
                }));
            }
        });
        new RemoteHelper(Remote.this,event);
        Tools.log("Emit start?...");
        event.emit("startup");
    }

    public void saveIP(String ip) {
        String FILENAME = "last_ip";
        FileOutputStream fos;
        try {
            fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
            fos.write(ip.getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getIP() {
        FileInputStream fis;
        try {
            fis = openFileInput("last_ip");
            StringBuilder fileContents = new StringBuilder("");

            byte[] buffer = new byte[1024];

            int n;
            while ((n = fis.read(buffer)) != -1)
            {
                fileContents.append(new String(buffer, 0, n));
            }
            return fileContents.toString();
        } catch (FileNotFoundException e) {
            Tools.log("No saved ip....");
            return "127.0.0.1";
        } catch (IOException e) {
            e.printStackTrace();
            return "127.0.0.1";
        }
    }
}
