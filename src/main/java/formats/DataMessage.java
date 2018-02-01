package formats;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import exceptions.InvalidPacketException;

/**
 * Representation of a TFTP Data Message
 */
public class DataMessage extends Message{

    private int blockNum;
    private byte[] data;
    private SocketAddress socketAddress;
    public static final int MAX_BLOCK_SIZE = 512;

    /**
     * Create a data message.
     * @param blockNum The block number. Must be >= 1 otherwise a runtime exception will be thrown
     * @param data  The data to be included in the message. Maximum data size of 512 bytes. Will be truncated if necessary.
     */
    public DataMessage(int blockNum, byte[] data)
    {
       this(blockNum, data, null);
    }

    /**
     * Create a data message with socketAddress.
     * @param blockNum The block number. Must be >= 1 otherwise a runtime exception will be thrown
     * @param data  The data to be included in the message. Maximum data size of 512 bytes. Will be truncated if necessary.
     * @param socketAddress The socket address of the sender
     */
    private DataMessage(int blockNum, byte[] data, SocketAddress socketAddress)
    {
        if(blockNum < 1)
            throw new RuntimeException("blockNum can not be less than 1");

        this.blockNum = blockNum;
        this.data = Arrays.copyOf(data, Math.min(data.length, MAX_BLOCK_SIZE));
        this.socketAddress = socketAddress;
    }

    /**
     * @return The block number of the given data
     */
    public int getBlockNum() {
        return blockNum;
    }

    /**
     * @return The sender socket address
     */
    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    /**
     * @return True if this block is the final block in a sequence of blocks
     */
    public boolean isFinalBlock() {
        return data.length != MAX_BLOCK_SIZE;
    }

    /**
     * Returns a copy of the data, in case of accidental modification to the byte array outside this class
     */
    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    /**
     * @return The size of the data block, without having to call getData (expensive call)
     */
    public int getDataSize()
    {
        return this.data.length;
    }

    @Override
    protected byte[] getBytes() throws IOException {
        ByteArrayOutputStream bAOS = new ByteArrayOutputStream();

        // Write big endian byte array representation of blockNum
        bAOS.write(Message.shortToByteArray((short)blockNum));

        // Write byte buffer
        bAOS.write(this.data);
        return bAOS.toByteArray();
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.DATA;
    }

    /**
     * Creates a listing of DataMessage objects that represent the byte array passed in
     * @param data The data to parse into a data message sequence
     * @return The sequence of data messages
     */
    public static List<DataMessage> createDataMessageSequence(byte[] data)
    {
        // Calculate number of blocks needed
        int numBlocks = data.length / MAX_BLOCK_SIZE + 1;

        List<DataMessage> dataSequence = new ArrayList<>();

        // Truncate the data into blocks, and encapsulate them into a DataMessage object
        for(int i = 0; i < numBlocks; i++)
        {
            byte[] curBlock = Arrays.copyOfRange(data, i * MAX_BLOCK_SIZE, Math.min(i * MAX_BLOCK_SIZE + MAX_BLOCK_SIZE, data.length));
            DataMessage msg = new DataMessage((short)(i + 1), curBlock);
            dataSequence.add(msg);
        }

        return dataSequence;
    }

    /**
     * Creates a DataMessage object from a packet object
     * @param packet The packet object containing the data to be parsed
     * @return The DataMessage object containing all relevant info
     * @throws InvalidPacketException If there was an error parsing the data
     */
    public static DataMessage parseDataFromPacket(DatagramPacket packet) throws InvalidPacketException {
        return parseDataFromBytes(Arrays.copyOf(packet.getData(), packet.getLength()), packet.getSocketAddress());
    }

    /**
     * Creates a DataMessage object from a byte array
     * @param data The Data retrieved in a packet
     * @param socketAddress The socket address of the sender
     * @return The DataMessage object containing all relevant info
     * @throws InvalidPacketException If there was an error parsing the data
     */
    private static DataMessage parseDataFromBytes(byte[] data, SocketAddress socketAddress) throws InvalidPacketException {
        // Data Messages have a minimum size of 4.
        if (data.length < 4)
            throw new InvalidPacketException("Packet length too short");

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
        if (!MessageType.DATA.equals(type))
            throw new InvalidPacketException("Invalid message type. Must be DATA (" + MessageType.DATA.getType() + "). Actual: " + requestType);

        // Read 2-byte block number
        int blockNum = Message.byteArrayToUnsignedShort(data, ptr);
        ptr += 2;

        // Check if the type is valid
        if(blockNum < 1)
            throw new InvalidPacketException("The block number can not be less than 1");

        // Parse the data sent in the packet
        byte[] sentData;

        // Check if we are at the end of the data (0 bytes of data)
        if (ptr == data.length)
            sentData = new byte[0];
        else
            sentData = Arrays.copyOfRange(data, ptr, data.length);

        return new DataMessage(blockNum, sentData, socketAddress);
    }
}
