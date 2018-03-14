package formats;

import exceptions.InvalidPacketException;
import javafx.util.Pair;
import org.junit.Before;
import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.*;

import static org.junit.Assert.*;

public class DataMessageTest {

    private DataMessage validMessage;
    private byte[] messageData;
    private int blockNum = 1;
    private Map<DataMessage, byte[]> validParseData;
    private List<Pair<String, byte[]>> invalidParseData;

    /**
     * Pre-test Setup (valid messages, parse data, etc)
     */
    @Before
    public void setUp() throws IOException
    {
        this.messageData = "This is my test data".getBytes();
        this.validMessage = new DataMessage(blockNum, messageData);

        // Set up valid parsing data
        validParseData = new HashMap<>();
        invalidParseData = new ArrayList<>();

        // Empty data block
        validParseData.put(new DataMessage(1, new byte[0]), getValidMessageBytes(1, "".getBytes()));

        // Data block with data
        validParseData.put(new DataMessage(1, messageData), getValidMessageBytes(1, messageData));

        // Max Block Size
        validParseData.put(new DataMessage(0xFFFF, messageData), getValidMessageBytes(0xFFFF, messageData));

        // Half Block size (test lower byte of short)
        validParseData.put(new DataMessage(0x00FF, messageData), getValidMessageBytes(0x00FF, messageData));

        // Test random block number other than 1
        validParseData.put(new DataMessage(5, messageData), getValidMessageBytes(5, messageData));

        invalidParseData.add(new Pair<>("Packet length too short", new byte[0]));
        invalidParseData.add(new Pair<>("Packet length too short", new byte[]{ 0, 3 }));
        invalidParseData.add(new Pair<>("Packet length too short", new byte[]{ 0, 3, 0}));
        invalidParseData.add(new Pair<>("The block number can not be less than 1", new byte[]{ 0, 3, 0, 0}));
        invalidParseData.add(new Pair<>("Invalid start byte. Expected 0. Actual: 1", new byte[] { 1, 3, 0, 5, 84}));
        invalidParseData.add(new Pair<>("Invalid message type. Must be DATA (3). Actual: ACK", new byte[] { 0, 4, 0, 5, 84}));

        // Generate a large amount of data > MAX BLOCK SIZE
        byte[] largeData = new byte[DataMessage.MAX_BLOCK_SIZE + 1];
        (new Random()).nextBytes(largeData);


        // Create large msg packet
        byte[] msgPacket = new byte[largeData.length + 4];
        msgPacket[0] = 0;
        msgPacket[1] = 3;
        msgPacket[2] = 0;
        msgPacket[3] = 1;

        // Append large data into msg packet
        System.arraycopy(largeData, 0, msgPacket, 4, largeData.length);

        invalidParseData.add(new Pair<>("The data length can not be greater than 512", msgPacket));
    }

    /**
     * Test Helper. Creates a proper ErrorMessage byte array
     * @param blockNum Block number
     * @param data Message data
     * @return correct message byte array
     */
    private byte[] getValidMessageBytes(int blockNum, byte[] data) throws IOException
    {
        ByteArrayOutputStream bAOS = new ByteArrayOutputStream();
        bAOS.write(0);
        bAOS.write(Message.MessageType.DATA.getType());
        bAOS.write(Message.shortToByteArray((short)blockNum));
        bAOS.write(data);
        return bAOS.toByteArray();
    }

    /**
     * Ensure that the block number was parsed correctly
     */
    @Test
    public void testGetBlock() {
        assertEquals(blockNum, this.validMessage.getBlockNum());
    }


    /**
     * Ensure that the data contained within the message was parsed correctly, the contents are the same
     * BUT that it does NOT refer to the same object reference.
     */
    @Test
    public void testGetData()
    {
        // Assert that array content is equivalent
        assertTrue(Arrays.equals(messageData, validMessage.getData()));

        // Assert that the references are NOT equivalent. We want to make a copy, not return
        // the actual reference. This is to ensure any modifications don't make it to the actual data
        // contained in the message object.
        assertNotEquals(messageData, validMessage.getData());
    }

    /**
     * Test Maximum buffer size. Ensure bigger data is truncated
     */
    @Test
    public void testMaxBufferSize()
    {
        DataMessage dataMessage = new DataMessage(blockNum, new byte[DataMessage.MAX_BLOCK_SIZE + 15]);
        assertEquals(DataMessage.MAX_BLOCK_SIZE, dataMessage.getData().length);
    }

    /**
     * Test Message Type
     */
    @Test
    public void testMessageType()
    {
        assertEquals(validMessage.getMessageType(), Message.MessageType.DATA);
    }


    /**
     * Ensure valid raw byte array data gets parsed properly
     */
    @Test
    public void testParseDataMessage() throws IOException, InvalidPacketException
    {
        MessageTestSuite.testValidParseData(DataMessage::parseMessage, validParseData);
        MessageTestSuite.testValidParseData(data -> DataMessage.parseMessage(new DatagramPacket(data, data.length)), validParseData);
    }

    /**
     * Ensure invalid raw byte array data throws InvalidPacketException
     */
    @Test
    public void testIncorrectParseData()
    {
        MessageTestSuite.testInvalidParseData(DataMessage::parseMessage, invalidParseData);
        MessageTestSuite.testInvalidParseData(data -> DataMessage.parseMessage(new DatagramPacket(data, data.length)), invalidParseData);
    }

}
