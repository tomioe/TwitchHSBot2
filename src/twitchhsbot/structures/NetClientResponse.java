/*
 */

package twitchhsbot.structures;

public class NetClientResponse {
    
    // netResponse is true if NetClient couldnt connect to server, or timeout happened
    private boolean netResponse = false;
    private int netResponseStatusCode;
    private String netResponseData;
    private NetClientRequest netRequest;

    public NetClientResponse(boolean netResponse, int netResponseStatusCode, String netResponseData, NetClientRequest netRequest) {
        this.netResponse = netResponse;
        this.netResponseStatusCode = netResponseStatusCode;
        this.netResponseData = netResponseData;
        this.netRequest = netRequest;
    }

    public boolean isNetResponse() {
        return netResponse;
    }

    public int getNetResponseStatusCode() {
        return netResponseStatusCode;
    }

    public String getNetResponseData() {
        return netResponseData;
    }

    public NetClientRequest getNetRequest() {
        return netRequest;
    }
    
    
    
}
