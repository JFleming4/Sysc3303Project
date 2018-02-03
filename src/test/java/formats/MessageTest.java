package formats;

import javafx.util.Pair;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class MessageTest {

    private Map<String, byte[]> validStrings;
    private List<Pair<String, byte[]>> invalidByteStrings;
    private Random random;

    /**
     * Pre-test Setup (valid messages, parse data, etc)
     */
    @Before
    public void setUp()
    {
        // Valid String + correct byte array mappings
        this.validStrings = new HashMap<>();
        this.validStrings.put("", "\0".getBytes());
        this.validStrings.put("Test", "Test\0".getBytes());
        this.validStrings.put("ValidString", "ValidString\0".getBytes());

        // Random generator for offsets
        this.random = new Random();

        // Invalid data to test
        this.invalidByteStrings = new ArrayList<>();
        this.invalidByteStrings.add(new Pair<>("Missing Null-Terminator", "Test".getBytes()));
        this.invalidByteStrings.add(new Pair<>("Random Non-Terminated Data", new byte[]{5, 5, 51}));
    }

    /**
     * Tests the utility for reading a String from a byte array ending with a null-terminator
     */
    @Test
    public void testReadStringInvalidData()
    {
        for(Pair<String, byte[]> arr : this.invalidByteStrings)
        {
            String res = Message.readStringFromBytes(arr.getValue(), 0);
            assertEquals(arr.getKey(),"", res);
        }
    }

    /**
     * Tests the utility for converting a short to a byte array
     */
    @Test
    public void testShortToByteArray()
    {
        // Max short value
        byte[] bytes = Message.shortToByteArray((short)0xFFFF);
        assertEquals(2, bytes.length);
        assertEquals((byte)0xFF, bytes[0]);
        assertEquals((byte)0xFF, bytes[1]);


        // Upper byte Short Value
        bytes = Message.shortToByteArray((short)0x00FF);
        assertEquals(2, bytes.length);
        assertEquals((byte)0x00, bytes[0]);
        assertEquals((byte)0xFF, bytes[1]);

        // Lower byte Short Value
        bytes = Message.shortToByteArray((short)0xFF00);
        assertEquals(2, bytes.length);
        assertEquals((byte)0xFF, bytes[0]);
        assertEquals((byte)0x00, bytes[1]);

        // Zero Short Value
        bytes = Message.shortToByteArray((short)0x0000);
        assertEquals(2, bytes.length);
        assertEquals((byte)0x00, bytes[0]);
        assertEquals((byte)0x00, bytes[1]);

        // Random short value
        bytes = Message.shortToByteArray((short)0x0102);
        assertEquals(2, bytes.length);
        assertEquals((byte)0x01, bytes[0]);
        assertEquals((byte)0x02, bytes[1]);
    }

    /**
     * Tests the utility for converting a byte array to an unsigned short (aka int with upper 16 bits = 0)
     */
    @Test
    public void testByteArrayToShort()
    {
        // Check to ensure that only lower 16 bits are set and that the value is what it should be
        int result = Message.byteArrayToUnsignedShort(new byte[] {(byte)0xFF, (byte)0xFF}, 0);

        // Check result value
        assertEquals(0x0000FFFF, result);

        // This is only true if the data is being represented as unsigned
        assertTrue(result > 0);

        // Assert Lower byte
        result = Message.byteArrayToUnsignedShort(new byte[] {(byte)0x00, (byte)0xFF}, 0);
        assertEquals(0x000000FF, result);

        // Assert Upper byte
        result = Message.byteArrayToUnsignedShort(new byte[] {(byte)0xFF, (byte)0x00}, 0);
        assertEquals(0x0000FF00, result);

        // Assert Lower and upper bytes
        result = Message.byteArrayToUnsignedShort(new byte[] {(byte)0x01, (byte)0x01}, 0);
        assertEquals(0x00000101, result);


        // Check Offsets
        result = Message.byteArrayToUnsignedShort(new byte[] {41, (byte)0xFF, (byte)0xFF}, 1);
        assertEquals(0x0000FFFF, result);

        result = Message.byteArrayToUnsignedShort(new byte[] {31, 12, (byte)0xFF, (byte)0xFF}, 2);
        assertEquals(0x0000FFFF, result);

    }


    /**
     * Tests the utility for reading a string from a byte array
     */
    @Test
    public void testReadStringFromBytes() throws IOException
    {
        for(Map.Entry<String, byte[]> valid : this.validStrings.entrySet())
        {
            // Test byte array to string
            String actual = Message.readStringFromBytes(valid.getValue(), 0);
            assertEquals(valid.getKey(), actual);

            // Generate and append random data for offset
            ByteArrayOutputStream bAOS = new ByteArrayOutputStream();

            /* Now Test Random Offsets */

            // Append random number of bytes for offset
            int offset = random.nextInt(5) + 1;
            for(int i = 0; i < offset; i++)
                bAOS.write('_');

            // Write original data
            bAOS.write(valid.getValue());

            // Test offset
            actual = Message.readStringFromBytes(bAOS.toByteArray(), offset);
            assertEquals(valid.getKey(), actual);
        }
    }

}