package formats;

import logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public abstract class Message {
    private final static Logger LOG = new Logger("Message");
	public final static int MAX_PACKET_SIZE = 516;

    /**
     * Enumeration of TFTP message formats
     */
    public enum MessageType {
        RRQ(1),
        WRQ(2),
        DATA(3),
        ACK(4),
        ERROR(5);

        private int type;
        MessageType(int type)
        {
            this.type = type;
        }

        public int getType() {
            return type;
        }

        /**
         * Gets the enum value based on an integer type
         * @param type The type integer
         * @return The enumeration instance associated with the integer type
         */
        public static MessageType getMessageType (int type)
        {
            for (MessageType t: MessageType.values())
            {
                if(type == t.getType())
                    return t;
            }

            return null;
        }

        /**
         * @param type The request type integer
         * @return True if the type integer corresponds to a valid formats.RequestType enumeration
         */
        public static boolean isValidMessageType(int type)
        {
            return getMessageType(type) != null;
        }

        /**
         * Checks to see if a given type is a request message (RRQ or WRQ)
         * @param type The type to check
         * @return True if the type is a request
         */
        public static boolean isRequestType(MessageType type)
        {
            return RRQ.equals(type) || WRQ.equals(type);
        }
    }

    /**
     * Returns a byte array that conforms to the TFTP protocol for the specified message type.
     * This byte array is ready to be sent as a packet.
     * @throws IOException
     */
    public byte[] toByteArray() throws IOException
    {
        ByteArrayOutputStream bAOS = new ByteArrayOutputStream();
        bAOS.write(0);
        bAOS.write(getMessageType().getType());
        bAOS.write(getBytes());
        return bAOS.toByteArray();
    }

    protected abstract byte[] getBytes() throws IOException;

    public abstract MessageType getMessageType();

    /**
     * Converts an unsigned short to big endian byte format (follows TFTP protocol)
     * @param val The short value
     * @return A byte array with two values in big endian format.
     */
    static byte[] shortToByteArray(short val)
    {

        byte[] bytes = new byte[2];

        // Write higher byte of val first
        bytes[0] = (byte)(val >> 8);

        // Then write lower byte of val
        bytes[1] = (byte)val;

        return bytes;
    }

    /**
     * Converts two bytes in big endian format into a short (follows TFTP protocol)
     * @param data The data to convert to short
     * @param offset The offset to start at in the data array
     * @return Short value, or -1 if the offset or data is invalid
     */
    static int byteArrayToUnsignedShort(byte[] data, int offset)
    {
        if(data == null || offset > data.length - 2)
            return -1;

        // As crazy as this looks, we need it so that we can maintain an unsigned short using an int
        return ((data[offset] << 8) + (data[offset+1] & 0x000000FF)) & 0x0000FFFF;
    }

    /**
     * Reads a string contained in a packet, ending with a 0
     * @param data The byte array of data to parse
     * @param offset The starting position of the string in the data array
     * @return A string parsed from the data array.
     */
    static String readStringFromBytes(byte[] data, int offset) {
        int ptr = offset;

        StringBuilder stringBuilder = new StringBuilder();

        // Iterate through data until a 0 is reached.
        while (data[ptr] != 0) {

            stringBuilder.append((char) data[ptr]);

            // If the end of the array is reached, and a 0 was not found, we return a blank string
            if (ptr++ == data.length - 1) {
                LOG.logVerbose("0 byte not found");
                return "";
            }
        }

        return stringBuilder.toString();
    }
}
