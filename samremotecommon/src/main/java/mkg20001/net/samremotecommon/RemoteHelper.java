package mkg20001.net.samremotecommon;

import net.nodestyle.events.EventEmitter;
import net.nodestyle.events.EventListener;

public class RemoteHelper {
    boolean isSearch=false;
    RemoteHelperView rh;
    RC remote=null;
    EventEmitter event;
    String subnet=null;
    public RemoteHelper(RemoteHelperView r,EventEmitter e) {
        this.rh=r;
        this.event=e;
        event.on("search", new EventListener() {
            @Override
            public void onEvent(java.lang.Object... objects) {
                remote=rh.getRemote();
                Tools.log("Search....");
                if (isSearch) {
                    Tools.log("Already searching...");
                    return;
                }
                new Thread(new Runnable() {
                    public void run() {
                        isSearch=true;
                        event.emit("state.change",R.drawable.loading,R.string.searching);

                        rh.setOffline(false); //we are looking for a tv
                        if (subnet==null) {
                            String[] sp=rh.getIPAddress().split("\\.");
                            try {
                                subnet=sp[0]+"."+sp[1]+"."+sp[2]+".";
                            } catch(Exception e) {
                                Tools.log("Using default subnet...");
                                subnet="192.169.178.";
                            }
                            Tools.log("Using subnet "+subnet);
                        }
                        String ips[];
                        if (rh.getDebug()) {
                            ips=new String[]{"127.0.0.1","192.168.178.25",rh.getIP()}; //hardcode the ip - won't work with ping
                        } else {
                            ips = new String[]{rh.getIP()}; //get last ip
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
                            ips=Tools.doScan(subnet);
                            lookfor:
                            for (String tv:ips) {
                                if (remote.connect(tv)) {
                                    found=true;
                                    rh.saveIP(tv);
                                    break lookfor;
                                }
                            }
                        }
                        final boolean f=found;
                        Tools.log(f?"Connected!":"No host found...");
                        if (f) {
                            event.emit("state.change",R.drawable.ok2,R.string.found);
                            final Integer cState=rh.curState;
                            if (rh.curState.compareTo(cState)<=0) {
                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e) {
                                    Tools.log("Can't delay");
                                }

                                Tools.log("C:"+rh.curState.compareTo(cState)+":"+rh.curState);
                                event.emit("state.change",R.drawable.ic_remote,R.string.about);
                                if (rh.curState.compareTo(cState)<=1) {
                                    try {
                                        Thread.sleep(2000);
                                    } catch (InterruptedException e) {
                                        Tools.log("Can't delay");
                                    }
                                    Tools.log("C:"+rh.curState.compareTo(cState)+":"+rh.curState);
                                    event.emit("state.change",R.drawable.ic_remote,R.string.empty);
                                }
                            }
                        } else {
                            event.emit("state.change",R.drawable.error,R.string.not_found_title);
                            //no tv found=offline
                            rh.setOffline(true);

                            event.emit("search.dialog");
                        }
                        isSearch=false;
                    }
                }).start();
            }
        });
    }
}
