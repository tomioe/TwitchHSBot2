/*
 */
package twitchhsbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import twitchhsbot.extensions.ChatModule;
import twitchhsbot.extensions.NetClientAccess;
import twitchhsbot.structures.ChatModuleAccess;
import twitchhsbot.structures.NetClientRequest;
import twitchhsbot.structures.NetClientRequest.RequestType;
import twitchhsbot.utilities.NetClient;
import twitchhsbot.utilities.PropertiesManager;
import twitchhsbot.wrappers.ChatMessage;

public class ModuleManager {

    /* Static or/and final application fields */
    public static final Logger LOG = TwitchHSBot.LOG;
    private static ModuleManager inst;

    public static void setInstance(ModuleManager modman) {
        inst = modman;
    }

    public static ModuleManager get() {
        return inst;
    }
    /* IRC related fields */
    private final String modTrigger = "moderators";
    private final String subTrigger = "subscriber";
    private String channelName = null;
    private IRCClient ircc;
    
    /* Utility related fields */
    private final String modulePack = "twitchhsbot.modules";
    private final PropertiesManager propman = PropertiesManager.get();
    private final NetClient netc = NetClient.get();
    private ArrayList<String> modList;
    private ArrayList<String> subList;
    
    /* Handle net requests for non-default (e.g. non-Forsen) channels */
    private boolean disablePostRequests = false;
    
    
    /* Module related fields */
    // Map key: module name, map value: RCM object (contains CMA and module itself)
    private HashMap<String, RegisteredChatModule> moduleMap;
    // Map key: command name, map value: name of the module
    private HashMap<String, String> moduleCommandMap;

    public ModuleManager() {
        LOG.info("Starting ModuleManager");
        moduleMap = new HashMap();
        moduleCommandMap = new HashMap();
        modList = new ArrayList();
        subList = new ArrayList();
        ircc = new IRCClient();
        

        // init the other internal utilities (netclient, properties, etc.) 
    }
    

    /* IRC Handling */
    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        if (this.channelName == null) {
            this.channelName = channelName;

            modList.add(this.channelName);
        }
    }
    
    public void connectIRC(HashMap<String, String> configMap) {
        if(configMap != null) {
            disablePostRequests = true;
        }
        ircc.connectChannel(configMap, this.channelName);
    }

    public void processMessage(ChatMessage message) {
        String messageSender = message.sender;
        String messageText = message.text;

        
        // is the sender JTV? then the message is used by the manager itself.
        if (messageSender.equalsIgnoreCase("jtv")) {
            if (messageText.contains(modTrigger)) {
                messageText = messageText.replace("The moderators of this room are: ", "");
                String[] channelMods = messageText.split(", ");
                modList.addAll(Arrays.asList(channelMods));
                if (modList.contains(ircc.defaultAuthUsername)) {
                    ircc.setModRateLimit(true);
                } else {
                    ircc.setModRateLimit(false);
                }
                if(!modList.contains("xx_k")){
                    modList.add("xx_k");
                }
                LOG.info("Updated moderator list.");
                ircc.setModList(true);
                return;
            } else if (messageText.contains(subTrigger)) {
                String[] detectedSub = messageText.split(" ");
                if (!subList.contains(detectedSub[1])) {
                    subList.add(detectedSub[1]);
                    //LOG.info("Added subscriber: " + detectedSub[1]);
                }
                return;
            }
        }
//        System.out.println("forwarding: '" +messageText+"'");
        int i = 1;
        //System.out.print("FWD: ");
        for (String moduleName : moduleMap.keySet()) {
            ChatModule module = moduleMap.get(moduleName).cm;
            // the following checks if the module which received the message
            // was able to use it and has performed processing on it.
            // if it did, no need to forward to other modules.
            //String modString = moduleName.replace("Module", "");
            try {
                if(module.processMessage(message)) {
                    break;
                }
            } catch (Exception ex) {
                LOG.warning("Module " + module.getModuleName() + " threw exception '"+ex.getClass().getName() + "'.");
                ex.printStackTrace();
            }
        }
    }

    public void queueMessage(ChatModule cm, ChatMessage mes, boolean ignoreRateLimit) {
        RegisteredChatModule rcm = moduleMap.get(cm.getModuleName());
        if (rcm != null) { // maybe also check if has CHAT_W/CHAT_RW
            if (ignoreRateLimit) {
                ircc.sendSyncMessage(mes);
            } else {
                if (rcm.isReady()) {
                    ircc.sendSyncMessage(mes);
                    rcm.cooldown();
                } else {
                    LOG.info(cm.getModuleName() + " tried to send a message, but was internally limited.");
                }
            }
        } else {
            LOG.warning("Non-registered module '" + cm.getModuleName() + "' tried to send a chat message.");
        }
    }
    
    public void queueMessage(final ChatModule cm, final ChatMessage mes, final boolean ignoreRateLimit, boolean repeat) {
        if(repeat) {
            queueMessage(cm,mes,ignoreRateLimit);
            new Timer().schedule(new TimerTask(){
                @Override
                public void run() {
                    queueMessage(cm,mes,ignoreRateLimit);
                }
            }, 500);
        } else {
            queueMessage(cm,mes,ignoreRateLimit);
        }
    }

    /* Utility Handling */
    public boolean isMod(String user) {
        return modList.contains(user);
    }

    public boolean isSub(String user) {
        return subList.contains(user);
    }

    public void saveProperties() {
        HashMap<String, String> totalModuleParams = new HashMap();
        for (String key : moduleMap.keySet()) {
            RegisteredChatModule rcm = moduleMap.get(key);
            if (rcm != null) {
                ChatModule saveChatModule = rcm.cm;
                HashMap<String, String> moduleProps = saveChatModule.getParameters();
                if (moduleProps != null) {
                    for (String saveKey : moduleProps.keySet()) {
                        totalModuleParams.put(saveKey, moduleProps.get(saveKey));
                    }
                }
            }
        }
        propman.saveChannelProperties(totalModuleParams);
    }

    /* Module Handling */
    public void loadModules(String disabledModules) {
        // load up all the properties
        HashMap<String, String> loadedProps = propman.loadChannelProperties(this.channelName);
        // use the google Reflections API to scan for classes of package twitchhsbot.modules
        Reflections refls = new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forPackage(modulePack, null)));
        // make sure the classes are an extension of the ChatModule abstract class
        Set<Class<? extends ChatModule>> chatModules = refls.getSubTypesOf(twitchhsbot.extensions.ChatModule.class);
        // now loop through each of the found classes and load their registration method
        for (Class<? extends ChatModule> cm : chatModules) {
            String newRegistarName = cm.getName();
            boolean registerStatus = false;
            
            String moduleName = newRegistarName.substring("twitchhsbot.modules.".length());
            if(disabledModules.contains(moduleName)){
                continue;
            }
            try {
                // from the scanned classes, load the next found class through reflection
                ChatModule newRegistar = (ChatModule) Class.forName(newRegistarName).newInstance();
                // register this class
                registerStatus = registerModule(newRegistar, newRegistar.registerModule());
                // load the modules parameters if the module registered successfully
                if (registerStatus && loadedProps != null) {
                    for (String key : loadedProps.keySet()) {
                        if (key.contains(newRegistar.getModuleName())) {
                            newRegistar.setParameters(loadedProps);
                            break;
                        }
                    }
                }
            } catch (Exception ex) {
                LOG.warning("Could not register module '" + newRegistarName + "', invalid module implementation: " + ex.getClass().getName());
            }
        }
    }

    private boolean registerModule(ChatModule cm, ChatModuleAccess cma) {
        String moduleName = cma.getModuleName();
        String logError = "Module '" + moduleName + "' tried to register";
        LOG.info("Registering module '" + moduleName+"'.");
        if (moduleMap.containsKey(cma.getModuleName())) {
            LOG.warning(logError + " but name was already taken.");
        } else {
            // Some modules might not need to register commands, and will return null if they don't 
            String[] requestedCommands = cma.getModuleReservedCommands();
            if (requestedCommands != null) {
                for (int i = 0; i < requestedCommands.length; i++) {
                    String requestedCommand = requestedCommands[i];
                    if (moduleCommandMap.containsKey(requestedCommand)) {
                        LOG.warning(logError + " but it tried registering command '" + requestedCommand + "' that was already registered to module '" + moduleCommandMap.get(requestedCommand) + "'.");
                        return false;
                    } else {
                        //LOG.info("Module '"+moduleName+"' registered command '"+requestedCommand+"'.");
                        moduleCommandMap.put(requestedCommand, moduleName);
                    }
                }
            }
            RegisteredChatModule rcm = new RegisteredChatModule(cm, cma);
            moduleMap.put(cma.getModuleName(), rcm);
            LOG.info("Module '" + moduleName + "' registered successfully.");
            return true;
        }
        return false;
    }

    public void getRequest(NetClientAccess nca, NetClientRequest ncreq) {
        // possibly check if the module is registered and also if the correct properties are there
        if (ncreq != null) {
            netc.getRequest(ncreq, nca);
        }
    }
    
    public void postRequest(NetClientAccess nca, NetClientRequest ncreq, RequestType reqt) {
        if(!disablePostRequests && ncreq != null) {
            netc.postRequest(ncreq, nca, reqt);
        } else {
            LOG.info("");
        }
    }

    public ChatModule getModule(String key) {
        return moduleMap.get(key).cm;
    }

    public boolean registerCommand(ChatModule chm, String reqCommand) {
        if (moduleCommandMap.containsKey(reqCommand)) {
            LOG.warning("Module '" + chm.getModuleName() + "' tried to register command '" + reqCommand + "', but this command was already registered by module '" + moduleCommandMap.get(reqCommand) + "'.");
        } else {
            moduleCommandMap.put(reqCommand, chm.getModuleName());
            LOG.info("Module '" + chm.getModuleName() + "' registered command '" + reqCommand + "' successfully.");
            return true;
        }
        return false;
    }
    
    public boolean unregisterCommand(ChatModule chm, String unreqCommand) {
        if(!moduleCommandMap.containsKey(unreqCommand)) {
            LOG.warning("Module '"+chm.getModuleName()+"' tried to unregister command " + unreqCommand +", but the command was not previously registered");
        } else {
            moduleCommandMap.remove(unreqCommand);
            LOG.info("Module '"+chm.getModuleName()+"' unregistered command "+unreqCommand+"' successfully.");
            return true;
        }
        return false;
    }
    
    private class RegisteredChatModule {

        private ChatModule cm;
        private ChatModuleAccess cma;
        private Timer internalTimer = new Timer();
        private int cooldownTime = 3000;
        private boolean ready = true;

        public RegisteredChatModule(ChatModule cm, ChatModuleAccess cma) {
            this.cm = cm;
            this.cma = cma;
            cooldownTime = cma.getRateLimit();
        }

        public boolean isReady() {
            return ready;
        }

        public void cooldown() {
            ready = false;
            internalTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    ready = true;
                }
            }, cooldownTime);
        }
    }
}
