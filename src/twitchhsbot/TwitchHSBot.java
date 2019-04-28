package twitchhsbot;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import twitchhsbot.utilities.PropertiesManager;

/**
 *
 * @author Toby
 */
public class TwitchHSBot {
    
    public static final Logger LOG = Logger.getLogger(TwitchHSBot.class.getName());
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String channelName = null;
        if (args.length == 1) {
            channelName = args[0];
        } else {
            System.out.println("Invalid argument, please specify channel to join.");
            System.exit(1);
        }
        if (channelName == null) {
            System.exit(1);
        }
        File f = new File(channelName+".config");
        TwitchHSBot twb;
        HashMap<String, String> configMap = null;
        if(f.exists()) {
            configMap = PropertiesManager.get().loadConfig(f);
        }
        twb = new TwitchHSBot(channelName, configMap);
    }
    
    private final ModuleManager modman;
    
    public TwitchHSBot(String channelName, HashMap<String,String> configMap) {
        initLogging(channelName);
        modman = new ModuleManager();
        modman.setInstance(modman);
        modman.setChannelName(channelName);
        
        modman.loadModules((configMap == null ? "" : configMap.get("disabled_modules")));
        modman.connectIRC(configMap);
    }
    
    
    private void initLogging(String channelName){
        try {
            File loggingDir = new File(System.getProperty("user.dir") + "/logs/");
            if(!loggingDir.exists()){
                loggingDir.mkdir();
            }
            SimpleDateFormat timeFormatter = new SimpleDateFormat("yyyyMMdd-HHmm");
            FileHandler fh = new FileHandler(System.getProperty("user.dir") + System.getProperty("file.separator")+ "logs"+ System.getProperty("file.separator")+channelName+"."+timeFormatter.format(Calendar.getInstance().getTime())+".log");
            LOG.getLogger("").addHandler(fh);
            LOG.setLevel(Level.CONFIG);
            fh.setFormatter(new SimpleFormatter());
            LOG.log(Level.INFO,"[MAIN] Logger initialized.");
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }
    
}
