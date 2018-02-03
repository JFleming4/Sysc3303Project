package logging;

import exceptions.InvalidPacketException;
import formats.*;

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

        builder.append("Packet Type: ");

        // Get packet type
        if(packet.getData().length < 2) {
            builder.append("INVALID");
            return builder.toString();
        }

        Message.MessageType type = Message.MessageType.getMessageType((int)packet.getData()[1]);

        if(type == null) {
            builder.append("INVALID");
            return builder.toString();
        }
        builder.append(type.name());
        builder.append(System.lineSeparator());

        builder.append("Message Data:");
        builder.append(System.lineSeparator());

        try{

            // Print applicable data
            if(Message.MessageType.isRequestType(type))
            {
                RequestMessage msg = RequestMessage.parseMessageFromPacket(packet);
                builder.append("Packet File Name: ");
                builder.append(msg.getFileName());
                builder.append(System.lineSeparator());

                builder.append("Packet File Name: ");
                builder.append(msg.getMode());
                builder.append(System.lineSeparator());
            }
            else if(type.equals(Message.MessageType.ACK))
            {
                AckMessage msg = AckMessage.parseMessageFromPacket(packet);
                builder.append("Block Number: ");
                builder.append(msg.getBlockNum());
                builder.append(System.lineSeparator());
            }
            else if(type.equals(Message.MessageType.DATA))
            {
                DataMessage msg = DataMessage.parseMessageFromPacket(packet);
                builder.append("Block Number: ");
                builder.append(msg.getBlockNum());
                builder.append(System.lineSeparator());

                builder.append("Number of bytes of data: ");
                builder.append(msg.getDataSize());
                builder.append(System.lineSeparator());
            }
            else if(type.equals(Message.MessageType.ERROR))
            {
                ErrorMessage msg = ErrorMessage.parseMessageFromPacket(packet);
                builder.append("Error Code: ");
                builder.append(msg.getErrorType().getCode());
                builder.append(System.lineSeparator());

                builder.append("Error Message: ");
                builder.append(msg.getMessage());
                builder.append(System.lineSeparator());
            }
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
