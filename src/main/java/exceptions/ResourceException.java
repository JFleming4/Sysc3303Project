package exceptions;

import java.io.IOException;

/**
 * Represents any exceptions necessary when obtaining resources
 */
public class ResourceException extends IOException {
    public ResourceException(String message) {
        super(message);
    }
}
