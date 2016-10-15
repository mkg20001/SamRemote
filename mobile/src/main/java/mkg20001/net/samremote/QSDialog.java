package mkg20001.net.samremote;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import net.nodestyle.events.EventEmitter;

import mkg20001.net.samremotecommon.PushButton;
import mkg20001.net.samremotecommon.Tools;

public class QSDialog
        extends DialogFragment {

    private Context _context;
    private QSDialogListener _listener;

    private EventEmitter event;

    /**
     * An inner class used to pass context into the dialog.
     */
    public static class Builder {

        private final Context _context;
        private QSDialogListener _listener;
        private EventEmitter event;

        Builder(Context context){
            this._context = context;
        }

        Builder setClickListener(QSDialogListener listener) {
            if (listener != null) {
                this._listener = listener;
            }
            return this;
        }

        Builder setEvent(EventEmitter e) {
            this.event=e;
            return this;
        }

        QSDialog create() {
            return new QSDialog()
                    .setContext(this._context)
                    .setEvent(event)
                    .setClickListener(this._listener);
        }
    }

    /**
     * A public interface for communication between the
     * dialog and the QSDialogService.
     */
    public interface QSDialogListener {
        void onDialogPositiveClick(DialogFragment dialog);
        void onDialogNegativeClick(DialogFragment dialog);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedState){

        // Read the saved state data passed in.
        /*boolean isTileActive = false;
        if (savedState.containsKey(TILE_STATE_KEY)) {
            isTileActive = savedState.getBoolean(TILE_STATE_KEY);
        }*/

        int actionButtonText = R.string.ok;

        AlertDialog.Builder alertBuilder =
                new AlertDialog.Builder(this._context);

        alertBuilder
                .setView(R.layout.activity_remote_dialog)

                // OnAttach doesn't get called on the dialog;
                // we have to apply our click event handlers here.
                /*.setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Log.d("QS", "Dialog cancel");
                                _listener.onDialogNegativeClick(QSDialog.this);
                            }
                        })*/
                .setPositiveButton(actionButtonText,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Log.d("QS", "Dialog action taken");
                                dialog.dismiss();

                                _listener.onDialogPositiveClick(QSDialog.this);
                            }
                        });

        return initAll(alertBuilder.create());
    }

    private QSDialog setClickListener(QSDialogListener listener) {
        this._listener = listener;
        return this;
    }

    private QSDialog setEvent(EventEmitter e) {
        this.event=e;
        return this;
    }

    private QSDialog setContext(Context context){
        this._context = context;
        return this;
    }

    private final View.OnClickListener keyClick = new View.OnClickListener() {
        public void onClick(View v) {
            event.emit("keyclick."+v.getId());
            event.emit("keyclick",v);
        }
    };

    private Dialog initAll(Dialog d) {
        // TODO: 09.10.16 init buttons
        for (Integer i:new Integer[]{
                /*R.id.key_poweroff,
                R.id.key_left,R.id.key_right,R.id.key_down,R.id.key_up,R.id.key_ok,
                R.id.key_volup,R.id.key_voldown,R.id.key_chup,R.id.key_chdown,
                R.id.key_enter,R.id.key_back,R.id.key_exit,
                R.id.key_menu,R.id.key_hdmi,R.id.key_source*/
                R.id.key2_enter
        }) {
            View b=d.findViewById(i);
            if (b==null) {
                Tools.log("KEY ERROR - IS ZERO: "+i);
            } else {
                b.setOnClickListener(keyClick);
            }
        }

        /*//Power
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
        new PushButton(R.id.key_chdown,"chdown",event);*/
        //Main
        new PushButton(R.id.key2_enter,"enter",event);
        /*new PushButton(R.id.key_back,"return",event);
        new PushButton(R.id.key_exit,"exit",event);
        //Special
        new PushButton(R.id.key_menu,"menu",event);
        new PushButton(R.id.key_hdmi,"hdmi",event);
        new PushButton(R.id.key_source,"source",event);*/
        return d;
    }
}