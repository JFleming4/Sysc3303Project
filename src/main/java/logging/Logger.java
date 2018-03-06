package logging;

import exceptions.InvalidPacketException;
import formats.AckMessage;
import formats.DataMessage;
import formats.ErrorMessage;
import formats.Message;
import formats.RequestMessage;

import java.net.DatagramPacket;

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

        // Occurs if the input text is strictly a new line character (or empty String)
        if(lines.length == 0)
            System.out.println();


        for (String s : lines)
            System.out.println("[" + componentName + "][" + level.name() +  "]: " + s);
    }

    /**
     * Logs a byte array to verbose output
     * @param bytes The byte array to log
     */
    public synchronized void logVerbose(byte[] bytes)
    {
        logVerbose(getByteArrayString(bytes, 0, bytes.length));
    }

    /**
     * Logs a byte array to quiet output
     * @param bytes The byte array to log
     */
    public synchronized void logQuiet(byte[] bytes)
    {
        logQuiet(getByteArrayString(bytes, 0, bytes.length));
    }
    
    /**
     * Logs a packet to verbose output
     * @param packet The packet to log
     */
    public synchronized void logVerbose(DatagramPacket packet)
    {
        logVerbose(getPacketString(packet));
    }

    /**
     * Logs a packet to quiet output
     * @param packet The packet to log
     */
    public synchronized void logQuiet(DatagramPacket packet)
    {
        logQuiet(getPacketString(packet));
    }

    /**
     * Logs a message to verbose output
     * @param message The message to log
     */
    public synchronized void logVerbose(Message message)
    {
        logVerbose(message.toString());
    }

    /**
     * Logs a message to quiet output
     * @param message The message to log
     */
    public synchronized void logQuiet(Message message){ logQuiet(message.toString()); }

    /**
     * @param packet The packet to print
     * @return A formatted string with packet data
     */
    private synchronized String getPacketString(DatagramPacket packet)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Packet Information:");
        builder.append(System.lineSeparator());

        builder.append("Packet Address: ");
        builder.append(packet.getAddress());
        builder.append(System.lineSeparator());

        builder.append("Packet Port: ");
        builder.append(packet.getPort());
        builder.append(System.lineSeparator());

        builder.append("Packet Data: ");
        builder.append(System.lineSeparator());

        try
        {
            Message message = Message.parseGenericMessage(packet);
            builder.append(message);

        }catch (InvalidPacketException iPE)
        {
            builder.append("Could Not Read Packet Data. Invalid Packet.");
            return builder.toString();
        }

        return builder.toString();
    }

    /**
     * @param bytes The byte array to print
     * @param offset the starting byte to print
     * @param length the point at which to truncate printing if it's less than the length of bytes
     * @return A comma separated list of all bytes in the array
     */
    private synchronized String getByteArrayString(byte[] bytes, int offset, int length)
    {
        StringBuilder byteStr = new StringBuilder("Byte Array: { ");

        // Add individual bytes to the string
        for(int i = offset; i < length; i++) {
            byte b = bytes[i];
            byteStr.append(String.format("0x%02X", b));

            // Append separator
            if(i != length - 1)
                byteStr.append(", ");
        }

        byteStr.append(" }");

        return byteStr.toString();
    }
}
