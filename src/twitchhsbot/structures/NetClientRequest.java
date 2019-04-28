
package twitchhsbot.structures;


public class NetClientRequest {
    
    public enum RequestType {
        PUT_REQUEST,
        POST_REQUEST
    }

    private String netAddress;
    private String netPath;
    private int netPort;
    private String netData;
    private String netContentType;
    private String[][] netHeaders;
    private boolean secureConnection;

    public NetClientRequest(String netAddress, String netPath, int netPort, String netData, String netContentType, String[][] netHeaders, boolean secureConnection) {
        this(netAddress, netPath, netPort, netData, netContentType, netHeaders);
        this.secureConnection = secureConnection;
    }
    
    public NetClientRequest(String netAddress, String netPath, int netPort, String netData, String netContentType, String netHeaders[][]) {
        this(netAddress, netPath, netPort, netData, netContentType);
        this.netHeaders = netHeaders;
    }
    
    public NetClientRequest(String netAddress, String netPath, int netPort, String netData, String netContentType) {
        this(netAddress, netPath, netPort);
        this.netData = netData;
        this.netContentType = netContentType;
    }

    public NetClientRequest(String netAddress, String netPath, int netPort) {
        this.netAddress = netAddress;
        this.netPath = netPath;
        this.netPort = netPort;
        netData = null;
        netContentType = null;
        netHeaders = null;
        secureConnection = false;
    }
    
    public String getNetAddress() {
        return netAddress;
    }

    public String getNetData() {
        return netData;
    }

    public String getNetPath() {
        return netPath;
    }

    public int getNetPort() {
        return netPort;
    }

    public String getNetContentType() {
        return netContentType;
    }

    public String[][] getNetHeaders() {
        return netHeaders;
    }

    public boolean isSecureConnection() {
        return secureConnection;
    }
    
}
