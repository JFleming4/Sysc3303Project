package formats;

import exceptions.InvalidPacketException;
import logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;

/**
 * Representation of a TFTP Error Message
 */
public class ErrorMessage extends Message {
	private static final Logger LOG = new Logger("ErrorPacket");
    /**
     * Error code definitions following TFTP protocol
     */
    public enum ErrorType
    {
        NOT_DEFINED(0),
        FILE_NOT_FOUND(1),
        ACCESS_VIOLATION(2),
        DISK_FULL(3),
        ILLEGAL_OPERATION(4),
        UNKNOWN_TRANSFER_ID(5),
        FILE_EXISTS(6),
        NO_SUCH_USER(7);

        private short code;
        ErrorType(int code)
        {
            this.code = (short)code;
        }
        public short getCode()
        {
            return this.code;
        }

        /**
         * Gets the enum value based on an integer type
         * @param code The type integer
         * @return The enumeration instance associated with the integer type
         */
        public static ErrorType getErrorType (int code)
        {
            for (ErrorType t: ErrorType.values())
            {
                if(code == t.getCode())
                    return t;
            }
            return null;
        }

    }

    private ErrorType type;
    private String message;

    public ErrorMessage(ErrorType type, String message)
    {
        this.type = type;
        this.message = message;
    }

    public ErrorType getErrorType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    @Override
    protected byte[] getBytes() throws IOException {
        ByteArrayOutputStream bAOS = new ByteArrayOutputStream();

        // Write two-byte big endian format of errorCode
        short errorCode = type.getCode();
        bAOS.write(Message.shortToByteArray(errorCode));

        bAOS.write(this.message.getBytes());
        bAOS.write(0);

        return bAOS.toByteArray();
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.ERROR;
    }

    /**
     * Check if two ErrorMessage objects are equal to each other
     * @param other The other ErrorMessage
     * @return True if the objects are equals
     */
    @Override
    public boolean equals(Object other)
    {
        if (this == other)
            return true;

        if(!(other instanceof ErrorMessage))
            return false;

        ErrorMessage otherMsg = (ErrorMessage) other;
        return this.getMessageType().equals(otherMsg.getMessageType())
                && this.type.equals(otherMsg.type)
                && this.message.equals(otherMsg.message);
    }
    
    /**
     * Check if a given packet is an ErrorMessage
     * @param packet
     * @return  true if it is an ErrorMessage false otherwise
     */
    public static boolean isErrorMessage(DatagramPacket packet) {
    	try {
    		parseMessage(packet);
    		return true;
    	} catch(InvalidPacketException ipe) {
    		return false;
    	}
    }

    /**
     * Creates an ErrorMessage object from a packet object
     * @param packet The packet object containing the data to be parsed
     * @return The ErrorMessage object containing all relevant info
     * @throws InvalidPacketException If there was an error parsing the data
     */
    public static ErrorMessage parseMessage(DatagramPacket packet) throws InvalidPacketException {
        return parseMessage(Arrays.copyOf(packet.getData(), packet.getLength()));
    }

    /**
     * Creates an ErrorMessage object from a byte array
     * @param data The Data retrieved in a packet
     * @return The ErrorMessage object containing all relevant info
     * @throws InvalidPacketException If there was an error parsing the data
     */
    public static ErrorMessage parseMessage(byte[] data) throws InvalidPacketException {
        // Error message has a minimum size of 5
        if (data.length < 5)
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
        if (!MessageType.ERROR.equals(type))
            throw new InvalidPacketException("Invalid message type. Must be ERROR (" + MessageType.ERROR.getType() + "). Actual: " + type);

        // Read 2-byte error code
        short errorCode = (short) Message.byteArrayToUnsignedShort(data, ptr);
        ptr += 2;

        // Parse the error type
        ErrorType errType = ErrorType.getErrorType(errorCode);

        // Check if the type is valid
        if(errType == null)
            throw new InvalidPacketException("Invalid error code. Error Code: " + errorCode);

        // Read error message from bytes
        String errorMessage = readStringFromBytes(data, ptr);
        ptr += errorMessage.length() + 1;

        // Check if we are not at the end of the packet (packet is too large)
        if (ptr != data.length)
            throw new InvalidPacketException("End of packet expected. Packet is too large.");

        return new ErrorMessage(errType, errorMessage);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Error Code: ");
        builder.append(getErrorType());
        builder.append(" (");
        builder.append(getErrorType().getCode());
        builder.append(")");
        builder.append(System.lineSeparator());

        builder.append("Error Message: ");
        builder.append(getMessage());
        builder.append(System.lineSeparator());

        return super.toString() + builder.toString();
    }
}
