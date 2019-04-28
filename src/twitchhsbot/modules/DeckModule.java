/*
 */

package twitchhsbot.modules;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import org.apache.commons.validator.routines.UrlValidator;
import org.ocpsoft.prettytime.PrettyTime;
import twitchhsbot.extensions.ChatModule;
import static twitchhsbot.extensions.ChatModule.LOG;
import twitchhsbot.structures.ChatModuleAccess;
import twitchhsbot.structures.ChatModuleAccess.ModuleDependencies;
import twitchhsbot.wrappers.ChatMessage;

public class DeckModule extends ChatModule {
    
    private ModuleDependencies[] modDeps = {ChatModuleAccess.ModuleDependencies.CHAT_RW,
                                            ChatModuleAccess.ModuleDependencies.MOD_CHECK,
                                            ChatModuleAccess.ModuleDependencies.PROPERTIES_RW};
    private ChatModuleAccess cma;
    private String paramPre;
    
    private String[] triggerWords = {
      "!setdeck",
      "!decks",
      "!decklist",
      "!deck",
      "!build"
    };
    
    private final String[] sch = {"http", "https"};
    private final UrlValidator urlValidator = new UrlValidator(sch);
    private final PrettyTime pt = new PrettyTime();
    private final SimpleDateFormat parseFormat = new SimpleDateFormat("MMMM d yyyy, HH:mm:ss", Locale.ENGLISH);
    private final int rateLimit = 3000;
    private final String deckHistoryUrl = "http://bit.ly/122iWDv";
    
    private Date setDeckDate;
    private String setDeckUrl;
    
    
    public DeckModule() {
        moduleName = "DeckModule";
        paramPre = moduleName + "_";
        cma  = new ChatModuleAccess(moduleName,  modDeps, rateLimit, triggerWords);
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
                if(messageText.equals(triggerWords[i])) {
                    triggered = true;
                    break;
                }
            }
            if(!triggered) {
                return false;
            }
        }
        String messageSender = message.sender;
        if(messageText.contains(triggerWords[0])) {
            if(manager.isMod(messageSender)) {
                // +1 in the substring means we also dont take the extra space in the url
                String updateUrl = messageText.substring(triggerWords[0].length()+1).trim();
                if (urlValidator.isValid(updateUrl)) {
                    LOG.info(messageSender + " updated deck to " + updateUrl);
                    setDeckDate = new Date();
                    setDeckUrl = updateUrl;
                    message.text = "Updated deck to " + updateUrl + "!";
                } else {
                    LOG.info(messageSender + " tried to update deck, invalid URL entered.");
                    message.text = "Invalid Deck URL specified.";
                }
                manager.queueMessage(this, message, true);
                return true;
            } else {
                LOG.info("Non-mod user " + messageSender + " tried to update deck.");
                return false;
            }
        } else if(messageText.equals(triggerWords[1])){  
            LOG.info(messageSender + " requested deck history, sending.");
            message.text = "A list of Forsen's decks are available here: " + deckHistoryUrl;
        } else {
            if(setDeckUrl != null && !setDeckUrl.isEmpty()) {
                LOG.info(messageSender +" requested current deck, sending.");
                message.text = "Current deck is: " + setDeckUrl + " (updated "+pt.format(setDeckDate)+")";
            } else {
                LOG.info(messageSender +" requested current deck, no deck was set.");
                message.text = "No deck has been set. Use '!setdeck {url}' to set a new deck.";
            }
        }
        manager.queueMessage(this, message, false);
        return true;
    }

    @Override
    public HashMap<String, String> getParameters() {
        HashMap<String, String> deckParams = new HashMap();
        if(setDeckDate != null) {
            deckParams.put(paramPre+"set_date", parseFormat.format(setDeckDate));
        }
        if(setDeckUrl != null && !setDeckUrl.isEmpty()){
            deckParams.put(paramPre+"set_url", setDeckUrl);
        }
        return deckParams;
    }

    @Override
    public void setParameters(HashMap<String, String> propertiesMap) {
        for(String key : propertiesMap.keySet()) {
            if (key.contains(paramPre)) {
                if (key.contains("set_date")) {
                    try {
                        String dateString = propertiesMap.get(key);
                        if (dateString != null && !dateString.isEmpty()) {
                            setDeckDate = parseFormat.parse(dateString);
                            LOG.info("Loaded stored parse date: " + setDeckDate.toString());
                        } else {
                            LOG.warning("Invalid stored date.");
                        }
                    } catch (Exception ex) {
                        LOG.warning("Unable to parse the stored set date, exception caught: " + ex.getClass().getName());
                    }
                } else if (key.contains("set_url")) {
                    String urlStringProp = propertiesMap.get(key);
                    if (urlStringProp != null && !urlStringProp.isEmpty()) {
                        setDeckUrl = urlStringProp;
                        LOG.info("Loaded Deck URL: " + setDeckUrl);
                    } else {
                        LOG.warning("Invalid stored Deck URL.");
                    }
                }
            }
        }
    }

}
