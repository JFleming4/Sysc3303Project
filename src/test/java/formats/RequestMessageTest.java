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
    private RequestMessage.MessageMode validMode;
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
        this.validMode = RequestMessage.MessageMode.NET_ASCII;

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
            validParseData.put(new RequestMessage(type, "", RequestMessage.MessageMode.NET_ASCII), getValidMessageBytes(type, "", RequestMessage.MessageMode.NET_ASCII.getModeName()));
            validParseData.put(new RequestMessage(type, "", RequestMessage.MessageMode.OCTET), getValidMessageBytes(type, "", RequestMessage.MessageMode.OCTET.getModeName()));
            validParseData.put(new RequestMessage(type, "", RequestMessage.MessageMode.MAIL), getValidMessageBytes(type, "", RequestMessage.MessageMode.MAIL.getModeName()));

            // Valid File names
            validParseData.put(new RequestMessage(type, validFileName, RequestMessage.MessageMode.NET_ASCII), getValidMessageBytes(type, validFileName, RequestMessage.MessageMode.NET_ASCII.getModeName()));
            validParseData.put(new RequestMessage(type, validFileName, RequestMessage.MessageMode.OCTET), getValidMessageBytes(type, validFileName, RequestMessage.MessageMode.OCTET.getModeName()));
            validParseData.put(new RequestMessage(type, validFileName, RequestMessage.MessageMode.MAIL), getValidMessageBytes(type, validFileName, RequestMessage.MessageMode.MAIL.getModeName()));
        }

        // Set up invalid parsing data
        invalidParseData = new ArrayList<>();

        // Packet empty
        invalidParseData.add(new Pair<>("Packet length too short", new byte[0]));
        invalidParseData.add(new Pair<>("Packet length too short", new byte[]{ 0, 1 }));
        invalidParseData.add(new Pair<>("Packet length too short", new byte[]{ 0, 2 }));
        invalidParseData.add(new Pair<>("Packet length too short", new byte[]{ 0, 1, 0 }));
        invalidParseData.add(new Pair<>("Packet length too short", new byte[]{ 0, 2, 0 }));
        invalidParseData.add(new Pair<>("Invalid message type. Must be RRQ or WRQ. Actual: DATA", new byte[]{ 0, 3, 0, 0 }));
        invalidParseData.add(new Pair<>("Invalid start byte. Expected 0. Actual: 1", new byte[]{ 1, 1, 0, 0 }));
        invalidParseData.add(new Pair<>("Request Mode invalid_mode is not a valid mode", getValidMessageBytes(RRQ, validFileName, "invalid_mode")));
        invalidParseData.add(new Pair<>("Request Mode invalid_mode is not a valid mode", getValidMessageBytes(WRQ, validFileName, "invalid_mode")));

        // Create RRQ too long
        byte[] validRRQ = getValidMessageBytes(RRQ, "", RequestMessage.MessageMode.NET_ASCII.getModeName());
        byte[] rrqTooLongPacket = new byte[validRRQ.length + 1];
        System.arraycopy(validRRQ, 0, rrqTooLongPacket, 0, validRRQ.length);

        // Create WRQ too long
        byte[] validWRQ = getValidMessageBytes(WRQ, "", RequestMessage.MessageMode.NET_ASCII.getModeName());
        byte[] wrqTooLongPacket = new byte[validWRQ.length + 1];
        System.arraycopy(validWRQ, 0, wrqTooLongPacket, 0, validWRQ.length);

        invalidParseData.add(new Pair<>("Packet length is too long. There should be no data after the 0 following the mode. Number of extra bytes: 1", rrqTooLongPacket));
        invalidParseData.add(new Pair<>("Packet length is too long. There should be no data after the 0 following the mode. Number of extra bytes: 1", wrqTooLongPacket));
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
        MessageTestSuite.testValidParseData(RequestMessage::parseMessage, validParseData);
        MessageTestSuite.testValidParseData(data -> RequestMessage.parseMessage(new DatagramPacket(data, data.length)), validParseData);
    }

    /**
     * Ensure invalid raw byte array data throws InvalidPacketException
     */
    @Test
    public void testIncorrectParseData()
    {
        MessageTestSuite.testInvalidParseData(RequestMessage::parseMessage, invalidParseData);
        MessageTestSuite.testInvalidParseData(data -> RequestMessage.parseMessage(new DatagramPacket(data, data.length)), invalidParseData);
    }

}
