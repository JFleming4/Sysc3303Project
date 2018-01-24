package exceptions;

/**
 * This exception is thrown when the server receives bad data
 */
public class InvalidPacketException extends Exception {

    public InvalidPacketException(String message)
    {
        super(message);
    }
}
