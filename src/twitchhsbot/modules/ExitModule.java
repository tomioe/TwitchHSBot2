/*
 */

package twitchhsbot.modules;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import twitchhsbot.extensions.ChatModule;
import static twitchhsbot.extensions.ChatModule.LOG;
import twitchhsbot.structures.ChatModuleAccess;
import twitchhsbot.wrappers.ChatMessage;

public class ExitModule extends ChatModule {

    @Override
    public ChatModuleAccess registerModule() {
        moduleName = "ExitModule";
        ChatModuleAccess cma = new ChatModuleAccess(moduleName, null, 0);
        return cma;
    }

    @Override
    public boolean processMessage(ChatMessage message) {
        if (message.text.equals("!exit")) {
            if (manager.isMod(message.sender)) {
                manager.saveProperties();
                LOG.info("Shutting down.");
                Timer t1 = new Timer();
                t1.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        System.exit(2);
                    }
                }, 500);
                return true;
            } else {
                LOG.info("Non-mod " + message.sender + " tried to exit the bot.");
                return true;
            }
        }
        return false;
    }

    @Override
    public HashMap<String, String> getParameters() {
        return null;
    }

    @Override
    public void setParameters(HashMap<String, String> propertiesMap) {}

}
