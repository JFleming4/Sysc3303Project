package logging;

public class Logger {

    // Enumeration for different types of log levels
    public enum LogLevel
    {
        QUIET(1),
        VERBOSE(2);

        private int rank;
        LogLevel(int rank)
        {
            this.rank = rank;
        }

        /**
         * @param other The logging level to check if it's enabled.
         * @return True if the 'other' logging level is enabled
         */
        public boolean isEnabled(LogLevel other)
        {
            return other.rank >= this.rank;
        }
    }

    // Global log level across all loggers
    private static LogLevel currentLogLevel = LogLevel.QUIET;

    /**
     * Gets the Component name
     */
    public synchronized String getComponentName() {
        return componentName;
    }

    /**
     * Sets Component name
     * @param componentName The new component name
     */
    public synchronized void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    private String componentName;

    /**
     * Create a logger with QUIET level logging
     */
    public Logger(String componentName)
    {
        this.componentName = componentName;
    }

    /**
     * Change global log level
     * @param level The level to change to
     */
    public static synchronized void setLogLevel(LogLevel level)
    {
        if(level == null)
            return;

        currentLogLevel = level;
    }

    /**
     * @return The current Logging Level
     */
    public static synchronized LogLevel getLogLevel()
    {
        return currentLogLevel;
    }

    /**
     * Output verbose text to System.out if verbose mode (or higher) is enabled
     * @param logText The text to log
     */
    public synchronized void logVerbose(String logText)
    {
        log(LogLevel.VERBOSE, logText);
    }

    /**
     * Output quiet text to System.out if quiet mode (or higher) is enabled
     * @param logText The text to log
     */
    public synchronized void logQuiet(String logText)
    {
        log(LogLevel.QUIET, logText);
    }

    /**
     * Neatly prints log text. Splits multi-line text so that the log tag is appended to each line
     * @param level The tag of the log level
     * @param text The text to log
     */
    private synchronized void log(LogLevel level, String text)
    {
        if(level == null || !level.isEnabled(currentLogLevel))
            return;

        // Make sure multi-lined text is appended with the tag
        String[] lines = text.split("\n");

        for (String s : lines)
            System.out.println("[" + componentName + "][" + level.name() +  "]: " + s);
    }

    /**
     * Logs a byte array to verbose output
     * @param bytes The byte array to log
     */
    public synchronized void logVerbose(byte[] bytes)
    {
        logVerbose(getByteArrayString(bytes));
    }

    /**
     * Logs a byte array to quiet output
     * @param bytes The byte array to log
     */
    public synchronized void logQuiet(byte[] bytes)
    {
        logQuiet(getByteArrayString(bytes));
    }

    /**
     * @param bytes The byte array to print
     * @return A comma separated list of all bytes in the array
     */
    private synchronized String getByteArrayString(byte[] bytes)
    {
        StringBuilder byteStr = new StringBuilder("Byte Array: { ");

        // Add individual bytes to the string
        for(int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            byteStr.append(String.format("0x%02X", b));

            // Append separator
            if(i != bytes.length - 1)
                byteStr.append(", ");
        }

        byteStr.append(" }");

        return byteStr.toString();
    }
}
