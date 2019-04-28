/*
 */
package twitchhsbot.extensions;

import java.util.HashMap;
import java.util.logging.Logger;

import twitchhsbot.ModuleManager;
import twitchhsbot.TwitchHSBot;
import twitchhsbot.structures.ChatModuleAccess;
import twitchhsbot.wrappers.ChatMessage;

/**
 *
 * @author Toby
 */
public abstract class ChatModule {
    
    public static final Logger LOG = TwitchHSBot.LOG;
    public final ModuleManager manager = ModuleManager.get();
    public String moduleName;
    
    public abstract ChatModuleAccess registerModule();
    public abstract boolean processMessage(ChatMessage message);
    public abstract HashMap<String, String> getParameters();
    public abstract void setParameters(HashMap<String, String> propertiesMap);
    
    public String getModuleName() {
        return moduleName;
    }
}
