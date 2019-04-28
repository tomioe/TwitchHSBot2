/*
 */

package twitchhsbot.modules;

import java.util.HashMap;
import twitchhsbot.extensions.ChatModule;
import static twitchhsbot.extensions.ChatModule.LOG;
import twitchhsbot.extensions.NetClientAccess;
import twitchhsbot.structures.ChatModuleAccess;
import twitchhsbot.structures.ChatModuleAccess.ModuleDependencies;
import twitchhsbot.structures.NetClientRequest;
import twitchhsbot.structures.NetClientResponse;
import twitchhsbot.wrappers.ChatMessage;

public class UptimeModule extends ChatModule implements NetClientAccess {
    
    private final ModuleDependencies[] modDeps = {ModuleDependencies.CHAT_RW,
                                            ModuleDependencies.NET_ACCESS};
    private final int modRateLimit = 4000;
    
    private final String[] triggerWords = {
        "!uptime"
    };
    
    private final String failureMessage = "Failed to retrieve channel uptime.";
    
    private final String netAddress = "localhost";
    private final int netPort = 3010;
    private String netPath = "/?channel=";
    private NetClientRequest ncr;
    
    private boolean requestMade = false;
    private String requestChannel = "";
    
    public UptimeModule() {
        moduleName = "UptimeModule";
        netPath += manager.getChannelName();
        requestChannel = "#"+manager.getChannelName();
        ncr = new NetClientRequest(netAddress, netPath, netPort);
        LOG.info("Updated NetClientRequest path to '" + netPath+"'.");
    }

    @Override
    public ChatModuleAccess registerModule() {
        return new ChatModuleAccess(moduleName, modDeps, modRateLimit, triggerWords);
    }

    @Override
    public boolean processMessage(ChatMessage message) {
        String messageText = message.text;
        if(messageText.charAt(0) != '!') {
            return false;
        } else if(!messageText.equals(triggerWords[0])) {
            return false;
        }
        manager.getRequest(this, ncr);
        LOG.info(message.sender + " requested uptime, querying.");
        return true;
    }

    @Override
    public HashMap<String, String> getParameters() {
    return null;
    }

    @Override
    public void setParameters(HashMap<String, String> propertiesMap) {}

    @Override
    public void netResponse(NetClientResponse ncresp) {
        ChatMessage chm;
        String bodyResponse = ncresp.getNetResponseData();
        if(!ncresp.isNetResponse() || ncresp.getNetResponseStatusCode() != 200 || bodyResponse.isEmpty()) {
            LOG.warning("Unable to retrieve uptime.");
            chm = new ChatMessage(requestChannel, moduleName, failureMessage);
        } else {
            
            LOG.info("Sent uptime to channel, '" + bodyResponse + "'");
            chm = new ChatMessage(requestChannel, moduleName, bodyResponse);
        }
        manager.queueMessage(this, chm, false);
    }

}
