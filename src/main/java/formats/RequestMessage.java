package formats;


import exceptions.InvalidPacketException;
import logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;

/**
 * Representation of a TFTP Request Message
 */
public class RequestMessage extends Message {
    private final static Logger LOG = new Logger("RequestMessage");
    private MessageType type;
    private String fileName;
    private String mode;

    /**
     * Create a DataPacket object. Throws an exception if type is null
     * @param type The Request Type of the data
     * @param fileName The file name
     * @param mode The mode
     */
    public RequestMessage(MessageType type, String fileName, String mode) {
        if (type == null || !MessageType.isRequestType(type))
            throw new RuntimeException("Invalid request type or request type can not be null");
        this.type = type;
        this.fileName = fileName;
        this.mode = mode;
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
    public String getMode() {
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
        bAOS.write(getMode().getBytes());
        bAOS.write(0);

        return bAOS.toByteArray();
    }

    /**
     * Creates a RequestMessage object from a packet object
     * @param packet The packet object containing the data to be parsed
     * @return The RequestMessage object containing all relevant info
     * @throws InvalidPacketException If there was an error parsing the data
     */
    public static RequestMessage parseDataFromPacket(DatagramPacket packet) throws InvalidPacketException {
        return parseDataFromBytes(Arrays.copyOf(packet.getData(), packet.getLength()));
    }

    /**
     * Creates a RequestMessage object from a byte array
     * @param packet The Data retrieved in a packet
     * @return The RequestMessage object containing all relevant info
     * @throws InvalidPacketException If there was an error parsing the data
     */
    public static RequestMessage parseDataFromBytes(byte[] packet) throws InvalidPacketException {
        // A minimum size of 6 assumes that the packet contains at least
        // one character for the filename and at least one character for the mode
        if (packet.length < 6)
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
            throw new InvalidPacketException("Invalid message type. Must be RRQ or WRQ. Actual: " + requestType);

        // Read filename from bytes
        String fileName = readStringFromBytes(packet, ptr);
        ptr += fileName.length() + 1;

        // Check if we are at the end of the packet (packet is missing mode)
        if (ptr == packet.length)
            throw new InvalidPacketException("End of packet not expected. Missing mode section.");

        // Read Mode from bytes
        String mode = readStringFromBytes(packet, ptr);
        ptr += mode.length() + 1;

        // Check to see if we are at the end of the packet
        if (ptr != packet.length)
            throw new InvalidPacketException("Packet length is too long. There should be no data after the 0 following the mode. Number of extra bytes: " + (packet.length - ptr));

        return new RequestMessage(type, fileName, mode);
    }
}