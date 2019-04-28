/*
 */

package twitchhsbot.modules;

import java.util.HashMap;
import twitchhsbot.extensions.ChatModule;
import static twitchhsbot.extensions.ChatModule.LOG;
import twitchhsbot.structures.ChatModuleAccess;
import twitchhsbot.wrappers.ChatMessage;

public class SubGreetModule extends ChatModule {
    
    private ChatModuleAccess.ModuleDependencies[] modDeps =
            {ChatModuleAccess.ModuleDependencies.CHAT_RW,
            ChatModuleAccess.ModuleDependencies.PROPERTIES_RW};
    private ChatModuleAccess cma;
    
    public SubGreetModule() {
        moduleName = "SubGreetModule";
        cma = new ChatModuleAccess(moduleName, modDeps, 0);
    }
    
    private final String subscriptionIndicator1 = " just subscribed!";
    private final String subscriptionIndicator2 = " subscribed for ";
    
    
    private final String anniversaryIndicator1 = "resubscribed";
    private final String anniversaryIndicator2 = "row";

    
            
    private final String hostTargetIndicator = "HOSTTARGET";
    private final String hostTargetDisableSelective = "- 0";
    
    private boolean selectiveGreetingEnable = false;
    private String selectiveGreetingChannel = "";

    @Override
    public ChatModuleAccess registerModule() {
        return cma;
    }

    @Override
    public boolean processMessage(ChatMessage message) {
        String messageSender = message.sender;
        String messageText = message.text;
        if(messageSender.equalsIgnoreCase("jtv") && messageText.contains(hostTargetIndicator)) {
            String[] hostTargetStatusArr = messageText.split(hostTargetIndicator + " ");
            if(hostTargetStatusArr[1].contains(hostTargetDisableSelective)) {
                LOG.info("Detected that host mode was exited.");
                selectiveGreetingEnable = false;
                selectiveGreetingChannel = "";
            } else {
                String[] hostTargetChannelArr;
                if(messageText.contains(hostTargetDisableSelective.substring(0, 1))) {
                    hostTargetChannelArr = hostTargetStatusArr[1].split(" -");
                } else {
                    hostTargetChannelArr = hostTargetStatusArr[1].split(" 0");
                }
                String hostTargetChannel = hostTargetChannelArr[0];
                selectiveGreetingEnable = true;
                selectiveGreetingChannel = hostTargetChannel;
                LOG.info("Detected that " + manager.getChannelName() + " is hosting '"+hostTargetChannel+"', enabling selective greeting.");
            }
        } else if(messageSender.equalsIgnoreCase("twitchnotify")) {
            if(messageText.length() > subscriptionIndicator1.length()) {
                if(selectiveGreetingEnable && messageText.contains(selectiveGreetingChannel)) {
                    LOG.info("Detected that subscription notification was triggered for hosted channel '" + selectiveGreetingChannel + "', skipping greeting");
                } else {
                    if(messageText.contains(anniversaryIndicator1)) {
                        LOG.info("Detected that the subscription notification was an 'anniversary notification'. Notification was: '" + messageText + "'.");
                        return true;
                    } else if(messageText.contains(anniversaryIndicator2)) {
                        String[] monthsRowArr = messageText.split(subscriptionIndicator2);
                        if(monthsRowArr.length == 2) {
                            String monthsRow = monthsRowArr[1].substring(0, 2).trim();
                            message.text = monthsRowArr[0] + ", thank you for your " + monthsRow +  " months of continuous support to the Snus Brotherhood" + getEmote(Integer.parseInt(monthsRow));
                            LOG.info("Celebrating anniversary for " + monthsRowArr[0] + ", months count: " + monthsRow);
                        } else {
                            return true;
                        }
                    } else if(messageText.contains(subscriptionIndicator1)){
                        String[] sl = messageText.split(" just");
                        LOG.info("Triggered new subscriber: " + sl[0]);
                        message.text = sl[0] + ", welcome to the Snus Brotherhood! forsenSnus";
                    }
                    manager.queueMessage(this, message, true);
                }
            }
            return true;
        }
        return false;
    }
    
    private String getEmote(int monthsCount) {
        if(monthsCount>=11){
            return "!!!!! forsenWOW forsenWOW forsenWOW forsenSnus";
        } else if(monthsCount>=9) {
            return "!!! PogChamp forsenSnus PogChamp forsenSnus";
        } else if(monthsCount>=6) {
            return "!! PogChamp forsenSnus";
        } else if(monthsCount>=3) {
            return "! ThunBeast forsenSnus";
        } else {
            return " forsenSnus";
        }
    }

    @Override
    public HashMap<String, String> getParameters() {
        return null;
    }

    @Override
    public void setParameters(HashMap<String, String> propertiesMap) {}

}
