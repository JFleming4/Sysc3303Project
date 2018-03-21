package formats;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;

import exceptions.InvalidPacketException;

/**
 * Representation of a TFTP Request Message
 */
public class RequestMessage extends Message {
    public static final MessageMode DEFAULT_MODE = MessageMode.NET_ASCII;
    private MessageType type;
    private String fileName;
    private MessageMode mode;

    /**
     * Use enumeration to keep track of all valid Message Modes in a Request Packet
     */
    enum MessageMode
    {
        NET_ASCII("netascii"),
        OCTET("octet"),
        MAIL("mail");

        private String modeName;

        MessageMode(String modeName)
        {
            this.modeName = modeName;
        }

        /**
         * @return The mode name of the enum value
         */
        public String getModeName() {
            return modeName;
        }

        /**
         * Gets a MessageMode enum value from the mode parameter.
         * @param mode The mode to search for
         * @return The MessageMode enum value, or null if no mode enum matches the mode parameter
         */
        public static MessageMode getModeEnum(String mode)
        {
            for(MessageMode msgMode : MessageMode.values())
            {
                if(msgMode.modeName.equalsIgnoreCase(mode))
                    return msgMode;
            }

            // Return null if no match is found
            return null;
        }
    }

    /**
     * Create a DataPacket object. Throws an exception if type is null
     * @param type The Request Type of the data (must be MessageType.RRQ or WWQ)
     * @param fileName The file name
     * @param mode The mode
     */
    public RequestMessage(MessageType type, String fileName, MessageMode mode) {
        if (type == null || !MessageType.isRequestType(type))
            throw new RuntimeException("Invalid request type or request type can not be null");
        this.type = type;
        this.fileName = fileName;
        this.mode = mode;
    }

    /**
     * Create a DataPacket object. Throws an exception if type is null. Uses default mode.
     * @param type The Request Type of the data
     * @param fileName The file name
     */
    public RequestMessage(MessageType type, String fileName) {
        this(type, fileName, DEFAULT_MODE);
    }

    @Override
    public MessageType getMessageType() {
        return this.type;
    }

    /**
     * @return The file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return The mode
     */
    public MessageMode getMode() {
        return mode;
    }

    /**
     * @return A byte array representation of this object, to be sent in a packet
     * @throws IOException
     */
    @Override
    protected byte[] getBytes() throws IOException
    {
        // Create byte stream, write to byte stream and create byte array from stream
        ByteArrayOutputStream bAOS = new ByteArrayOutputStream();
        bAOS.write(getFileName().getBytes());
        bAOS.write(0);
        bAOS.write(getMode().getModeName().getBytes());
        bAOS.write(0);

        return bAOS.toByteArray();
    }

    /**
     * Check if two RequestMessage objects are equal to each other
     * @param other The other RequestMessage
     * @return True if the objects are equals
     */
    @Override
    public boolean equals(Object other)
    {
        if (this == other)
            return true;

        if(!(other instanceof RequestMessage))
            return false;

        RequestMessage otherMsg = (RequestMessage) other;

        return this.getMessageType().equals(otherMsg.getMessageType())
                && this.fileName.equals(otherMsg.fileName)
                && this.mode.equals(otherMsg.mode);
    }

    /**
     * Creates a RequestMessage object from a packet object
     * @param packet The packet object containing the data to be parsed
     * @return The RequestMessage object containing all relevant info
     * @throws InvalidPacketException If there was an error parsing the data
     */
    public static RequestMessage parseMessage(DatagramPacket packet) throws InvalidPacketException {
        return parseMessage(Arrays.copyOf(packet.getData(), packet.getLength()));
    }

    /**
     * Creates a RequestMessage object from a byte array
     * @param packet The Data retrieved in a packet
     * @return The RequestMessage object containing all relevant info
     * @throws InvalidPacketException If there was an error parsing the data
     */
    public static RequestMessage parseMessage(byte[] packet) throws InvalidPacketException {
        // A minimum size of 4 assumes that the packet contains the two byte opcode, and two zeros (one
        // for terminating file name, and one for terminating mode - where file name and mode are empty)
        if (packet.length < 4)
            throw new InvalidPacketException("Packet length too short");

        // Used as a pointer to iterate through the byte array
        int ptr = 0;

        // Read the start byte and increment the current pointer
        byte startByte = packet[ptr++];

        // Start byte must be 0, otherwise it is incorrect.
        if (startByte != 0)
            throw new InvalidPacketException("Invalid start byte. Expected 0. Actual: " + startByte);

        // Read the request type and increment the current pointer
        byte requestType = packet[ptr++];
        MessageType type = MessageType.getMessageType(requestType);

        // Request type must be valid to continue
        if (!MessageType.isRequestType(type))
            throw new InvalidPacketException("Invalid message type. Must be RRQ or WRQ. Actual: " + type);

        // Read filename from bytes
        String fileName = readStringFromBytes(packet, ptr);
        ptr += fileName.length() + 1;

        // Check if we are at the end of the packet (packet is missing mode)
        if (ptr == packet.length)
            throw new InvalidPacketException("End of packet not expected. Missing mode section.");

        // Read Mode from bytes
        String modeName = readStringFromBytes(packet, ptr);
        ptr += modeName.length() + 1;

        // Check mode is a valid enum value
        MessageMode mode = MessageMode.getModeEnum(modeName);

        if(mode == null)
            throw new InvalidPacketException("Request Mode " + modeName + " is not a valid mode");

        // Check to see if we are at the end of the packet
        if (ptr != packet.length)
            throw new InvalidPacketException("Packet length is too long. There should be no data after the 0 following the mode. Number of extra bytes: " + (packet.length - ptr));

        return new RequestMessage(type, fileName, mode);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("File Name: ");
        builder.append(getFileName());
        builder.append(System.lineSeparator());

        builder.append("File Mode: ");
        builder.append(getMode());
        builder.append(System.lineSeparator());

        return super.toString() + builder.toString();
    }
}
