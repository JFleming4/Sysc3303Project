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
import static formats.Message.MessageType.*;
import static formats.Message.MessageType;

public class RequestMessageTest {

    private RequestMessage validRRQMessage;
    private RequestMessage validWRQMessage;
    private String validFileName;
    private String validMode;
    private Map<RequestMessage, byte[]> validParseData;
    private List<Pair<String, byte[]>> invalidParseData;
    private List<MessageType> requestTypes;

    /**
     * Pre-test Setup (valid messages, parse data, etc)
     */
    @Before
    public void setUp() throws IOException
    {
        this.validFileName = "test.txt";
        this.validMode = "NetAscii";

        this.validRRQMessage = new RequestMessage(RRQ, validFileName, validMode);
        this.validWRQMessage = new RequestMessage(WRQ, validFileName, validMode);

        // Set up valid parsing data
        validParseData = new HashMap<>();
        requestTypes = new ArrayList<>();
        requestTypes.add(WRQ);
        requestTypes.add(RRQ);


        // For each valid request type, add tests for valid data
        for(MessageType type : requestTypes)
        {
            // Empty file name and mode
            validParseData.put(new RequestMessage(type, "", ""), getValidMessageBytes(type, "", ""));
            validParseData.put(new RequestMessage(type, validFileName, validMode), getValidMessageBytes(type, validFileName, validMode));
        }

        // Set up invalid parsing data
        invalidParseData = new ArrayList<>();

        // Packet empty
        invalidParseData.add(new Pair<>("Empty Packet", new byte[0]));
        invalidParseData.add(new Pair<>("RRQ: Packet Too Small / Missing FileName & Mode", new byte[]{ 0, 1 }));
        invalidParseData.add(new Pair<>("WRQ: Packet Too Small / Missing FileName & Mode", new byte[]{ 0, 2 }));
        invalidParseData.add(new Pair<>("RRQ: Packet Too Small / Missing Mode", new byte[]{ 0, 1, 0 }));
        invalidParseData.add(new Pair<>("WRQ: Packet Too Small / Missing Mode", new byte[]{ 0, 2, 0 }));
        invalidParseData.add(new Pair<>("Invalid Message Type", new byte[]{ 0, 3, 0, 0 }));
        invalidParseData.add(new Pair<>("Invalid Start Byte", new byte[]{ 1, 1, 0, 0 }));
        invalidParseData.add(new Pair<>("RRQ: Packet too large", new byte[]{ 0, 1, 0, 0, 0 }));
        invalidParseData.add(new Pair<>("WRQ: Packet too large", new byte[]{ 0, 1, 0, 0, 0 }));
    }

    /**
     * Test Helper. Creates a proper ErrorMessage byte array
     * @param type WRQ or RRQ
     * @param fileName file name
     * @param mode  The mode
     * @return correct message byte array
     */
    private byte[] getValidMessageBytes(MessageType type, String fileName, String mode) throws IOException
    {
        if(!requestTypes.contains(type))
            throw new IOException("Incorrect Message Type");

        ByteArrayOutputStream bAOS = new ByteArrayOutputStream();
        bAOS.write(0);
        bAOS.write(type.getType());
        bAOS.write(fileName.getBytes());
        bAOS.write(0);
        bAOS.write(mode.getBytes());
        bAOS.write(0);
        return bAOS.toByteArray();
    }

    /**
     * Ensure valid message types
     */
    @Test
    public void testMessageType()
    {
        assertEquals(WRQ, validWRQMessage.getMessageType());
        assertEquals(RRQ, validRRQMessage.getMessageType());
    }

    /**
     * Ensure fileName and mode are properly retrieved through getters
     */
    @Test
    public void testFileNameAndModeGetter()
    {
        assertEquals(validFileName, validRRQMessage.getFileName());
        assertEquals(validFileName, validWRQMessage.getFileName());
        assertEquals(validMode, validRRQMessage.getMode());
        assertEquals(validMode, validWRQMessage.getMode());

    }

    /**
     * Ensure valid raw byte array data gets parsed properly
     */
    @Test
    public void testParseDataMessage() throws IOException, InvalidPacketException
    {
        MessageTestSuite.testValidParseData(RequestMessage::parseMessageFromBytes, validParseData);
        MessageTestSuite.testValidParseData(data -> RequestMessage.parseMessageFromPacket(new DatagramPacket(data, data.length)), validParseData);
    }

    /**
     * Ensure invalid raw byte array data throws InvalidPacketException
     */
    @Test
    public void testIncorrectParseData()
    {
        MessageTestSuite.testInvalidParseData(RequestMessage::parseMessageFromBytes, invalidParseData);
        MessageTestSuite.testInvalidParseData(data -> RequestMessage.parseMessageFromPacket(new DatagramPacket(data, data.length)), invalidParseData);
    }

}
