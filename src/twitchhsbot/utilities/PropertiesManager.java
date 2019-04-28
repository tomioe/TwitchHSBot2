/*
 */

package twitchhsbot.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Logger;
import twitchhsbot.TwitchHSBot;

public class PropertiesManager {
    
    private static final Logger LOG = TwitchHSBot.LOG;
    private static PropertiesManager instance;
    public static PropertiesManager get() {
        if(instance == null) {
            instance = new PropertiesManager();
        }
        return instance;
    }
    
    
    private final String fileExtension = "properties";
    private String propertiesName;
    
    private PropertiesManager() {
        
    }
    
    public void saveChannelProperties(HashMap<String, String> propertiesMap) {
        try {
            Properties props = new Properties();
            for(String key : propertiesMap.keySet()) {
                props.setProperty(key, propertiesMap.get(key));
            }
            File f = new File(propertiesName);
            OutputStream out = new FileOutputStream(f);
            props.store(out, "");
        } catch (Exception ex) {
            LOG.info("Saving properties failed!");
        }
        LOG.info("Properties successfully saved.");
    }
    
    public HashMap<String, String> loadChannelProperties(String channelName) {
        if(propertiesName != null && !propertiesName.isEmpty()) {
            LOG.warning("No channel name specified for the properties file!");
            return null;
        } else {
            propertiesName = channelName+"."+fileExtension;
        }
        HashMap<String, String> loadedParams = null;
        Properties props = new Properties();
        InputStream is = null;
        File f = new File(propertiesName);
        
        try {
            is = new FileInputStream(f);
        } catch (Exception ex) {
            LOG.warning("First properties load failed, trying next!");
        }
        if(is == null) {
            try {
                is = getClass().getResourceAsStream(propertiesName);
            } catch (Exception ex) {
                LOG.warning("Second load failed, unable to load properties!");
            }
        }
        
        if(is != null) {
            try {
                props.load(is);
            } catch (Exception ex) {
                LOG.warning("Unable to load the properties file.");
            }
            loadedParams = new HashMap();
            Enumeration e = props.propertyNames();
            
            while(e.hasMoreElements()) {
                String key = (String) e.nextElement();
                loadedParams.put(key, props.getProperty(key));
            }
            LOG.info("Properties successfully loaded!");
        } else {
            try {
                LOG.info("Properties file not found, creating new!");
                f.createNewFile();
            } catch(Exception ex) {
                LOG.warning("Properties file could not be created!");
            }
        }
        return loadedParams;
    }
    
    public HashMap<String, String> loadConfig(File f) {
        if(f != null) {
            try {
                InputStream is = new FileInputStream(f);
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                int disabedModules = 0;
                HashMap<String, String> optionMap = new HashMap();
                while((line = br.readLine()) != null) {
                    if(line.contains("pw")) {
                        optionMap.put("irc_pw", splitAtEquals(line));
                    } else if(line.contains("user")) {
                        optionMap.put("irc_user", splitAtEquals(line));
                    } else if(line.contains("disabled")) {
                        optionMap.put("disabled_modules", splitAtEquals(line));
                    }
                }
                return optionMap;
            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("[MAIN] Failed to load channel configuration");
            }
        }
        return null;
    }
    
    private String splitAtEquals(String inputString) {
        return inputString.split("=")[1];
    }
    
    
}
