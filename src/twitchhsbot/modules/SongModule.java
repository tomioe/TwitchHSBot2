/*
 * This is the module for "!song", "!nowplaying", etc.
 */

package twitchhsbot.modules;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import twitchhsbot.extensions.ChatModule;
import static twitchhsbot.extensions.ChatModule.LOG;
import twitchhsbot.extensions.NetClientAccess;
import twitchhsbot.structures.ChatModuleAccess;
import twitchhsbot.structures.ChatModuleAccess.ModuleDependencies;
import twitchhsbot.structures.NetClientRequest;
import twitchhsbot.structures.NetClientResponse;
import twitchhsbot.wrappers.ChatMessage;

public class SongModule extends ChatModule implements NetClientAccess {
    
    private final ModuleDependencies[] modDeps = {ChatModuleAccess.ModuleDependencies.CHAT_RW,
                                            ChatModuleAccess.ModuleDependencies.NET_ACCESS};
    private final int modRateLimit = 4000;
    private ChatModuleAccess cma;
    
    
    private String[] triggerWords = {
        "!song",
        "!playing",
        "!nowplaying",
        "!currentsong"
    };
    
    private final String plugUrl = "localhost";
    private final String plugPath = "/request-title";
    private final int plugPort = 420;
    private final NetClientRequest ncr = new NetClientRequest(plugUrl, plugPath, plugPort);
    private final String failureMessage = "Unable to retrieve plug.dj song information.";
    
    private String requestChannel = "";
    private boolean requestMade = false;
    
    private final Timer requestTimer = new Timer();
    
    
    
    public SongModule() {
        moduleName = "SongModule";
        cma = new ChatModuleAccess(moduleName, modDeps, modRateLimit, triggerWords);
    }
    

    @Override
    public ChatModuleAccess registerModule() {
        return cma;
    }

    @Override
    public boolean processMessage(ChatMessage message) {
        String messageText = message.text;
        if(messageText.charAt(0) != '!') {
            return false;
        } else {
            boolean triggered = false;
            for(int i=0; i<triggerWords.length; i++) {
                if(messageText.contains(triggerWords[i])) {
                    triggered = true;
                    break;
                }
            }
            if(!triggered) {
                return false;
            }
        }
        requestChannel = message.channel;
        LOG.info("Plug.dj song request from " + message.sender + ", making net request.");
        manager.getRequest(this, ncr);
        return requestMade;
    }

    @Override
    public HashMap<String, String> getParameters() {
        return null;
    }

    @Override
    public void setParameters(HashMap<String, String> propertiesMap) {}
    

    /* Method is called when NetClient responds to the request */
    @Override
    public void netResponse(NetClientResponse ncr) {
        //if(requestMade) {
            String requestBody = ncr.getNetResponseData();
            int requestCode = ncr.getNetResponseStatusCode();
            ChatMessage chm;
            if(requestCode != 200 || requestBody.isEmpty() || !ncr.isNetResponse()) {
                LOG.warning("Unable to send now playing information, response status code: " + requestCode + ", response body: " + requestBody);
                chm = new ChatMessage(requestChannel, moduleName, failureMessage);
            } else {
                String currentSong = ncr.getNetResponseData();
                LOG.info("Received response '"+currentSong+"', queueing 'Now Playing' message.");
                String messageText = "Currently playing in plug.dj/forsen: "+ currentSong;
                chm = new ChatMessage(requestChannel, moduleName, messageText);
            }
            manager.queueMessage(this, chm, false);
       //     requestMade = false;
        //}
    }
    
    private class RequestResetTask extends TimerTask {
        @Override
        public void run() {
            if(requestMade) {
                requestMade = false;
            }
        }
    }

}
