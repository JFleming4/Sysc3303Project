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

import static org.junit.Assert.assertEquals;
import static formats.ErrorMessage.ErrorType.*;

public class ErrorMessageTest {

    // Valid Message Constants
    private static final ErrorMessage.ErrorType VALID_MESSAGE_ERROR_TYPE = ACCESS_VIOLATION;
    private static final String VALID_MESSAGE_STRING = "Valid";
    private ErrorMessage validMessage;
    private Map<ErrorMessage, byte[]> validParseData;
    private List<Pair<String, byte[]>> invalidParseData;

    /**
     * Pre-test Setup (valid messages, parse data, etc)
     */
    @Before
    public void setUp() throws IOException
    {
        validMessage = new ErrorMessage(VALID_MESSAGE_ERROR_TYPE, VALID_MESSAGE_STRING);
        validParseData = new HashMap<>();
        invalidParseData = new ArrayList<>();

        // Add valid parse data (empty error message)
        validParseData.put(new ErrorMessage(ACCESS_VIOLATION, ""), getValidMessageBytes(ACCESS_VIOLATION, ""));

        // Put all error types into valid parse data
        for(ErrorMessage.ErrorType type : ErrorMessage.ErrorType.values())
        {
            validParseData.put(new ErrorMessage(type, VALID_MESSAGE_STRING), getValidMessageBytes(type, VALID_MESSAGE_STRING));
        }

        /* Invalid Data byte arrays */
        invalidParseData.add(new Pair<>("Empty Packet", new byte[0]));
        invalidParseData.add(new Pair<>("Packet Too Small / Missing Error Number", new byte[]{ 0, 5 }));
        invalidParseData.add(new Pair<>("Packet Too Small / Missing One Byte of Error Number", new byte[]{ 0, 5, 0}));
        invalidParseData.add(new Pair<>("Invalid Error number", new byte[]{ 0, 5, 0, 8, 0}));
        invalidParseData.add(new Pair<>("Incorrect Start Byte", new byte[] { 1, 5, 0, 0, 0 }));
        invalidParseData.add(new Pair<>("Missing Message Null-Terminator", new byte[] { 0, 5, 0, 2, 'a', 'b', 'c'}));

    }

    /**
     * Test Helper. Creates a proper ErrorMessage byte array
     * @param type The ErrorType
     * @param message The error message
     * @return valid byte array of message data
     */
    private byte[] getValidMessageBytes(ErrorMessage.ErrorType type, String message) throws IOException
    {
        ByteArrayOutputStream bAOS = new ByteArrayOutputStream();
        bAOS.write(0);
        bAOS.write(Message.MessageType.ERROR.getType());
        bAOS.write(Message.shortToByteArray(type.getCode()));
        bAOS.write(message.getBytes());
        bAOS.write(0);
        return bAOS.toByteArray();
    }

    /**
     * Tests constructor with various error types
     */
    @Test
    public void testConstructor()
    {
        for(ErrorMessage.ErrorType type : ErrorMessage.ErrorType.values())
        {
            ErrorMessage err = new ErrorMessage(type, type.name());
            assertEquals(type, err.getErrorType());
            assertEquals(type.name(), err.getMessage());
        }
    }

    /**
     * Tests error message getter
     */
    @Test
    public void testGetErrorMessage()
    {
        assertEquals(VALID_MESSAGE_STRING, validMessage.getMessage());
    }

    /**
     * Tests error type getter
     */
    @Test
    public void testGetErrorType()
    {
        assertEquals(VALID_MESSAGE_ERROR_TYPE, validMessage.getErrorType());
    }

    /**
     * Tests getMessageType()
     */
    @Test
    public void testMessageType()
    {
        assertEquals(validMessage.getMessageType(), Message.MessageType.ERROR);
    }

    /**
     * Tests parsing valid data
     */
    @Test
    public void testParseValidData() throws IOException, InvalidPacketException
    {
        MessageTestSuite.testValidParseData(ErrorMessage::parseMessageFromBytes, validParseData);
        MessageTestSuite.testValidParseData(data -> ErrorMessage.parseMessageFromPacket(new DatagramPacket(data, data.length)), validParseData);
    }

    /**
     * Tests error type getter
     */
    @Test
    public void testParseInvalidData() throws InvalidPacketException
    {
        MessageTestSuite.testInvalidParseData(ErrorMessage::parseMessageFromBytes, invalidParseData);
        MessageTestSuite.testInvalidParseData(data -> ErrorMessage.parseMessageFromPacket(new DatagramPacket(data, data.length)), invalidParseData);
    }
}
