package mkg20001.net.samremote;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import net.nodestyle.events.EventEmitter;

import mkg20001.net.samremotecommon.PushButton;

import static mkg20001.net.samremotecommon.Tools.log;

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
                .setView(createLayout(R.layout.activity_remote_dialog,this._context))

                // OnAttach doesn't get called on the dialog;
                // we have to apply our click event handlers here.
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                log("Open in App");
                                _listener.onDialogNegativeClick(QSDialog.this);
                            }
                        })
                .setPositiveButton(actionButtonText,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                log("Close QS");
                                dialog.dismiss();

                                _listener.onDialogPositiveClick(QSDialog.this);
                            }
                        });

        return initAll(alertBuilder.create());
    }

    private View createLayout(int layoutid,Context context) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(layoutid, null);

        for (Integer i:new Integer[]{
                R.id.dialog_poweroff,
                R.id.dialog_left,R.id.dialog_right,R.id.dialog_down,R.id.dialog_up,R.id.dialog_ok,
                R.id.dialog_volup,R.id.dialog_voldown,R.id.dialog_chup,R.id.dialog_chdown,
                R.id.dialog_enter,R.id.dialog_back,R.id.dialog_exit,
                R.id.dialog_menu,R.id.dialog_hdmi,R.id.dialog_source
        }) {
            View b=layout.findViewById(i);
            if (b==null) {
                log("KEY ERROR - IS ZERO: "+i);
            } else {
                b.setOnClickListener(keyClick);
            }
        }

        //Power
        new PushButton(R.id.dialog_poweroff,"poweroff",event);
        //Dir
        new PushButton(R.id.dialog_left,"left",event);
        new PushButton(R.id.dialog_right,"right",event);
        new PushButton(R.id.dialog_down,"down",event);
        new PushButton(R.id.dialog_up,"up",event);
        new PushButton(R.id.dialog_ok,"enter",event);
        //Vol/CH
        new PushButton(R.id.dialog_volup,"volup",event);
        new PushButton(R.id.dialog_voldown,"voldown",event);
        new PushButton(R.id.dialog_chup,"chup",event);
        new PushButton(R.id.dialog_chdown,"chdown",event);
        //Main
        new PushButton(R.id.dialog_enter,"enter",event);
        new PushButton(R.id.dialog_back,"return",event);
        new PushButton(R.id.dialog_exit,"exit",event);
        //Special
        new PushButton(R.id.dialog_menu,"menu",event);
        new PushButton(R.id.dialog_hdmi,"hdmi",event);
        new PushButton(R.id.dialog_source,"source",event);

        return layout;
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
        return d;
    }
}