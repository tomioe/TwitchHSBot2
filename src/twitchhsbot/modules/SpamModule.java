/*
 */

package twitchhsbot.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import twitchhsbot.extensions.ChatModule;
import static twitchhsbot.extensions.ChatModule.LOG;
import twitchhsbot.structures.ChatModuleAccess;
import twitchhsbot.wrappers.ChatMessage;

public class SpamModule extends ChatModule {
    
    private final ChatModuleAccess.ModuleDependencies[] modDeps = {ChatModuleAccess.ModuleDependencies.CHAT_RW,
                                            ChatModuleAccess.ModuleDependencies.MOD_CHECK,
                                            ChatModuleAccess.ModuleDependencies.PROPERTIES_RW};
    private ChatModuleAccess cma;
    private final int moduleRateLimit = 500;
    private String paramPre;
    private final String paramPhrase = "saved_";
    private final String paramActive = "active_";
    private final String paramActiveAscii = "ascii_";
    private final String paramActivePhrase = "phrase_";
    
    private String activeChannel;
    
    private final String[] triggerWords = {
      "!ban"
    };
    
    private final String[] commandWords = {
      "ascii",
      "phrase"
    };
    
    private final String[] optionWords = {
      "on",
      "off",
      "clear"
    };
    
    // following array contains on/off status of the sub-modules.
    // true = on, false = off.
    private boolean[] commandStatus = {
        false,
        false,
    };
    
    private ArrayList<String> bannedPhrases = new ArrayList();
    private ArrayList<String> bannedAscii = new ArrayList();
    
   
    private final int spamLength = 380;
    private String asciiString = new String();
    
    private final String[] asciiCharacters = {
      "▄","■","▀","█","▌","▒","░","▋","╲","━","▔","┈","ͯ","ͩ","͏","ͭ҉","ف",
      "ͤ","҉","҈","ے","ڪ","▉","┳","┳","┃","็","╰","┻","◥","͓","͚","͎","̰","̼",
      "̟","̥","̳","̯","̣","͉"
    };
    
    
    public SpamModule() {
        moduleName = "SpamModule";
        paramPre = moduleName + "_";
        cma = new ChatModuleAccess(moduleName, modDeps, moduleRateLimit, triggerWords);
        activeChannel = manager.getChannelName();
        bannedAscii.addAll(Arrays.asList(asciiCharacters));
        char c = '\u2580';
        for(int i = 0; i<32; i++) {
            bannedAscii.add(c+"");
            c++;
        }
        c = '\u2500';
        for(int i = 0; i<128; i++) {
            bannedAscii.add(c+"");
            c++;
        }
        c = '\u8199';
        bannedAscii.add(c+"");
    }

    @Override
    public ChatModuleAccess registerModule() {
        return cma;
    }

    @Override
    public boolean processMessage(ChatMessage message) {

        String messageText = message.text;
        String messageSender = message.sender;

        if(manager.isMod(messageSender) && isFirstWordCommand(messageText, triggerWords[0])) {
            Scanner scn = new Scanner(messageText);
            scn.next();
            String commandWord = scn.next();
            String optionWord = scn.next();
            int commandIndex = checkOptionIndex(commandWords, commandWord);
            int optionIndex = checkOptionIndex(optionWords, optionWord);
            if (commandIndex != -1) {
                if(optionIndex >= 0 && optionIndex <= 1) {
                    commandStatus[commandIndex] = (optionIndex == 0) ? true : false;
                    message.text = commandWords[commandIndex].toUpperCase() + " bans turned " + (commandStatus[commandIndex] ? "on." : "off.");
                    LOG.info("Moderator "+messageSender+" set '" + commandWords[commandIndex] + "' bans to " + (commandStatus[commandIndex] ? "on." : "off."));
                } else if(commandIndex == 1) {
                    if(optionIndex == -1) {
                        String phraseToBeAdded = messageText.substring(triggerWords[0].length() + commandWord.length() + 2).toLowerCase();
                        if(!bannedPhrases.contains(phraseToBeAdded)) {
                            bannedPhrases.add(phraseToBeAdded);
                            LOG.info("Moderator " + messageSender + " added '"+phraseToBeAdded+"' to the banned phrases list.");
                            message.text = "Ban phrase successfully added!";
                            manager.saveProperties();
                        } else {bannedPhrases.add(phraseToBeAdded);
                            LOG.info("Moderator " + messageSender + " tried adding '"+phraseToBeAdded+"' to the banned phrases list, but it was already present.");
                            message.text = "Ban phrase already present!";
                        }                       
                    } else if(optionIndex == 2) {
                        LOG.info("Moderator " + messageSender + " cleared the banned phrases list.");
                        bannedPhrases.clear();
                        message.text = "Banned phrases cleared!";
                    } else {
                        LOG.warning(messageSender + " triggered an invalid state in module, Command Index: " + commandIndex + ", Option Index: " + optionIndex);
                        message.text = "Internal error in spam module.";
                    }
                } else {
                    LOG.warning(messageSender + " entered an invalid option for the command, Command Index: " + commandIndex + ", Option Index: " + optionIndex);
                    message.text = "Invalid option specified for the entered command.";
                }
            } else {
                LOG.warning("Invalid command given to spam moudle by '" + messageSender +  "'.");
                message.text = "Invalid command specified. Valid commands are: 'ascii'/'phrase'.";
                
            }
            manager.queueMessage(this, message, true);
            return true;
        } else {
            boolean sendMessage = false;
//            // add variable timeout time for ascii/phrase/length
//            if( (commandStatus[0] && scanForAscii(messageText)) ||
//                (commandStatus[1] && scanForPhrase(messageText))) {
//            
//            } else if((messageText.length() > spamLength)){
//                
//            }
            if(commandStatus[0] && scanForAscii(messageText)) {
                LOG.info("Found ASCII spam in message from " + messageSender);
                message.text = "/timeout " + messageSender + " 400";
                sendMessage = true;
            }
            if(commandStatus[1] && scanForPhrase(messageText.toLowerCase())) {
                LOG.info("Found banned phrase in message from " + messageSender);
                message.text = "/timeout " + messageSender + " 400";
                sendMessage = true;
            }
            if(messageText.length()>spamLength) {
                LOG.info("Found too long phrase phrase in message from " + messageSender);
                message.text = "/timeout " + messageSender + " 100";
                sendMessage = true;
            }
            if(sendMessage) {
                manager.queueMessage(this, message, true, true);
                return true;
            }
        }
        
        
        return false;
    }
    
    private boolean scanForPhrase(String phraseText) {
        for(String bannedPhrase : bannedPhrases) {
            if(phraseText.contains(bannedPhrase)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean scanForAscii(String phraseText) {
        int asciiCount = 0;
        for(int i = 0; i<phraseText.length(); i++) {
            String currentPhraseCharacter = phraseText.charAt(i) + "";
            if(bannedAscii.contains(currentPhraseCharacter)) {
                asciiCount++;
            }
        }
//        for(String asciiCharacter : bannedAscii) {
//            if(phraseText.contains(asciiCharacter)) {
//                asciiCount++;
//            }
//        }
        //System.out.println("Ascii count: " + asciiCount);
        if(asciiCount>20) {
            return true;
        } else {
            return false;
        }
    }
    
    private boolean isFirstWordCommand(String fullMessage, String commandToCheckFor) {
        if(fullMessage.length() < commandToCheckFor.length()) {
            return false;
        } else {
            return fullMessage.substring(0, commandToCheckFor.length()).equals(commandToCheckFor);
        }
    }
    
    private int checkOptionIndex(String[] optionArray, String requestedOption) {
        for (int i = 0; i < optionArray.length; i++) {
            if (optionArray[i].equals(requestedOption)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public HashMap<String, String> getParameters() {
        HashMap<String, String> newParams = new HashMap();
        ArrayList<Integer> usedRandInts = new ArrayList();
        
        Random rnd = new Random();
        boolean generating = true;
        Integer randomInt = rnd.nextInt(4000);
        String paramFinal;
        
        for(String bannedPhrase : bannedPhrases) {
            while(generating) {
                if(!usedRandInts.contains(randomInt)) {
                    usedRandInts.add(randomInt);
                    generating = false;
                }
                randomInt = rnd.nextInt(4000);
            }
            
            paramFinal = paramPre + paramPhrase + randomInt;
            newParams.put(paramFinal, bannedPhrase);
            generating = true;
        }
        paramFinal = paramPre + paramActive + paramActiveAscii;
        newParams.put(paramFinal, commandStatus[0]+"");
        paramFinal = paramPre + paramActive + paramActivePhrase;
        newParams.put(paramFinal, commandStatus[1]+"");
        return newParams;
    }

    @Override
    public void setParameters(HashMap<String, String> propertiesMap) {
        for(String keyValue : propertiesMap.keySet()) {
            if(keyValue.contains(paramPhrase)) {
                bannedPhrases.add(propertiesMap.get(keyValue));
                LOG.info("Load ban phrase: '" +propertiesMap.get(keyValue)+"'");
            } else if(keyValue.contains(paramActiveAscii)) {
                commandStatus[0] = Boolean.parseBoolean(propertiesMap.get(keyValue));
            } else if(keyValue.contains(paramActivePhrase)) {
                commandStatus[1] = Boolean.parseBoolean(propertiesMap.get(keyValue));
            }
        }
    }

}
