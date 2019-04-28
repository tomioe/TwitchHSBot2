/*
 */

package twitchhsbot.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import twitchhsbot.extensions.ChatModule;
import static twitchhsbot.extensions.ChatModule.LOG;
import twitchhsbot.structures.ChatModuleAccess;
import twitchhsbot.structures.ChatModuleAccess.ModuleDependencies;
import twitchhsbot.wrappers.ChatMessage;

public class RaffleModule extends ChatModule {
    
     private final ModuleDependencies[] modDeps = {ChatModuleAccess.ModuleDependencies.CHAT_RW,
                                            ChatModuleAccess.ModuleDependencies.MOD_CHECK};
     private final int rateLimit = 300;
     
     private final String[] triggerWords = {
       "!draw"
     };
     
     private final String[] optionWords = {
       "start",
       "stop",
       "pick",
       "repeat",
       "clear"
     };
     
     public RaffleModule() {
         moduleName = "RaffleModule";
     }
     
     
     private final Random rng = new Random();
     
     private boolean onGoingRaffle = false;
     private String raffleWord;
     private ArrayList<String> entreeNames = new ArrayList();
     private ArrayList<String> drawnNames = new ArrayList();
     private ArrayList<String> previousWinners = new ArrayList();
     


    @Override
    public ChatModuleAccess registerModule() {
        return new ChatModuleAccess(moduleName, modDeps, rateLimit, triggerWords);
    }

    @Override
    public boolean processMessage(ChatMessage message) {
        String messageText = message.text;
        if(onGoingRaffle) {
            String messageSender = message.sender;
            if(messageText.equals(raffleWord) && !entreeNames.contains(messageSender)) {
                entreeNames.add(messageSender);
                LOG.info("Adding "+messageSender+" to raffle entrees.");
            }
        }
        if(messageText.charAt(0) != '!') {
            return false;
        } else if(isFirstWordCommand(messageText, triggerWords[0])) {
            String messageSender = message.sender;
            if(manager.isMod(messageSender)) {
                Scanner scn = new Scanner(messageText);
                scn.next();
                if (scn.hasNext()) {
                    String optionCommand = scn.next();
                    int optionIndex = checkOptionIndex(optionWords, optionCommand);
                   // System.out.println("Option index is: " + optionIndex);
                    switch (optionIndex) {
                        // start
                        case 0:
                            if(onGoingRaffle) {
                                LOG.warning("Mod " + messageSender + " tried starting a raffle that was already started.");
                                message.text = "Please enter a trigger word to start the raffle.";
                            } else {
                                if (scn.hasNext()) {
                                    String[] drawTriggerArray = messageText.split(optionWords[0]+" ");
                                    String newDrawTrigger = drawTriggerArray[1];
                                    if(!newDrawTrigger.isEmpty()) {
                                        if(manager.registerCommand(this, newDrawTrigger)) {
                                            onGoingRaffle = true;
                                            raffleWord = newDrawTrigger;
                                            LOG.info("Mod " + messageSender + " started a new raffle with the entree word '"+raffleWord+"'.");
                                            message.text = "Giveaway started! Type '"+newDrawTrigger+"' in chat (without quotes) to enter!";
                                        } else {
                                            LOG.info("Mod " + messageSender + " tried to start a raffle, but used a command/keyword that was reserved! The request keyword/command was '"+newDrawTrigger+"'.");
                                            message.text = "The entered trigger word is already in use.";
                                        }
                                        
                                    }
                                } else {
                                    LOG.warning("Mod "+messageSender + " tried starting a raffle without any entree word.");
                                }
                            }
                            break;
                        // stop
                        case 1:
                            if(onGoingRaffle) {
                                manager.unregisterCommand(this, raffleWord);
                                LOG.info("Mod " + messageSender + " stopped the on-going raffle with the raffle word '"+raffleWord+"'.");
                                raffleWord = "";
                                onGoingRaffle = false;
                                entreeNames.clear();
                                drawnNames.clear();
                                message.text = "Stopped giveaway, winners and entrees cleared.";
                            } else {
                                LOG.warning("Mod " + messageSender + " tried stopping a raffle that wasn't started.");
                                message.text = "Cannot stop giveaway that wasn't started.";
                            }
                            break;
                        // pick
                        case 2:
                            if(!onGoingRaffle) {
                                message.text = "No giveaway currently active.";
                                LOG.info("Mod " + messageSender + " tried to draw people in the raffle, however no raffle had started.");
                                break;
                            } else if(entreeNames.isEmpty()) {
                                LOG.info("Mod " + messageSender + " tried to draw people in the raffle, however there were no entrants.");
                                message.text = "No people have entered yet! Type '"+raffleWord+"' in chat (without quotes) to enter!";
                                break;
                            } else {
                                if(scn.hasNextInt()){
                                    try {
                                        int peopleToDraw = scn.nextInt();
                                        int entreeNamesSize = entreeNames.size();
                                        if(peopleToDraw > entreeNamesSize) {
                                            LOG.info("Mod " + messageSender + " tried to draw " + peopleToDraw + " people, but only " + entreeNamesSize + " entered.");
                                            message.text = "Invalid draw number, only " + entreeNamesSize + (entreeNamesSize==1 ? " person has" : " people have" ) + " entered so far.";
                                            break;
                                        } else if(peopleToDraw <= 0) {
                                            LOG.info("Mod " + messageSender + " tried to draw 0 or less people in the raffle.");
                                            message.text = "Number of people to draw must be 1 or greater!";
                                            break;
                                        }
                                        boolean generating = true;
                                        ArrayList<Integer> usedIntegers = new ArrayList();
                                        Integer randomInt = rng.nextInt(entreeNamesSize);
                                        drawnNames.clear();
                                        while(generating) {
                                            if(drawnNames.size() == peopleToDraw) {
                                                generating = false;
                                            }
                                            if(!usedIntegers.contains(randomInt)) {
                                                usedIntegers.add(randomInt);
                                                String entreeName = entreeNames.get(randomInt);
                                                if(!previousWinners.contains(entreeName)) {
                                                    drawnNames.add(entreeName);
                                                } else {
                                                    LOG.info("Excluding " + entreeName + " from raffle draw, due to win in previous raffle");
                                                    peopleToDraw--;
                                                }
                                            }    
                                            for(int timesToDraw = 0;timesToDraw<5;timesToDraw++) {
                                                randomInt = rng.nextInt(entreeNamesSize);
                                            }
                                        }
                                        previousWinners.clear();
                                        previousWinners.addAll(drawnNames);
                                    } catch (Exception ex) {
                                        LOG.warning("Mod " + messageSender + " entered an invalid number of people to draw.");
                                        message.text = "Invalid number entered for giveaway draw.";
                                        break;
                                    }
                                } else {
                                    LOG.warning("Mod " + messageSender + " did not enter a number of people to draw.");
                                    message.text = "Invalid number entered for giveaway draw.";
                                    break;
                                }
                            }
                        // repeat
                        case 3:
                           // System.out.println("Drawn names size: " + drawnNames.size());
                            if(drawnNames.size() < 0) {
                                LOG.info("Mod " + messageSender + " tried to draw people before using '!draw pick ___'.");
                                message.text = "No winners have been drawn yet, please use '!draw pick {amount}' to draw winners.";
                            } else {
                                String drawnNamesString = getDrawnNames();
                                if (drawnNamesString.isEmpty()) {
                                    LOG.info("Could not draw people for the giveaway, since only previous winners were drawn.");
                                    message.text = "No eligible winners drawn!";
                                } else {
                                    message.text = "The following people won: " + drawnNamesString+"!";
                                    LOG.info("Drew the following winners: " + drawnNamesString);
                                }
                            }
                             break;
                        // clear
                        case 4:
                            entreeNames.clear();
                            drawnNames.clear();
                            message.text = "Giveaway entress and winners cleared.";
                            LOG.info("Moderator  " + messageSender + " cleared the raffle entrees/winners.");
                            break;
                        default:
                            break;
                    }
                    
                } else {
                    message.text = "Invalid options entered. Valid options are 'start'/'stop'/'pick'/'repeat'/'clear'.";
                }
                manager.queueMessage(this, message, true);
            } else {
                LOG.info("Non-mod "+messageSender+" interacted with the Raffle Module.");
            }
            return true;
        }
        return false;
    }

    @Override
    public HashMap<String, String> getParameters() {
        return null;
    }

    @Override
    public void setParameters(HashMap<String, String> propertiesMap) {}
    
    private boolean isFirstWordCommand(String fullMessage, String commandToCheckFor) {
        if(fullMessage.length() < commandToCheckFor.length()) {
            return false;
        } else {
            return fullMessage.substring(0, commandToCheckFor.length()).equals(commandToCheckFor);
        }
    }
    
    private String getDrawnNames() {
        String drawnNamesString = "";
        for (int i = 0; i < drawnNames.size(); i++) {
            drawnNamesString += drawnNames.get(i) + (i == drawnNames.size()-1 ? "" : ", ");
        }
        return drawnNamesString;
    }
    
    private int checkOptionIndex(String[] optionArray, String requestedOption) {
        for (int i = 0; i < optionArray.length; i++) {
            if (optionArray[i].equals(requestedOption)) {
                return i;
            }
        }
        return -1;
    }

}
