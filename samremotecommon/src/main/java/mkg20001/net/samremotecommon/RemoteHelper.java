package mkg20001.net.samremotecommon;

import net.nodestyle.events.EventEmitter;
import net.nodestyle.events.EventListener;

public class RemoteHelper {
    private boolean isSearch=false;
    private final RemoteHelperView rh;
    private RC remote=null;
    private final EventEmitter event;
    private String subnet=null;
    private Boolean ping=false;
    private Boolean isQS=false;
    public RemoteHelper(RemoteHelperView r,EventEmitter e,Boolean s,Boolean isQS) {
        this.isQS=isQS;
        ping=!s; //isDebug = true => !true=false
        this.rh=r;
        this.event=e;
        m();
    }
    public RemoteHelper(RemoteHelperView r,EventEmitter e,Boolean s) {
        ping=!s; //isDebug = true => !true=false
        this.rh=r;
        this.event=e;
        m();
    }
    private void m() {
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
                        event.emit("state.change",R.drawable.loading,isQS?R.string.searching_:R.string.searching);

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
                            //ips=new String[]{"127.0.0.1"}; //for offline testing
                            ips=new String[]{"127.0.0.1","192.168.178.25",rh.getIP()}; //hardcode the ip - won't work with ping
                        } else {
                            ips = new String[]{rh.getIP()}; //get last ip
                        }
                        boolean found=false;
                        for (String tv:ips) {
                            if (remote.connect(tv)) {
                                found=true;
                                //no need to save - loaded from file
                                break;
                            }
                        }
                        if (!found) {
                            ips=ping?Tools.doScan(subnet):new String[0];
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
                            event.emit("search.done", true);
                            final Integer cState= RemoteHelperView.curState;
                            if (RemoteHelperView.curState.compareTo(cState)<=0) {
                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e) {
                                    Tools.log("Can't delay");
                                }

                                Tools.log("C:"+ RemoteHelperView.curState.compareTo(cState)+":"+ RemoteHelperView.curState);
                                event.emit("state.change",R.drawable.ic_remote,isQS?R.string.remote_online:R.string.about);
                                if (!isQS) {
                                    if (RemoteHelperView.curState.compareTo(cState)<=1) {
                                        try {
                                            Thread.sleep(2000);
                                        } catch (InterruptedException e) {
                                            Tools.log("Can't delay");
                                        }
                                        Tools.log("C:"+ RemoteHelperView.curState.compareTo(cState)+":"+ RemoteHelperView.curState);
                                        event.emit("state.change",R.drawable.ic_remote,R.string.empty);
                                    }
                                }
                            }
                        } else {
                            //no tv found=offline
                            rh.setOffline(true);
                            event.emit("state.change",isQS?R.drawable.ic_remote:R.drawable.error,isQS?R.string.not_found_short:R.string.not_found_title);
                            event.emit("search.done", false);
                            event.emit("search.dialog");
                        }
                        isSearch=false;
                    }
                }).start();
            }
        });
    }
}
