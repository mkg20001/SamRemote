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
import mkg20001.net.samremotecommon.Tools;

public class Remote extends AppCompatActivity {

    TextView state;
    RC remote;
    FloatingActionButton stateIcon;
    EventEmitter event=new EventEmitter();
    Object keys=new Object();
    Integer target=Build.VERSION.SDK_INT;
    boolean mplus=target>=23;
    Integer curState=0;
    boolean isSearch=false;
    //static final int AccessWifi = 3;
    private boolean isOffline=true;

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

    boolean ISDEBUGMODE=false;

    private void checkForDebugMode() {
        //ISDEBUGMODE = (Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID) == null);

        ISDEBUGMODE=false;
        ISDEBUGMODE=Build.FINGERPRINT.startsWith("generic");
        //String androidID = ...;
        //if(androidID == null || androidID.equals("9774D56D682E549C"))


        /*TelephonyManager man = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        if(man != null){
            String devId = man.getDeviceSoftwareVersion();
            ISDEBUGMODE = (devId == null);
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
        event.on("search", new EventListener() {
            @Override
            public void onEvent(java.lang.Object... objects) {
                if (isSearch) {
                    Tools.log("Already searching...");
                    return;
                }
                new Thread(new Runnable() {
                    private final TextView stat=state;
                    private final FloatingActionButton icon=stateIcon;
                    public void run() {
                        isSearch=true;
                        event.emit("state.change",R.drawable.loading,R.string.searching);
                        isOffline=false; //we are looking for a tv
                        String ips[];
                        if (ISDEBUGMODE) {
                            ips=new String[]{"127.0.0.1","192.168.178.25",getIP()}; //get last ip
                            Tools.log("ISDEBUG!");
                            Tools.log("ISDEBUG!");
                            Tools.log("ISDEBUG!");
                        } else {
                            ips = new String[]{getIP()}; //get last ip
                        }
                        boolean found=false;
                        lookfor:
                        for (String tv:ips) {
                            if (remote.connect(tv)) {
                                found=true;
                                //no need to save - loaded from file
                                break lookfor;
                            }
                        }
                        if (!found) {
                            doScan();
                            lookfor:
                            for (java.lang.Object tv:Remote.this.ips.getItems()) {
                                if (remote.connect((String) tv)) {
                                    found=true;
                                    saveIP((String) tv);
                                    break lookfor;
                                }
                            }
                        }
                        final boolean f=found;
                        Tools.log(f?"Connected!":"No host found...");
                        runOnUiThread(new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if (f) {
                                    stat.setText(R.string.about);
                                    event.emit("state.change",R.drawable.ok2,R.string.found);
                                    final Integer cState=curState;
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (curState.compareTo(cState)<=0) {
                                                try {
                                                    Thread.sleep(2000);
                                                } catch (InterruptedException e) {
                                                    Tools.log("Can't delay");
                                                }

                                                Tools.log("C:"+curState.compareTo(cState)+":"+curState);
                                                event.emit("state.change",R.drawable.ic_remote,R.string.about);
                                                if (curState.compareTo(cState)<=1) {
                                                    try {
                                                        Thread.sleep(2000);
                                                    } catch (InterruptedException e) {
                                                        Tools.log("Can't delay");
                                                    }
                                                    Tools.log("C:"+curState.compareTo(cState)+":"+curState);
                                                    event.emit("state.change",R.drawable.ic_remote,R.string.empty);
                                                }
                                                }
                                        }
                                    }).start();
                                } else {
                                    event.emit("state.change",R.drawable.error,R.string.not_found_title);
                                    //no tv found=offline
                                    isOffline=true;

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
                                isSearch=false;
                            }
                        }));
                    }
                }).start();
            }
        });
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
            e.printStackTrace();
            return "";
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static final int NB_THREADS = 25;
    public Array ips=new Array();

    public void doScan() {
        ips.clear();
        String LOG_TAG="loggg";
        Log.i(LOG_TAG, "Start scanning");

        ExecutorService executor = Executors.newFixedThreadPool(NB_THREADS);
        for(int dest=0; dest<255; dest++) {
            String host = "192.168.178." + dest;
            executor.execute(pingRunnable(host));
        }

        Log.i(LOG_TAG, "Waiting for executor to terminate...");
        executor.shutdown();
        try { executor.awaitTermination(60*1000, TimeUnit.MILLISECONDS); } catch (InterruptedException ignored) { }

        Log.i(LOG_TAG, "Scan finished");
    }

    private Runnable pingRunnable(final String host) {
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
