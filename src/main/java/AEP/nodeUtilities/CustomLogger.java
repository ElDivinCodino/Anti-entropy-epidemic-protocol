package AEP.nodeUtilities;

/**
 * Useful to select what to show during the execution, in order to avoid verbose information
 */
public class CustomLogger {
    public static final String ANSI_PREFIX = "\u001B[";

    public static final String ANSI_RESET = ANSI_PREFIX + "0m";
    public static final String ANSI_RED = ANSI_PREFIX + "31m";
    public static final String ANSI_GREEN = ANSI_PREFIX + "32m";
    public static final String ANSI_YELLOW = ANSI_PREFIX + "33m";
    public static final String ANSI_CYAN = ANSI_PREFIX + "36m";
    public static final String ANSI_WHITE = ANSI_PREFIX + "37m";

    public enum LOG_LEVEL { INFO, DEBUG, ERROR, OFF }
    private LOG_LEVEL level;
    private String prefix;
    private String processName;

    public CustomLogger(String processName){
        this.prefix = null;
        this.processName = processName;
    }

    private String applyArgsToString(String message, Object[] args){
        for (Object arg : args) {
            if (arg == null){
                message = message.replaceFirst("\\{\\}", "null");
            } else {
                message = message.replaceFirst("\\{\\}", arg.toString());
            }

        }
        return message;
    }

    private String addProcessName(String prefix){
        if (this.processName == null){
            return prefix;
        }else {
            return prefix + ANSI_CYAN + "[" +  processName + "] " + ANSI_RESET;
        }
    }

    private String addPrefix(String message){
        this.prefix = this.addProcessName(this.prefix);
        if (this.prefix == null){
            return message;
        }else {
            return prefix + message;
        }
    }

    private String format(String message){
        message = addPrefix(message);
        return message;
    }

    public void info(String message, Object... args){
        this.prefix = ANSI_CYAN + "[INFO] " + ANSI_RESET;
        if (this.level != LOG_LEVEL.OFF)
            System.out.println(this.format(this.applyArgsToString(message, args)));
    }

    public void debug(String message, Object... args){
        this.prefix = ANSI_YELLOW + "[DEBUG] " + ANSI_RESET;
        if (this.level == LOG_LEVEL.DEBUG){
            System.out.println(this.format(this.applyArgsToString(message, args)));
        }
    }

    public void error(String message, Object... args){
        this.prefix = ANSI_RED + "[ERROR] " + ANSI_RESET;
        if (this.level == LOG_LEVEL.ERROR)
        System.out.println(this.format(this.applyArgsToString(message, args)));
    }

    public void setLevel(LOG_LEVEL level) {
        this.level = level;
    }
}
