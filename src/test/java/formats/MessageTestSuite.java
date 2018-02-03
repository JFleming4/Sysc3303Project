package formats;

import exceptions.InvalidPacketException;
import javafx.util.Pair;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Responsible for running all Message object tests.
 * Also provides some useful utilities for Message testing.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        MessageTest.class,
        AckMessageTest.class,
        DataMessageTest.class,
        ErrorMessageTest.class,
        RequestMessageTest.class})
public class MessageTestSuite {


    /**
     * Tests invalid parsing data. Expects to throw an exception when parsing
     * @param executor The implementation expected to parse the data
     * @param invalidParseData The Listing of test inputs and their corresponding failure messages (if the test fails)
     */
    static void testInvalidParseData(ParseExecute executor, List<Pair<String, byte[]>> invalidParseData)
    {
        System.out.println("Expecting " + invalidParseData.size() + " exceptions.");
        for(Pair<String, byte[]> data : invalidParseData)
        {
            // Expect exceptions to be thrown
            try {
                // Parse the data using interface implemented by individual tests
                executor.parse(data.getValue());
                fail("Expected InvalidPacketException: " + data.getKey());
            }catch (InvalidPacketException iPE)
            {
                System.out.println("Expected Exception: " + iPE.getLocalizedMessage());
            }
        }

        // Blank Line for spacing after output
        System.out.println("");
    }

    /**
     * Ensure valid raw byte array data gets parsed properly
     */
    static void testValidParseData(ParseExecute executor, Map<?,?> validParseData) throws IOException, InvalidPacketException
    {
        for(Map.Entry<?,?> entries : validParseData.entrySet())
        {
            // Ensure we can cast the Map entries as needed
            if(!(entries.getValue() instanceof byte[]) || !(entries.getKey() instanceof Message))
                return;

            Message expectedMessage = (Message) entries.getKey();
            byte[] parseData = (byte[]) entries.getValue();

            Message parsed = executor.parse(parseData);
            assertEquals("Parsed Message must be equivalent to expected message", expectedMessage, parsed);
            assertTrue("Checking toByteArray() matches with valid raw byte array", Arrays.equals(parsed.toByteArray(), parseData));
        }
    }
}

/**
 * This interface is used to abstract the MessageType that is being parsed so that each test class can test its own
 * parsing logic
 */
interface ParseExecute
{
    Message parse(byte[] data) throws InvalidPacketException;
}