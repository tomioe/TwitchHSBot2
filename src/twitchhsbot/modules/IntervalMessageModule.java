/*
 */
package twitchhsbot.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import twitchhsbot.extensions.ChatModule;
import static twitchhsbot.extensions.ChatModule.LOG;
import twitchhsbot.structures.ChatModuleAccess;
import twitchhsbot.structures.ChatModuleAccess.ModuleDependencies;
import twitchhsbot.wrappers.ChatMessage;

public class IntervalMessageModule extends ChatModule {

    private ModuleDependencies[] modDeps = {
        ModuleDependencies.CHAT_RW,
        ModuleDependencies.MOD_CHECK,
        ModuleDependencies.PROPERTIES_RW
    };
    private final String[] triggerWords = {
        "!message"
    };
    private final int messageArrayMax = 5;
    private final int rateInterval = 1000;
    private String paramPre;
    private final String paramMessage = "message";
    private final String paramInterval = "interval";
    private final String paramEnable = "enabled";
    private final String paramChannel = "channel";
    private final String paramIndex = "index";
    private final String paramSeparator = "_";
    private final String[] commandWords = {
        "enable",
        "time",
        "set"
    };
    private ChatModule selfReference = this;
    private int messageInterval = -1;
    // entered in seconds, low = 30 secs
    private final int messageIntervalLow = 30 - 1;
    // high = 30 mins
    private final int messageIntervalHigh = (30 * 60) - 1;
    private Timer messageTimer;
    private String messageChannel = "";
    private ArrayList<IntervalMessageWrapper> messageArray = new ArrayList();
    
    public IntervalMessageModule() {
        moduleName = "IntervalMessage";
        paramPre = moduleName + paramSeparator;
        messageTimer = new Timer();
        nextMessageIndex = 0;
        for(int i = 0; i < messageArrayMax; i++) {
            IntervalMessageWrapper imw = new IntervalMessageWrapper(false, "");
            messageArray.add(i, imw);
        }
    }

    @Override
    public ChatModuleAccess registerModule() {
        return new ChatModuleAccess(moduleName, modDeps, rateInterval, triggerWords);
    }

    @Override
    public boolean processMessage(ChatMessage message) {
        String messageText = message.text;
        String messageSender = message.sender;
        if (messageText.charAt(0) != '!') {
            return false;
        } else if (!messageText.contains(triggerWords[0])) {
            return false;
        }
        if (!manager.isMod(messageSender)) {
            LOG.info("Non-moderator " + messageSender + " tried to interact with the Interval Message module.");
            return true;
        }
        Scanner scn = new Scanner(messageText);
        scn.next();
        String commandWord = scn.next();
        boolean messageIndexValid;
        int messageIndex = -1;
        
        if (commandWord.equals(commandWords[1])) {
            messageIndexValid = true;
        } else {
            try {
                messageIndex = scn.nextInt();
                if (messageIndex != -1 && messageIndex < messageArrayMax) {
                    messageIndexValid = true;
                } else {
                    message.text = "The Interval Message index was not valid!";
                    LOG.info("The specified Interval Message index was not valid (not saved or larger than 5)");
                    messageIndexValid = false;
                }
            } catch (Exception ex) {
                messageIndexValid = false;
                message.text = "Invalid message index specified!";
                LOG.warning(messageSender + " tried to use command '" + commandWord + "' on an invalid index, caught exception: '" + ex.getClass().getName() + "'.");
            }
        }
        
        String optionWord = scn.next();

        int commandIndex = findIndexNumber(commandWords, commandWord);
        if (messageIndexValid) {
            switch (commandIndex) {
                case 0:
                    if (optionWord.equals("on") || optionWord.equals("off")) {
                        boolean newMessageEnable = optionWord.equals("on") ? true : false;
                        ((IntervalMessageWrapper) messageArray.get(messageIndex)).setEnableMessage(newMessageEnable);
                        messageChannel = message.channel;
                        message.text = "Interval Message at index " + messageIndex + " turned " + (newMessageEnable ? "on." : "off.");
                        LOG.info(messageSender + " turned " + (newMessageEnable ? "on" : "off") + " interval message index " + messageIndex + ".");
                    } else {
                        message.text = "Interval Message enabling must be either 'on' or 'off'.";
                        LOG.info(messageSender + " specified an invalid option for Interval Message enabling.");
                    }
                    break;
                case 1:
                    int newMessageTime = -1;
                    try {
                        newMessageTime = Integer.parseInt(optionWord);
                    } catch (Exception ex) {
                        LOG.warning(messageSender + " entered invalid interval message time, caught: " + ex.getClass().getName());
                        message.text = "Invalid time entered for interval message";
                    }
                    if (newMessageTime <= messageIntervalLow || newMessageTime >= messageIntervalHigh) {
                        LOG.warning(messageSender + " entered invalid time interval for interval message.");
                        message.text = "Interval Message time must be entered in seconds, between 30 seconds and 30 minutes";
                    } else {
                        messageInterval = newMessageTime;
                        messageTimer.cancel();
                        messageTimer.purge();
                        messageTimer = new Timer();
                        messageTimer.scheduleAtFixedRate(new CustomTimerTask(), 0, messageInterval * 1000);
                        message.text = "Updated Interval Message time.";
                    }
                    break;
                case 2:
                    String newIntervalMessage = messageText.substring(triggerWords[0].length() + commandWord.length() + 4);
                    if (!newIntervalMessage.isEmpty() && newIntervalMessage.length() > 0) {
                        String messageNew = newIntervalMessage.trim();
                        ((IntervalMessageWrapper) messageArray.get(messageIndex)).setMessageText(messageNew);
                        message.text = "Updated Interval Message text at index " + messageIndex + ".";
                        LOG.info(messageSender + " updated interval message text for index " + messageIndex + " to '" + messageNew + "'.");
                    } else {
                        message.text = "Invalid Interval Message text, please enter a non-empty message.";
                        LOG.info(messageSender + " tried to update the deck, but entered an invalid text.");
                    }
                    break;
                default:
                    LOG.warning(messageSender + " entered invalid Interval Message command");
                    message.text = "Invalid command for interval message, valid commands are 'enable'/'time'/'set'";
            }
        }
        manager.queueMessage(this, message, true);
        return true;
    }

    private int findIndexNumber(String[] listOfWords, String wantedWord) {
        for (int i = 0; i < listOfWords.length; i++) {
            if (listOfWords[i].equals(wantedWord)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public HashMap<String, String> getParameters() {
        HashMap<String, String> loadParams = new HashMap();
        loadParams.put(paramPre+paramChannel, messageChannel);
        loadParams.put(paramPre + paramInterval, messageInterval + "");
        for(int i = 0; i<messageArray.size(); i++) {
            IntervalMessageWrapper currentArrayMessage = messageArray.get(i);
            boolean enableMessage = currentArrayMessage.isEnableMessage();
            String messageText = currentArrayMessage.getMessageText();
            loadParams.put(paramPre +paramIndex +paramSeparator+i+paramSeparator +paramEnable,enableMessage+"");
            loadParams.put(paramPre +paramIndex +paramSeparator+i+paramSeparator +paramMessage,messageText);
        }

        return loadParams;
    }

    @Override
    public void setParameters(HashMap<String, String> propertiesMap) {
        for (String loadKey : propertiesMap.keySet()) {
            if (loadKey.contains(paramInterval)) {
                try {
                    messageInterval = Integer.parseInt(propertiesMap.get(loadKey));
                    LOG.info("Starting interval messages with time " + messageInterval + " seconds.");
                } catch (Exception ex) {
                    LOG.warning("Unable to load 'time interval' property, defaulting to -1.");
                }
            } else if (loadKey.contains(paramChannel)) {
                messageChannel = propertiesMap.get(loadKey);
                if (messageChannel.isEmpty()) {
                    LOG.info("Starting interval messages with empty defauly channel.");
                } else {
                    LOG.info("Start interval messages with channel: '" + messageChannel + "'.");
                }
            } else if(loadKey.contains(paramIndex)) {
                String subStringIndexer = paramPre+paramIndex+paramSeparator;
                int subStringIndexerLength = subStringIndexer.length();
                String paramStringInt = loadKey.substring(subStringIndexerLength, subStringIndexerLength+1);
                try {
                    int savedMessageIndex = Integer.parseInt(paramStringInt);
                    if(loadKey.contains(paramMessage)) {
                        String loadedMessageText = propertiesMap.get(loadKey);
                        ((IntervalMessageWrapper)messageArray.get(savedMessageIndex)).setMessageText(loadedMessageText);
                    } else if(loadKey.contains(paramEnable)) {
                        boolean messageEnabled = Boolean.parseBoolean(propertiesMap.get(loadKey));
                        ((IntervalMessageWrapper)messageArray.get(savedMessageIndex)).setEnableMessage(messageEnabled);
                    }
                } catch (Exception ex) {
                    System.out.println("Caught exception in trying to find message index: '"+ex.getClass().getName()+"'.");
                }
            }
        }
        if (detectEnabledMessages() && !messageChannel.isEmpty()) {
            LOG.info("Starting with Interval Messages enabled.");
            Timer t1 = new Timer();
            t1.schedule(new TimerTask(){
                @Override
                public void run() {
                    LOG.info("Startuing interval messages.");
                    messageTimer.scheduleAtFixedRate(new CustomTimerTask(), 0, messageInterval * 1000);
                }
            }, 20000);
        }
    }

    private class CustomTimerTask extends TimerTask {
        @Override
        public void run() {
            if(detectEnabledMessages()) {
                int validIndex = getNextValidIndex();
                if(validIndex != -1) {
                    //System.out.println("Getting text for message at index " + validIndex);
                    String currentMessageText = messageArray.get(validIndex).getMessageText();
                    if(!currentMessageText.isEmpty()) {
                        //System.out.println("Sending message: '"+currentMessageText+"' (channel " + messageChannel+")");
                        manager.queueMessage(selfReference, new ChatMessage(messageChannel, moduleName, currentMessageText), false);
                    }
                }
            }
        }
    }
    
    private boolean detectEnabledMessages() {
        for(int i = 0; i < messageArray.size(); i++) {
            if(messageArray.get(i).isEnableMessage()) {
                //System.out.println("Found an enabled message at index: " + i);
                return true;
            }
        }
        return false;
    }
    
    
    private int nextMessageIndex;
    
    private int getNextValidIndex() {
        //System.out.println("previous index = " + previousMessageIndex + ", nextmessageindex = " + nextMessageIndex);
        for(int i = nextMessageIndex; i<messageArray.size();i++) {
            if(messageArray.get(i).isEnableMessage()) {
                //System.out.println("Message at index " + i + " is enabled.");
                nextMessageIndex = i+1;
                return i;
            }
        }
        nextMessageIndex = 0;
        //System.out.println("Reset to 0");
        return -1;
    }
    
    private class IntervalMessageWrapper {
        private boolean enableMessage = false;
        private String messageText = "";
        private IntervalMessageWrapper(boolean enableMessage, String messageText) {
            this.enableMessage = enableMessage;
            this.messageText = messageText;
        }
        public boolean isEnableMessage() {
            return enableMessage;
        }
        public String getMessageText() {
            return messageText;
        }
        public void setEnableMessage(boolean enableMessage) {
            this.enableMessage = enableMessage;
        }
        public void setMessageText(String messageText) {
            this.messageText = messageText;
        }
    }
}
