package mkg20001.net.samremote;

import net.nodestyle.events.EventEmitter;
import net.nodestyle.events.EventListener;

public class PushButton {
    public PushButton(int keyid,final String key,final EventEmitter e) {
        e.on("keyclick." + keyid, new EventListener() {
            @Override
            public void onEvent(Object... objects) {
                e.emit("keysend",key);
            }
        });
    }
}
