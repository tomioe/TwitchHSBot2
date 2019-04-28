/*
 */

package twitchhsbot;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import org.pircbotx.Configuration;
import org.pircbotx.Configuration.ServerEntry;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.MessageEvent;
import twitchhsbot.wrappers.ChatMessage;

public class IRCClient {
    /* STATIC APPLICATION FIELDS */
    ModuleManager modman = ModuleManager.get();
    public static final Logger LOG = TwitchHSBot.LOG;
    
    /* TIMER APPLICATION FIELDS */
    private Timer rateLimitTimer;
    
    /* FINAL IRC FIELDS */
    public final String defaultAuthUsername = "snusbot";
    private final String ircAddress = "irc.twitch.tv";
    private final int ircPort = 6667;
    private final String defaultAuthPassword = "oauth:z2";
    private final long timeRateLimit = 30500;
    private final int rateLimitTimerPeriod = 100;
    private final int reconnectAttempts = 5;
    
     /* VARIABLE IRC FIELDS */
    private String channelName;
    private List<MessageQueueObject> messageQueue;
    private int messageRateLimit = 20; // total rate limit for non-modded user
    private PircBotX bot;
    
    public IRCClient() {
        rateLimitTimer = new Timer();
        messageQueue = Collections.synchronizedList(new ArrayList<MessageQueueObject>());
        rateLimitTimer.scheduleAtFixedRate(new RateLimitTask(), 0, rateLimitTimerPeriod);
    }
    
    public void connectChannel(HashMap<String, String> configMap, String connChan) {
        this.channelName = "#"+connChan;
        modman = ModuleManager.get();
        ServerEntry se = new ServerEntry(ircAddress, ircPort);
        ArrayList serverList = new ArrayList();
        serverList.add(se);
        String userName = configMap == null ? defaultAuthUsername : configMap.get("irc_user");
        LOG.info("Connecting with IRC username '" + userName + "' " + (configMap == null ? "(default)" : "(non-default)")+ ".");

        Configuration configuration = new Configuration.Builder()
                .setServers(serverList)
            .setName(configMap == null ? defaultAuthUsername : configMap.get("irc_user")) //Set the nick of the bot. CHANGE IN YOUR CODE
            .setLogin("LQ") //login part of hostmask, eg name:login@host
            .setCapEnabled(false) //Enable CAP features
            .addListener(new BotListener()) 
            .setServerPassword(configMap == null ? defaultAuthPassword : configMap.get("irc_pw"))
            .addAutoJoinChannel(this.channelName) 
            .setAutoReconnect(true)
            .setAutoReconnectAttempts(reconnectAttempts)
            .buildConfiguration();
        bot = new PircBotX(configuration);
        try {
            bot.startBot();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
    
    public synchronized void sendSyncMessage(ChatMessage cm) {
        messageQueue.add(new MessageQueueObject(cm, new Date(), false));
    }
    
    public void setModRateLimit(boolean setAsMod) {
        // enables the irc client to send messages as either mod or user
        messageRateLimit = setAsMod ? 99 : 19;
        LOG.info("Setting rate limit to send as '" + (setAsMod ? "mod'" : "user'"));
    }
    
    private class RateLimitTask extends TimerTask {
        @Override
        public void run() {
            synchronized(messageQueue) {
                Date now = new Date();
                // first we need to remove all old messages, before we send any more.
                // the resulting messageQueue contains all sent messages within the 
                // past 30.5 seconds. This means that the size of the queue after 
                // this cleanup are part of the "rate limit".
                for(Iterator<MessageQueueObject> it =  messageQueue.iterator(); it.hasNext();) {
                    MessageQueueObject mqc = it.next();
                    long timeDiff = now.getTime() - mqc.getEnteredTimestamp().getTime();
                    // check if the queued message has been sent and is older than 30.5 seconds
                    if(mqc.isSent() && timeDiff > timeRateLimit) {
                        // in that case, remove it
                        it.remove();
                        // LOG.info("Removing message from rate limit queue.");
                    }
                    // maybe we're dealing with a message that was supposed to be sent 
                    // some time ago? if unsent and older than 40 seconds, just 
                    // delete it then (when does this ever happen????)
                }
                // now that the queue has been cleaned, lets see if we can send more
                // messages or if we should hold back, waiting for the queue size to be reduced
                for(MessageQueueObject mqc : messageQueue){
                    if(!mqc.isSent() && messageQueue.size()<messageRateLimit) {
                        ChatMessage messageToSend = mqc.getMessage();
                        bot.sendIRC().message(messageToSend.channel, messageToSend.text);
                        mqc.setSent(true);
                        mqc.setSentTimestap(now);
                    }
                }
            }
        }
    }
    
    private boolean modListReceived = false;
    
    private void getModList() {
        Timer modTimer = new Timer();
        modTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(!modListReceived) {
                    System.out.println("Checking for mod!");
                    bot.sendIRC().message(channelName, ".mods");
                } else {
                    cancel();
                }
            }
        }, 0, 500);
    }
    
    public void setModList(boolean received) {
        modListReceived = received;
    }
    
    
    private class BotListener extends ListenerAdapter {
        @Override
        public void onConnect(ConnectEvent event) throws Exception {
            LOG.info("Connected to IRC Server.");
            event.getBot().sendRaw().rawLineNow("TWITCHCLIENT 3");
            getModList();
        }
        
        @Override
        public void onMessage(MessageEvent event) throws Exception {
            processMessage(event.getUser().getNick(),event.getMessage());
            
        }

        @Override
        public void onAction(ActionEvent event) throws Exception {
            processMessage(event.getUser().getNick(),event.getMessage());
        }
        
        @Override
        public void onDisconnect(DisconnectEvent event) throws Exception {
            LOG.warning("Disconnected from server.");
        }
        
        private void processMessage(String sender, String message) {
            modman.processMessage(new ChatMessage(channelName, sender, message));
        }
    }
    
    private class MessageQueueObject {

        private ChatMessage cm;
        private Date enteredTimestamp;
        private Date sentTimestap;
        private boolean sent;

        public MessageQueueObject(ChatMessage cm, Date entryTime, boolean sent) {
            this.cm = cm;
            this.enteredTimestamp = entryTime;
            this.sent = sent;
        }

        public ChatMessage getMessage() {
            return cm;
        }

        public Date getEnteredTimestamp() {
            return enteredTimestamp;
        }

        public Date getSentTimestap() {
            return sentTimestap;
        }

        public void setSentTimestap(Date sentTimestap) {
            this.sentTimestap = sentTimestap;
        }

        public boolean isSent() {
            return sent;
        }

        public void setSent(boolean sent) {
            this.sent = sent;
        }
    }

}
