package formats;

import exceptions.InvalidPacketException;
import javafx.util.Pair;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static formats.Message.MessageType.ACK;
import static org.junit.Assert.*;

public class AckMessageTest {

    private static final int BLOCK_NUM = 15;
    private AckMessage validAck;
    private Map<AckMessage, byte[]> validParseData;
    private List<Pair<String, byte[]>> invalidParseData;


    /**
     * Pre-test Setup (valid messages, parse data, etc)
     */
    @Before
    public void setUp() throws IOException
    {
        // Set up valid ACK Message object
        validAck = new AckMessage(BLOCK_NUM);

        validParseData = new HashMap<>();
        invalidParseData = new ArrayList<>();

        // Correct byte arrays that should have no issues
        validParseData.put(new AckMessage(0), getValidMessageBytes((short)0));
        validParseData.put(new AckMessage(1), getValidMessageBytes((short)1));
        validParseData.put(new AckMessage(5), getValidMessageBytes((short)5));
        validParseData.put(new AckMessage(0xFFFF), getValidMessageBytes((short)0xFFFF));
        validParseData.put(new AckMessage(0x00FF), getValidMessageBytes((short)0x00FF));
        validParseData.put(new AckMessage(0xFF00), getValidMessageBytes((short)0xFF00));

        // Invalid raw data
        invalidParseData.add(new Pair<>("Invalid packet length", new byte[0]));
        invalidParseData.add(new Pair<>("Invalid packet length", new byte[] { 0, (byte) ACK.getType()}));
        invalidParseData.add(new Pair<>("Invalid packet length", new byte[] { 0, (byte) ACK.getType(), 0}));
        invalidParseData.add(new Pair<>("Invalid start byte. Expected 0. Actual: 1", new byte[] { 1, (byte) ACK.getType(), 0, 0}));
        invalidParseData.add(new Pair<>("Invalid message type. Must be ACK (4). Actual: null", new byte[] { 0, 0, 0, 0}));

    }

    /**
     * Test Helper. Creates a proper ErrorMessage byte array
     * @param blockNum The block number
     * @return valid byte array of message data
     */
    private byte[] getValidMessageBytes(short blockNum) throws IOException
    {
        ByteArrayOutputStream bAOS = new ByteArrayOutputStream();
        bAOS.write(0);
        bAOS.write(ACK.getType());
        bAOS.write(Message.shortToByteArray(blockNum));
        return bAOS.toByteArray();
    }

    @Test
    public void testConstructor()
    {
        assertEquals(validAck.getBlockNum(), BLOCK_NUM);
    }

    /**
     * Ensure that the toByteArray returns a byte array that should be parsed
     * correctly
     * @throws InvalidPacketException
     * @throws IOException
     */
    @Test
    public void testToByteArray() throws InvalidPacketException, IOException
    {
        AckMessage parse = AckMessage.parseMessage(validAck.toByteArray());
        assertEquals(validAck, parse);
    }

    /**
     * Test blockNum getter
     */
    @Test
    public void testGetBlockNum()
    {
        assertEquals(BLOCK_NUM, validAck.getBlockNum());
    }

    /**
     * Ensure valid raw byte array data gets parsed properly
     */
    @Test
    public void testParseDataMessage() throws IOException, InvalidPacketException
    {
        MessageTestSuite.testValidParseData(AckMessage::parseMessage, validParseData);
        MessageTestSuite.testValidParseData(data -> AckMessage.parseMessage(new DatagramPacket(data, data.length)), validParseData);
    }

    /**
     * Ensure invalid raw byte array data throws InvalidPacketException
     */
    @Test
    public void testIncorrectParseData()
    {
        MessageTestSuite.testInvalidParseData(AckMessage::parseMessage, invalidParseData);
        MessageTestSuite.testInvalidParseData(data -> AckMessage.parseMessage(new DatagramPacket(data, data.length)), invalidParseData);
    }
}