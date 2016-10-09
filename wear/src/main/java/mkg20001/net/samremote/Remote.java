package mkg20001.net.samremote;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.text.format.Formatter;
import android.view.View;
import android.widget.TextView;

import net.nodestyle.events.EventEmitter;
import net.nodestyle.events.EventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import mkg20001.net.samremotecommon.PushButton;
import mkg20001.net.samremotecommon.RC;
import mkg20001.net.samremotecommon.RemoteHelper;
import mkg20001.net.samremotecommon.RemoteHelperView;
import mkg20001.net.samremotecommon.Tools;

public class Remote extends WearableActivity implements RemoteHelperView {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;

    /* implements */
    RC remote=null;
    public RC getRemote() {
        return remote;
    }
    public boolean getDebug() {
        return isDebug;
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

    boolean isDebug=false;

    private void checkForDebugMode() {
        isDebug=false;
        isDebug=Build.FINGERPRINT.startsWith("Android/sdk_")||Build.FINGERPRINT.startsWith("generic");
        if (isDebug) Tools.log("Debug Mode (Emulator Mode)");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.text);
        mClockView = (TextView) findViewById(R.id.clock);

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


        final View padVolch=findViewById(R.id.volch);
        final View padNav=findViewById(R.id.nav);
        padVolch.setVisibility(View.INVISIBLE);

        findViewById(R.id.key_ok).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                padNav.setVisibility(View.INVISIBLE);
                padVolch.setVisibility(View.VISIBLE);
                return true;
            }
        });
        findViewById(R.id.key_change).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                padNav.setVisibility(View.VISIBLE);
                padVolch.setVisibility(View.INVISIBLE);
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
                        //icon.setForeground(getResources().getDrawable((int) objects[0])); min:23
                        Drawable draw=getResources().getDrawable((int) objects[0]);
                        icon.setForeground(draw);
                        Tools.log("Image set to "+objects[0]);
                        stat.setText((int) objects[1]);
                        icon.setColorFilter(R.color.light);
                        curState++;
                    }
                }));
            }
        });
        new RemoteHelper(Remote.this,event,isDebug);
        Tools.log("Emit start?...");
        event.emit("startup");
    }

    public void saveIP(String ip) {
        Remote.this.getPreferences(Context.MODE_PRIVATE).edit().putString("last_ip", ip).commit();
    }

    public String getIP() {
        return Remote.this.getPreferences(Context.MODE_PRIVATE).getString("last_ip", "127.0.0.1");
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mTextView.setTextColor(getResources().getColor(android.R.color.white));
            mClockView.setVisibility(View.VISIBLE);

            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
            mTextView.setTextColor(getResources().getColor(android.R.color.black));
            mClockView.setVisibility(View.GONE);
        }
    }
}
