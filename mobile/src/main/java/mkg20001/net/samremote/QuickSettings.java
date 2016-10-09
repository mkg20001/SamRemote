package mkg20001.net.samremote;

import android.annotation.TargetApi;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
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

import static mkg20001.net.samremotecommon.Tools.log;

/**
 * Created by maciej on 09.10.16.
 */
@TargetApi(Build.VERSION_CODES.N)
public class QuickSettings extends TileService implements RemoteHelperView {

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

    Boolean firstRun=true;

    public SharedPreferences getPref() {
        Context context = QuickSettings.this;
        return context.getSharedPreferences("mkg20001.net.samremote.settings", Context.MODE_PRIVATE);
    }

    public String getIPAddress() {
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    }

    public String getMACAddress() {
        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        return info.getMacAddress();
    }

    public void saveIP(String ip) {
        getPref().edit().putString("last_ip", ip).commit();
    }

    public String getIP() {
        return getPref().getString("last_ip", "127.0.0.1");
    }

    boolean isDebug=false;

    private void checkForDebugMode() {
        isDebug=false;
        isDebug=Build.FINGERPRINT.startsWith("Android/sdk_")||Build.FINGERPRINT.startsWith("generic");
        if (isDebug) Tools.log("Debug Mode (Emulator Mode)");
    }

    EventEmitter event=new EventEmitter();
    /**
     * Called when the tile is added to the Quick Settings.
     * @return TileService constant indicating tile state
     */

    @Override
    public void onTileAdded() {
        log("Tile added");
        init();
    }

    public void init() {
        if (firstRun) {
            firstRun=false;
            once();
        }
    }

    public void once() {
        event.on("startup", new EventListener() {
            @Override
            public void onEvent(java.lang.Object... objects) {
                Tools.log("Startup...");
                //Register RC
                remote = new RC(getIPAddress(), getMACAddress(), Tools.getDeviceName(),event);
                //event.emit("search"); - do this later
            }
        });
        checkForDebugMode();
        event.on("search.dialog", new EventListener() {
            @Override
            public void onEvent(java.lang.Object... objects) {
                /*runOnUiThread(new Runnable() {
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
                });*/
            }
        });
        event.on("state.change", new EventListener() {
            @Override
            public void onEvent(final java.lang.Object... objects) {
                log("Change stat");
                Tile tile=getQsTile();
                if (objects[0].toString().equalsIgnoreCase(((Integer) R.drawable.ic_remote).toString()))
                    objects[0] = R.drawable.ic_remote_svg; //fix ugly icon
                tile.setIcon(Icon.createWithResource(getApplicationContext(),(int) objects[0]));
                Tools.log("Icon set to "+objects[0]);
                int id=(int) objects[1];
                tile.setLabel(getString(id));
                tile.setState(isOffline?Tile.STATE_INACTIVE:Tile.STATE_ACTIVE);
                tile.updateTile();
                curState++;
            }
        });
        new RemoteHelper(QuickSettings.this,event,isDebug,true);
        Tools.log("Emit start...");
        event.emit("startup");
        // TODO: 09.10.16 connect to events - if not already
    }

    /**
     * Called when this tile begins listening for events.
     */
    @Override
    public void onStartListening() {
        init();
        log("Start listening");
        if (isOffline) {
            log("LOL");
            event.emit("search");
            event.once("search.done", new EventListener() {
                @Override
                public void onEvent(Object... objects) {
                    //init();
                }
            });
        } else {
            log("GG");
            //init();
        }
    }

    /**
     * Called when the user taps the tile.
     */
    Boolean isOn=false;

    @Override
    public void onClick(){

        // Get the tile's current state.
        //Tile tile = getQsTile();
        //isTileActive = (tile.getState() == Tile.STATE_ACTIVE);

        EventListener e=new EventListener() {
            @Override
            public void onEvent(Object... objects) {
                QSDialog.Builder dialogBuilder = new QSDialog.Builder(getApplicationContext());

                QSDialog dialog = dialogBuilder
                        .setEvent(event)
                        .setClickListener(new QSDialog.QSDialogListener() {
                            @Override
                            public void onDialogPositiveClick(DialogFragment dialog) {
                                log("Positive registed");

                                // The user wants to change the tile state.
                                //isTileActive = !isTileActive;
                                updateTile();
                            }

                            @Override
                            public void onDialogNegativeClick(DialogFragment dialog) {
                                log("Negative registered");

                                // The user is cancelled the dialog box.
                                // We can't do anything to the dialog box here,
                                // but we can do any cleanup work.
                            }
                        })
                        .create();

                // Pass the tile's current state to the dialog.
                Bundle args = new Bundle();
                //args.putBoolean(QSDialog.TILE_STATE_KEY, isTileActive);

                QuickSettings.this.showDialog(dialog.onCreateDialog(args));
            }
        };

        if (isOn&&isOffline) {
            isOn=false;
        } else if (isOn&&!isOffline) {
            e.onEvent();
        } else if (!isOn) {
            //turn on

        }
    }

    /**
     * Called when this tile moves out of the listening state.
     */
    @Override
    public void onStopListening() {
        log("Stop Listening");
    }

    /**
     * Called when the user removes this tile from Quick Settings.
     */
    @Override
    public void onTileRemoved() {
        log("Tile removed");
    }

    // Changes the appearance of the tile.
    private void updateTile() {

        /*Tile tile = this.getQsTile();
        boolean isActive = getServiceStatus();

        Icon newIcon;
        String newLabel;
        int newState;

        // Change the tile to match the service status.
        if (isActive) {

            newLabel = String.format(Locale.US,
                    "%s %s",
                    getString(R.string.tile_label),
                    getString(R.string.service_active));

            newIcon = Icon.createWithResource(getApplicationContext(),
                    ic_android_black_24dp);

            newState = Tile.STATE_ACTIVE;

        } else {
            newLabel = String.format(Locale.US,
                    "%s %s",
                    getString(R.string.tile_label),
                    getString(R.string.service_inactive));

            newIcon =
                    Icon.createWithResource(getApplicationContext(),
                            android.R.drawable.ic_dialog_alert);

            newState = Tile.STATE_INACTIVE;
        }

        // Change the UI of the tile.
        tile.setLabel(newLabel);
        tile.setIcon(newIcon);
        tile.setState(newState);

        // Need to call updateTile for the tile to pick up changes.
        tile.updateTile();*/
    }

    // Access storage to see how many times the tile
    // has been tapped.
    /*private boolean getServiceStatus() {

        SharedPreferences prefs =
                getApplicationContext()
                        .getSharedPreferences(PREFERENCES_KEY,
                                MODE_PRIVATE);

        boolean isActive = prefs.getBoolean(SERVICE_STATUS_FLAG, false);
        isActive = !isActive;

        prefs.edit().putBoolean(SERVICE_STATUS_FLAG, isActive).apply();

        return isActive;
    }*/
}
