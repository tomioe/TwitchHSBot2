/*
 */
package twitchhsbot.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;
import org.json.JSONObject;
import twitchhsbot.extensions.ChatModule;
import static twitchhsbot.extensions.ChatModule.LOG;
import twitchhsbot.extensions.NetClientAccess;
import twitchhsbot.structures.ChatModuleAccess;
import twitchhsbot.structures.ChatModuleAccess.ModuleDependencies;
import twitchhsbot.structures.NetClientRequest;
import twitchhsbot.structures.NetClientRequest.RequestType;
import twitchhsbot.structures.NetClientResponse;
import twitchhsbot.wrappers.ChatMessage;

public class CommandModule extends ChatModule implements NetClientAccess {

    private ModuleDependencies[] modDeps = {ChatModuleAccess.ModuleDependencies.CHAT_RW,
        ChatModuleAccess.ModuleDependencies.MOD_CHECK,
        ChatModuleAccess.ModuleDependencies.PROPERTIES_RW};
    private final int rateLimit = 3000;
    private final String[] triggerWords = {
        "!command"
    };
    private final String[] optionCommands = {
        "add",
        "delete"
    };
    private String paramPre;
    private final String paramCommand = "command_";
    private final int paramMaxRandomInt = 10000;
    private HashMap<String, String> commandMap = new HashMap();

    private final String netAddress = "localhost";
    private final int netPort = 69;
    private final String netPathPost = "/post-command";
    private final String netPathDelete = "/delete-command";
    private final String netContentType = "application/json";
    private ArrayList<ResponseWrapper> commandPosts = new ArrayList();
    
    public CommandModule() {
        moduleName = "CommandModule";
        paramPre = moduleName + "_";
    }

    @Override
    public ChatModuleAccess registerModule() {
        return new ChatModuleAccess(moduleName, modDeps, rateLimit, triggerWords);
    }

    @Override
    public boolean processMessage(ChatMessage message) {
        String messageText = message.text;

        if (messageText.charAt(0) == '!') {
            for (String commandKey : commandMap.keySet()) {
                if (messageText.equals(commandKey)) {
                    message.text = commandMap.get(commandKey);
                    manager.queueMessage(this, message, false);
                    return true;
                }
            }
            if (isFirstWordCommand(messageText, triggerWords[0])) {
                String messageSender = message.sender;
                if (manager.isMod(messageSender)) {
                    Scanner scn = new Scanner(messageText);
                    String modFailAdd = "Mod " + messageSender + " tried adding a new Custom Command ";
                    String hsBot = scn.next();
                    if (scn.hasNext()) {
                        String optionWord = scn.next();
                        int optionIndex = checkOptionIndex(optionCommands, optionWord);
                        String newCommandTrigger;
                        if(scn.hasNext()) {
                            switch (optionIndex) {
                                // add case
                                case 0:
                                    newCommandTrigger = scn.next();
                                    messageText = messageText.replace(newCommandTrigger, newCommandTrigger.toLowerCase());
                                    newCommandTrigger = newCommandTrigger.toLowerCase();
                                    if (newCommandTrigger.charAt(0) != '!') {
                                        message.text = "Command Trigger has to start with '!'.";
                                        LOG.warning(modFailAdd+"but did not specify a trigger starting with '!'");
                                    } else {
                                        if (commandMap.containsKey(newCommandTrigger)) {
                                            message.text = "Command Trigger already defined.";
                                            LOG.warning(modFailAdd+"but trigger was already defined");

                                        } else {
                                            if (newCommandTrigger.length() <= 1) {
                                                message.text = "Command trigger length must be at least 1 character!";
                                                LOG.warning("Mod " + messageSender + " tried adding a custom command, but the trigger was not long enough.");
                                            } else {
                                                boolean validCommand = true;
                                                int stringIndex = triggerWords[0].length()+optionCommands[optionIndex].length()+newCommandTrigger.length();
                                                
                                                String newNewCommandText = ""; // 3 is for three spaces
                                                try {
                                                    newNewCommandText = messageText.substring(stringIndex+3).trim();
                                                } catch(Exception ex){
                                                    System.out.println("Invalid command!");
                                                    validCommand = false;
                                                }
                                                if (validCommand) {
                                                    if (!newNewCommandText.isEmpty()) {
                                                        if (manager.registerCommand(this, newCommandTrigger)) {
                                                            commandMap.put(newCommandTrigger, newNewCommandText);
                                                            message.text = "Added new Custom Command with trigger '" + newCommandTrigger + "' succesfully!";
                                                            LOG.info("Mod " + messageSender + " added new Custom Command '" + newCommandTrigger + "' with text: '" + newNewCommandText + "'");
                                                            postCommandData(newCommandTrigger, newNewCommandText, messageSender);
                                                            manager.saveProperties();
                                                        } else {
                                                            LOG.warning(modFailAdd + "but the trigger was already taken by another chat module.");
                                                            message.text = "Command Trigger already used by another module.";
                                                        }

                                                    } else {
                                                        message.text = "Command Text cannot be empty!";
                                                        LOG.warning(modFailAdd + "with no text.");
                                                    }
                                                } else {
                                                    LOG.warning(modFailAdd + "but the supplied text was empty.");
                                                    message.text = "Custom Command must have some text.";
                                                }
                                            }
                                        }
                                    }
                                    break;
                                // delete case
                                case 1:
                                    newCommandTrigger = scn.next().toLowerCase();
                                    if(commandMap.containsKey(newCommandTrigger)) {
                                        commandMap.remove(newCommandTrigger);
                                        manager.unregisterCommand(this, newCommandTrigger);
                                        deleteCommand(newCommandTrigger);
                                        message.text = "Removed command '"+newCommandTrigger+"' successfully.";
                                        LOG.info("Mod "+messageSender+" removed custom command '"+newCommandTrigger+"'.");
                                    } else {
                                        LOG.warning("Mod " +messageSender + " tried to delete a non-existing Custom Command.");
                                        message.text = "The specified Command Trigger '" + newCommandTrigger + "' does not exist";
                                    }
                                    break;
                                default:
                                    message.text = "Invalid argument, valid arguments are 'add'/'delete'.";
                            }
                        } else {
                            LOG.warning("Mod " + messageSender + " tried to interact with the command module, but did not specify a Command Trigger.");
                            message.text = "Please specify a Command Trigger as well, starting with '!'.";
                        }
                    } else if(messageText.equals("!commands")) {
                        return false;
                    } else {
                        message.text = "Invalid argument, valid arguments are 'add'/'delete'.";
                        LOG.warning("Mod " + messageSender + " tried to interact with the command module, but didn't type any argument.");
                    }
                    manager.queueMessage(this, message, true);
                } else {
                    LOG.info("Non-mod " + messageSender + " tried to interact with the Command module.");
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public HashMap<String, String> getParameters() {
        HashMap<String, String> saveParams = new HashMap();
        ArrayList<Integer> usedRandomInts = new ArrayList();

        Random rnd = new Random();
        Integer randomInt = rnd.nextInt(paramMaxRandomInt);
        boolean generating = true;
        
        for (String commandSaveTrigger : commandMap.keySet()) {
            do {
                if(!usedRandomInts.contains(randomInt)) {
                    usedRandomInts.add(randomInt);
                    generating = false;
                }
                randomInt = rnd.nextInt(paramMaxRandomInt);
            } while(generating);
            
            String paramSave = paramPre + paramCommand + randomInt;
            saveParams.put(paramSave + "_t", commandSaveTrigger);
            saveParams.put(paramSave + "_c", commandMap.get(commandSaveTrigger));
            LOG.info("Saving Custom Command '"+commandSaveTrigger+"'.");
            generating = true;
        }
        return saveParams;
    }

    @Override
    public void setParameters(HashMap<String, String> propertiesMap) {
        for(String commandKey : propertiesMap.keySet()) {
            if(commandKey.contains(paramCommand) && commandKey.contains("_t")) {
                String triggerToAdd = propertiesMap.get(commandKey);
                String commandToAdd = propertiesMap.get(commandKey.replace("_t", "_c"));
                manager.registerCommand(this, triggerToAdd);
                commandMap.put(triggerToAdd, commandToAdd);
                LOG.info("Loading custom command '"+triggerToAdd+"'.");
            }
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

    private boolean isFirstWordCommand(String fullMessage, String commandToCheckFor) {
        if(fullMessage.length() < commandToCheckFor.length()) {
            return false;
        } else {
            return fullMessage.substring(0, commandToCheckFor.length()).equals(commandToCheckFor);
        }
    }
    
    private void postCommandData(String commandTrigger, String commandText, String commandEntrant) {
        LOG.info("Sending new command data to website.");
        String netData = createCommandJSON(commandTrigger, commandText, commandEntrant);
        forwardNetRequest(new NetClientRequest(netAddress, netPathPost, netPort, netData, netContentType));        
    }
    
    private void deleteCommand(String commandTrigger) {
        LOG.info("Removing command from website.");
        String netData = "{\"trigger\":\""+commandTrigger+"\"}";
        forwardNetRequest(new NetClientRequest(netAddress, netPathDelete, netPort, netData, netContentType));
    }
    
    private String createCommandJSON(String trigger, String command, String entrant) {
        JSONObject obj = new JSONObject();
        HashMap<String, String> map = new HashMap();
        map.put("trigger", trigger);
        map.put("entrant", entrant.equalsIgnoreCase("forsenlol") ? "Forsen" : entrant);
        map.put("command", command);
        obj.put("command", map);
        return obj.toString();
    }
    
    private void forwardNetRequest(NetClientRequest ncr) {
        manager.postRequest(this, ncr, RequestType.POST_REQUEST);
        commandPosts.add(new ResponseWrapper(ncr));
    }

    @Override
    public void netResponse(NetClientResponse ncresp) {
        Iterator it = commandPosts.iterator();
        while(it.hasNext()) {
            ResponseWrapper rew = (ResponseWrapper) it.next();
            NetClientRequest ncr = rew.ncr;
            if(ncresp.getNetRequest().equals(ncr)) {
                if(rew.retryCount++ < 6) {
                    if (!ncresp.isNetResponse() || ncresp.getNetResponseStatusCode() != 200) {
                        LOG.info("A command was sent/deleted on the website, but was not saved. Should be sent again. JSON Data: " + ncr.getNetData());
                        manager.postRequest(this, ncr, RequestType.POST_REQUEST);
                        break;
                    } else {
                        LOG.info("Command was successfully sent/deleted on the website.");
                    }
                } else {
                    LOG.warning("A command was unsuccessfully sent/deleted on the website, retry count exceeded.");
                }
                it.remove();
                break;
            }
        }
    }
    
    private class ResponseWrapper {
        public NetClientRequest ncr;
        public int retryCount = 0;
        public boolean sentSuccessfully = false;
        private ResponseWrapper(NetClientRequest ncr) {
            this.ncr = ncr;
        }
    }
}
