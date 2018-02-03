package formats;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;

import exceptions.InvalidPacketException;

/**
 * Representation of a TFTP ACK Message
 */
public class AckMessage extends Message {

    private int blockNum;

    /**
     * Create the acknowledgement message
     * @param blockNum Unsigned short. The block number. Can not be negative. Limited to max positive short value
     */
    public AckMessage(int blockNum)
    {
        if(blockNum < 0)
            throw new RuntimeException("blockNum can not be less than 0");

        // If blockNum is larger than max positive short value,
        // throw exception
        if (blockNum > 0xFFFF)
            throw new RuntimeException("blockNum can not be more than " + 0xFFFF);
        this.blockNum = blockNum;
    }

    /**
     * @return A byte representation of the message
     * @throws IOException
     */
    @Override
    protected byte[] getBytes() throws IOException {
        ByteArrayOutputStream bAOS = new ByteArrayOutputStream();

        // Write two-byte big endian format of blockNum
        bAOS.write(Message.shortToByteArray((short)blockNum));

        return bAOS.toByteArray();
    }

    /**
     * @return The acknowledged block number
     */
    public int getBlockNum()
    {
        return this.blockNum;
    }

    /**
     * @return The MessageType enumeration value
     */
    @Override
    public MessageType getMessageType() {
        return MessageType.ACK;
    }

    /**
     * Check if two AckMessage objects are equal to each other
     * @param other The other AckMessage
     * @return True if the objects are equals
     */
    @Override
    public boolean equals(Object other)
    {
        if (this == other)
            return true;

        if(!(other instanceof AckMessage))
            return false;

        AckMessage otherAck = (AckMessage) other;
        return this.getMessageType().equals(otherAck.getMessageType()) && this.blockNum == otherAck.blockNum;
    }

    /**
     * Creates a AckMessage object from a packet object
     * @param packet The packet object containing the data to be parsed
     * @return The AckMessage object containing all relevant info
     * @throws InvalidPacketException If there was an error parsing the data
     */
    public static AckMessage parseMessageFromPacket(DatagramPacket packet) throws InvalidPacketException {
        return parseMessageFromBytes(Arrays.copyOf(packet.getData(), packet.getLength()));
    }

    /**
     * Creates a AckMessage object from a byte array
     * @param data The byte array retrieved in a packet
     * @return The AckMessage object containing all relevant info
     * @throws InvalidPacketException If there was an error parsing the data
     */
    public static AckMessage parseMessageFromBytes(byte[] data) throws InvalidPacketException {
        // ACK has packet size of strictly 4
        if (data.length != 4)
            throw new InvalidPacketException("Invalid packet length");

        // Used as a pointer to iterate through the byte array
        int ptr = 0;

        // Read the start byte and increment the current pointer
        byte startByte = data[ptr++];

        // Start byte must be 0, otherwise it is incorrect.
        if (startByte != 0)
            throw new InvalidPacketException("Invalid start byte. Expected 0. Actual: " + startByte);

        // Read the request type and increment the current pointer
        byte requestType = data[ptr++];
        MessageType type = MessageType.getMessageType(requestType);

        // Request type must be valid to continue
        if (!MessageType.ACK.equals(type))
            throw new InvalidPacketException("Invalid message type. Must be ACK (" + MessageType.ACK.getType() + "). Actual: " + requestType);

        // Read 2-byte block number
        int blockNum = Message.byteArrayToUnsignedShort(data, ptr);
        ptr += 2;

        // Check if the type is valid
        if(blockNum < 0)
            throw new InvalidPacketException("The block number can not be less than 0");

        // Check if we are at the end of the data (0 bytes of data)
        if (ptr != data.length)
            throw new InvalidPacketException("The data packet contains too many bytes");

        return new AckMessage(blockNum);
    }


}
