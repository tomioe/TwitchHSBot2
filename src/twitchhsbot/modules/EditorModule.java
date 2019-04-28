/*
 */

package twitchhsbot.modules;

import java.io.BufferedReader;
import org.json.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import twitchhsbot.extensions.ChatModule;
import twitchhsbot.extensions.NetClientAccess;
import twitchhsbot.structures.ChatModuleAccess;
import twitchhsbot.structures.NetClientRequest;
import twitchhsbot.structures.NetClientRequest.RequestType;
import twitchhsbot.structures.NetClientResponse;
import twitchhsbot.wrappers.ChatMessage;

public class EditorModule extends ChatModule implements NetClientAccess {
    
    private final ChatModuleAccess.ModuleDependencies[] modDeps = {ChatModuleAccess.ModuleDependencies.CHAT_RW};
    private final int moduleRateLimit = 100;
    
    private final String[] triggerWords = {
        "!title",
        "!game"
    };
    
    
    
    private final String netAddress = "api.twitch.tv";
    private String netPath = "/kraken/channels/";
    private final int netPort = 443;
    private final String netContentType = "application/x-www-form-urlencoded";
    //private final String netContentType = "application/json";
    private String netData;
    private String[][] netHeaders = {
        {"Accept", "application/vnd.twitchtv.v2+json"},
        {"Authorization", "OAuth "},
        {"Content-Length", ""},
    };
    private NetClientRequest ncr;
    
    
    
    private final String filePath = "../api_manager/";
    private final String fileAppend = ".auth";
    private final String jsonUsername = "username";
    private final String jsonToken = "token";
    
    private final String dataChannelString = "channel";
    private final String dataStatusString = "status";
    private final String dataGameString = "game";
    
    private final String botOwner = "xx_k";
    private final String botSponsor = "forsenlol";
    
    private String channelOwner;
    
    private boolean authorized = false;
    
    private boolean updateTitle = true;

    public EditorModule() {
        moduleName = "EditorModule";
        channelOwner = manager.getChannelName();
        File f = new File(filePath+channelOwner+fileAppend);
        if(f.exists()) {
            try {
                FileInputStream fsi = new FileInputStream(f);
                InputStreamReader isr = new InputStreamReader(fsi);
                BufferedReader br = new BufferedReader(isr);
                String jsonAuth = br.readLine();
                JSONObject jso = new JSONObject(jsonAuth);
                String authUsername = jso.getString(jsonUsername);
                if(!authUsername.equals(channelOwner)) {
                    LOG.severe("Authorized channel does not match the channel name!");
                } else {
                    LOG.info("User " + authUsername + " has authorized channel editing, enabling module!");
                    authorized = true;
                    netHeaders[1][1] += jso.getString(jsonToken);
                    netPath += channelOwner;
                }
            } catch(Exception ex) {
                LOG.warning("Unable to load the authorization file for " + channelOwner + ", caught exception: " + ex.getClass().getName());
            }
        } else {
            LOG.warning(channelOwner + " has not authorized channel editing yet.");
        }
    }
    
    @Override
    public ChatModuleAccess registerModule() {
        if(authorized) {
            return new ChatModuleAccess(moduleName, modDeps, moduleRateLimit, triggerWords);
        } else {
            return null;
        }
    }

    @Override
    public boolean processMessage(ChatMessage message) {
        if((message.text.length() > 0 && message.text.charAt(0) != '!') || !authorized) {
            return false;
        } else {
            boolean triggered = false;
            String messageText = message.text;
            int i;
            for(i = 0; i<triggerWords.length; i++) {
                if(messageText.contains(triggerWords[i])) {
                    triggered = true;
                    break;
                }
            }
            if(triggered && (message.sender.equals(channelOwner) || message.sender.equals(botOwner) || message.sender.equals(botSponsor))) {
                String messageSender = message.sender;
                String[] updateTextArray = messageText.split(triggerWords[i]+" ");
                switch(i) {
                    // title update
                    case 0:
                        if(updateTextArray.length>1) {
                            String extractedTitle = updateTextArray[1].trim();
                            if(extractedTitle.isEmpty()) {
                                message.text = "The provided title was empty, cannot set empty title.";
                                LOG.warning(messageSender + " entered a title that was empty.");
                            } else {
                                System.out.println("extracted title: " + extractedTitle);
                                LOG.info(messageSender + " initiated new title update to '" + extractedTitle+"', making API request.");
                                message.text = "";
                                String dataString = buildDataString(dataStatusString, extractedTitle);
                                makeRequest(dataString);
                                updateTitle = true;
                            }
                        } else {
                            LOG.info(messageSender + " tried to update title, but nothing was written");
                            message.text = "Please enter something to set the title as!";
                        }
                        break; 
                    // game update
                    case 1:
                        if(updateTextArray.length>1) {
                            String extractedGame = updateTextArray[1].trim();
                            if(extractedGame.isEmpty()) {
                                LOG.warning(messageSender + " requested to update the Game, but entered text was empty.");
                                message.text = "The entered game cannot be empty!";
                            } else {
                                LOG.info(messageSender +" initiated new game update to '"+extractedGame+"', making API request.");
                                message.text = "";
                                String dataString = buildDataString(dataGameString, extractedGame);
                                makeRequest(dataString);
                                updateTitle = false;
                            }
                        } else {
                            LOG.warning(messageSender + " requested to update the Game, but nothing was entered.");
                            message.text = "Please enter a game to update to!";
                        }
                        break;
                    default:
                        message.text = "Invalid Editor command!";
                        LOG.info(messageSender + " invoked an invalid command.");
                        break;
                }
                if(!message.text.isEmpty()) {
                    manager.queueMessage(this, message, false);
                }
                return true;
            }
        }
        return false;
    }
    
    private void makeRequest(String data) {
        netData = data;
        netHeaders[2][1] = netData.length()+"";
        //System.out.print("posting to '" +netAddress+":"+netPort+netPath+"',\n");
        //System.out.println(data);
        ncr = new NetClientRequest(netAddress, netPath, netPort, netData, netContentType, netHeaders, true);
        manager.postRequest(this, ncr, RequestType.PUT_REQUEST);
        netData = "";
    }
    
    private String buildDataString(String type, String data) {
        if(type.isEmpty() || data.isEmpty()) {
            return "";
        }
        try {
            //System.out.println("Ready Data String before: '" + data+"'.");
            data = URLEncoder.encode(data,"UTF-8");
            //System.out.println("Ready Data String after: '" + data+"'.");
            String dataString = dataChannelString+"["+type+"]"+"="+data;
            return dataString.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
//        JSONObject obj = new JSONObject();
//        HashMap<String, String> map = new HashMap();
//        map.put(type, data);
//        obj.put("channel", map);
//        return obj.toString();
    }

    @Override
    public HashMap<String, String> getParameters() {
        return null;
    }

    @Override
    public void setParameters(HashMap<String, String> propertiesMap) {}

    @Override
    public void netResponse(NetClientResponse ncresp) {
        int statusCode = ncresp.getNetResponseStatusCode();
        String responseData = ncresp.getNetResponseData();
        String messageText;
        String updatedInfo = updateTitle ? "title" : "game";
        if(!ncresp.isNetResponse() || statusCode != 200 || responseData.isEmpty()) {
            LOG.warning("API provided invalid response when responding to " + updatedInfo + " update, status code: " + statusCode + ", received response:\n"+responseData);
            messageText = "Unable to update " + updatedInfo +  ", error in contacting Twitch!";
        } else {
            LOG.info("Title was succesfully updated!");
            JSONObject responseJSON = new JSONObject(responseData);
            String responseString = updateTitle ? dataStatusString : dataGameString;
            messageText = "The " + updatedInfo +  " was succesfully updated to: '"+responseJSON.getString(responseString) + "'!";
        }
        manager.queueMessage(this, new ChatMessage("#"+channelOwner, moduleName, messageText), true);
    }
    
    

}
