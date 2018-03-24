package session;

import exceptions.SessionException;
import formats.ErrorMessage;
import resources.ResourceManager;
import socket.TFTPDatagramSocket;

import java.io.IOException;


public interface ISessionHandler {
    /**
     * @return The ResourceManager to use for the TFTP Session
     */
    ResourceManager getSessionResourceManager();

    /**
     * @return The TFTPDatagramSocket to use for the TFTP Session
     */
    TFTPDatagramSocket getSessionTFTPSocket();

    /**
     * The Callback that gets called when an error occurs with the session.
     * To Stop the session, throw a SessionException, otherwise, returning from this function
     * will continue the session.
     *
     * @param session The TFTPSession where the error occurred.
     * @param message The ERROR message representing the error that occurred.
     * @throws SessionException When the session should be stopped
     */
    void sessionErrorOccurred(TFTPSession session, ErrorMessage message) throws IOException, SessionException;

    /**
     * The Callback that gets called when an error is received from the destination by the session.
     * After this method is called, the session will stop.
     *
     * @param session The TFTPSession where the error was received.
     * @param message The ERROR message representing the error that was received by the destination.
     */
    void sessionErrorReceived(TFTPSession session, ErrorMessage message);

    /**
     * The Callback that gets called when a session completes
     *
     * @param session The TFTPSession where the error was received.
     */
    void sessionCompleted(TFTPSession session);
}
