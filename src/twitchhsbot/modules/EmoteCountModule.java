/*
 */
package twitchhsbot.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import twitchhsbot.extensions.ChatModule;
import static twitchhsbot.extensions.ChatModule.LOG;
import twitchhsbot.structures.ChatModuleAccess;
import twitchhsbot.structures.ChatModuleAccess.ModuleDependencies;
import twitchhsbot.wrappers.ChatMessage;

public class EmoteCountModule extends ChatModule {

    private final ChatModuleAccess.ModuleDependencies[] modDeps = {
        ModuleDependencies.CHAT_RW,
        ModuleDependencies.MOD_CHECK
    };
    private final int rateLimit = 3000;
    private final String[] triggerWords = {
        "!kpm",
        "!pjpm",
        "!popm",
        "!bipm",
        "!bapm",
        "!tpm",
        "!fpm",
        "!4pm",
        "!spm",
        "!emoteclear"
    };
    private final String[] triggerEmotes = {
        "Kappa",
        "PJSalt",
        "PogChamp",
        "BibleThump",
        "BabyRage",
        "ThunBeast",
        "FrankerZ",
        "4Head",
        "forsenSnus"
    };
    private final Timer emoteTimer;
    private List<EmoteWrapper> emoteQueue;

    public EmoteCountModule() {
        moduleName = "EmoteCountModule";
        emoteQueue = Collections.synchronizedList(new ArrayList<EmoteWrapper>());
        emoteTimer = new Timer();
        emoteTimer.scheduleAtFixedRate(new EmoteQueueTask(), 0, 500);
    }

    @Override
    public ChatModuleAccess registerModule() {
        return new ChatModuleAccess(moduleName, modDeps, rateLimit, triggerWords);
    }

    @Override
    public boolean processMessage(ChatMessage message) {
        String messageText = message.text;
        boolean triggered = false;
        boolean triggeredByEmote = false;
        if (messageText.charAt(0) != '!') {
            for (int i = 0; i < triggerEmotes.length; i++) {
                if (messageText.contains(triggerEmotes[i])) {
                    triggered = true;
                    triggeredByEmote = true;
                    break;
                }
            }
        } else {
            for (int i = 0; i < triggerWords.length; i++) {
                if (messageText.equals(triggerWords[i])) {
                    triggered = true;
                    break;
                }
            }
        }
        if (triggered) {
            String messageSender = message.sender;
            if (triggeredByEmote) {
                Scanner scn = new Scanner(messageText);
                while (scn.hasNext()) {
                    String nextWord = scn.next();
                    for (int i = 0; i < triggerEmotes.length; i++) {
                        String emoteTrigger = triggerEmotes[i];
                        if (nextWord.equals(emoteTrigger)) {
                            EmoteWrapper emw = new EmoteWrapper(emoteTrigger, new Date());
                            synchronized (emoteQueue) {
                                emoteQueue.add(emw);
                            }
                        }
                    }
                }
            } else {
                for (int i = 0; i < triggerWords.length; i++) {
                    String triggeredCommand = triggerWords[i];
                    if (messageText.equals(triggeredCommand)) {
                        if (i <= 8) {
                            String emoteName = triggerEmotes[i];
                            double emoteCount = getEmoteCountInQueue(emoteName);
                            message.text = "Current " + emoteName + " 's per minute is: " + emoteCount + "0";
                            LOG.info(messageSender + " triggered emote count for '" + emoteName + "' emote, current count: " + emoteCount);
                            manager.queueMessage(this, message, false);
                        } else if (i == 9) {
                            if (manager.isMod(messageSender)) {
                                synchronized (emoteQueue) {
                                    emoteQueue.clear();
                                }
                                message.text = "Cleared emote queue!";
                                manager.queueMessage(this, message, false);
                                LOG.info("Mod " + messageSender + " cleared the emote queue");
                            } else {
                                LOG.warning("Non-mod user " + messageSender + " tried to clear the emote queue.");
                            }
                        } else {
                            LOG.warning("Unknown command index reached, should not happen.");
                        }
                        break;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private int getEmoteCountInQueue(String emoteName) {
        int emoteCount = 0;
        synchronized (emoteQueue) {
            for (EmoteWrapper ewq : emoteQueue) {
                if (ewq.getEmoteName().equals(emoteName)) {
                    emoteCount++;
                }
            }
        }
        return emoteCount;
    }

    @Override
    public HashMap<String, String> getParameters() {
        return null;
    }

    @Override
    public void setParameters(HashMap<String, String> propertiesMap) {}

    private class EmoteWrapper {

        private String emoteName;
        private Date entryTime;

        public EmoteWrapper(String emoteName, Date entryTime) {
            this.emoteName = emoteName;
            this.entryTime = entryTime;
        }

        public String getEmoteName() {
            return emoteName;
        }

        public Date getEntryTime() {
            return entryTime;
        }
    }

    private class EmoteQueueTask extends TimerTask {

        @Override
        public void run() {
            synchronized (emoteQueue) {
                if (emoteQueue.size() > 0) {
                    Iterator it = emoteQueue.iterator();
                    while (it.hasNext()) {
                        EmoteWrapper queuedEmote = (EmoteWrapper) it.next();
                        Date queuedEmoteEntryTime = queuedEmote.getEntryTime();
                        long timeDifference = new Date().getTime() - queuedEmoteEntryTime.getTime();
                        if (timeDifference > 60000) {
                            it.remove();
                        }
                    }
                }
            }
        }
    }
}
