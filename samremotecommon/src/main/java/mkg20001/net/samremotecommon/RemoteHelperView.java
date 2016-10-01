package mkg20001.net.samremotecommon;

/**
 * Created by maciej on 01.10.16.
 */

public interface RemoteHelperView {
    Integer curState=0;
    void saveIP(String ip);
    String getIP();
    boolean isOffline=true;
    boolean isDebug=false;
    void setOffline(boolean s);
    RC getRemote();
}
