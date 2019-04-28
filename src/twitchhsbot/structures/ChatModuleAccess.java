/*
 */

package twitchhsbot.structures;

public class ChatModuleAccess {
    
    public enum ModuleDependencies {
        MOD_CHECK,
        CHAT_W,
        CHAT_R,
        CHAT_RW,
        PROPERTIES_RW,
        NET_ACCESS
    }
    
    private String moduleName;
    private ModuleDependencies[] moduleDependencies;
    private int moduleRateLimit;
    private String[] moduleReservedCommands;
    
    public ChatModuleAccess(String moduleName,
                            ModuleDependencies[] moduleDependencies,
                            int moduleRateLimit) {
        this.moduleName = moduleName;
        this.moduleDependencies = moduleDependencies;
        this.moduleRateLimit = moduleRateLimit;
        moduleReservedCommands = null;
    }
    
    public ChatModuleAccess(String moduleName,
                            ModuleDependencies[] moduleDependencies,
                            int moduleRateLimit,
                            String[] moduleReservedCommands) {
        this(moduleName, moduleDependencies, moduleRateLimit);
        this.moduleReservedCommands = moduleReservedCommands;
    }

    public String getModuleName() {
        return moduleName;
    }
    
    public int getRateLimit() {
        return moduleRateLimit;
    }

    public String[] getModuleReservedCommands() {
        return moduleReservedCommands;
    }

    public ModuleDependencies[] getModuleDependencies() {
        return moduleDependencies;
    }
    
}
