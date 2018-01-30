package exceptions;
/**
 * Exception thrown when command syntax is invalid
 */
public class InvalidCommandException extends Exception {
	public InvalidCommandException(String message) {
		super(message);
	}

}
