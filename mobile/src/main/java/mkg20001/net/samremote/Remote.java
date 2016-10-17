package mkg20001.net.samremote;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.view.View;
import android.widget.TextView;

import net.nodestyle.events.EventEmitter;
import net.nodestyle.events.EventListener;

import mkg20001.net.samremotecommon.PushButton;
import mkg20001.net.samremotecommon.RC;
import mkg20001.net.samremotecommon.RemoteHelper;
import mkg20001.net.samremotecommon.RemoteHelperView;
import mkg20001.net.samremotecommon.Tools;

public class Remote extends AppCompatActivity implements RemoteHelperView {

    /* implements */
    private RC remote=null;
    public RC getRemote() {
        return remote;
    }
    public boolean getDebug() {
        return isDebug;
    }
    private Integer curState=0;
    private boolean isOffline=true;
    private static ColorFilter whiteFilter=Tools.filter();
    @Override
    public void setOffline(boolean s) {
        isOffline=s;
    }

    private TextView state;
    private FloatingActionButton stateIcon;
    private final EventEmitter event=new EventEmitter();
    private final Integer target=Build.VERSION.SDK_INT;
    private final boolean mplus=target>=23;

    private final View.OnClickListener keyClick = new View.OnClickListener() {
        public void onClick(View v) {
            event.emit("keyclick."+v.getId());
            event.emit("keyclick",v);
        }
    };
    private final View.OnClickListener stateClick = new View.OnClickListener() {
        public void onClick(View v) {
            if (isOffline) event.emit("search");
        }
    };

    public String getIPAddress() {
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void setIcon(FloatingActionButton icon, Drawable draw) {
        icon.setForeground(draw);
    }

    private String getMACAddress() {
        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        return info.getMacAddress();
    }

    private boolean isDebug=false;

    private void checkForDebugMode() {
        isDebug=false;
        isDebug=Build.FINGERPRINT.startsWith("Android/sdk_")||Build.FINGERPRINT.startsWith("generic");
        if (isDebug) Tools.log("Debug Mode (Emulator Mode)");
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
                for (Integer i:new Integer[]{
                        R.id.key_poweroff,
                        R.id.key_left,R.id.key_right,R.id.key_down,R.id.key_up,R.id.key_ok,
                        R.id.key_volup,R.id.key_voldown,R.id.key_chup,R.id.key_chdown,
                        R.id.key_enter,R.id.key_back,R.id.key_exit,
                        R.id.key_menu,R.id.key_hdmi,R.id.key_source
                }) {
                    View b=findViewById(i);
                    if (b==null) {
                        Tools.log("KEY ERROR - IS ZERO: "+i);
                    } else {
                        b.setOnClickListener(keyClick);
                    }
                }

                //Power
                new PushButton(R.id.key_poweroff,"poweroff",event);
                //Dir
                new PushButton(R.id.key_left,"left",event);
                new PushButton(R.id.key_right,"right",event);
                new PushButton(R.id.key_down,"down",event);
                new PushButton(R.id.key_up,"up",event);
                new PushButton(R.id.key_ok,"enter",event);
                //Vol/CH
                new PushButton(R.id.key_volup,"volup",event);
                new PushButton(R.id.key_voldown,"voldown",event);
                new PushButton(R.id.key_chup,"chup",event);
                new PushButton(R.id.key_chdown,"chdown",event);
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
                        Drawable draw = ContextCompat.getDrawable(Remote.this,(int) objects[0]);
                        draw.setColorFilter(whiteFilter);
                        if (mplus) {
                            setIcon(icon,draw);
                        } else {
                            icon.setImageDrawable(draw);
                        }
                        Tools.log("Image set to "+objects[0]);
                        int id=(int) objects[1];
                        stat.setText(id);
                        curState++;
                    }
                }));
            }
        });
        new RemoteHelper(Remote.this,event,isDebug);
        Tools.log("Emit start...");
        event.emit("startup");
    }

    private SharedPreferences getPref() {
        Context context = Remote.this;
        return context.getSharedPreferences("mkg20001.net.samremote.settings", Context.MODE_PRIVATE);
    }

    public void saveIP(String ip) {
        getPref().edit().putString("last_ip", ip).apply();
    }

    public String getIP() {
        return getPref().getString("last_ip", "127.0.0.1");
    }
}
