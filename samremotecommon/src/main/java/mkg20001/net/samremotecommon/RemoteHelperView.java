package mkg20001.net.samremotecommon;

public interface RemoteHelperView {
    Integer curState=0;
    void saveIP(String ip);
    String getIP();
    boolean isDebug=false;
    void setOffline(boolean s);
    RC getRemote();
}
