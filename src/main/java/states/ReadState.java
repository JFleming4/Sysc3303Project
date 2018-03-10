package states;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;

import exceptions.SessionException;
import formats.*;
import formats.Message.MessageType;
import logging.Logger;
import resources.ResourceFile;
import resources.ResourceManager;
import session.ISessionHandler;
import session.ReceiveSession;
import session.TFTPSession;
import socket.TFTPDatagramSocket;

import static resources.Configuration.GLOBAL_CONFIG;

public class ReadState extends State implements ISessionHandler {

    private TFTPDatagramSocket socket;
    private ResourceManager resourceManager;
    private SocketAddress serverAddress;
    private String filename;

    private static final Logger LOG = new Logger("FTPClient - Read");

    public ReadState(SocketAddress serverAddress, ResourceManager resourceManager, String filename, boolean isVerbose) throws IOException {
        this(serverAddress, resourceManager, filename, isVerbose, new TFTPDatagramSocket());
    }


    public ReadState(SocketAddress serverAddress, ResourceManager resourceManager, String filename, boolean isVerbose, TFTPDatagramSocket socket) throws SocketException {
        this.serverAddress = serverAddress;
        this.filename = filename;

        this.socket = socket;
        this.socket.setSoTimeout(GLOBAL_CONFIG.SOCKET_TIMEOUT_MS);
        this.resourceManager = resourceManager;

        if (isVerbose)
            Logger.setLogLevel(Logger.LogLevel.VERBOSE);
        else
            Logger.setLogLevel(Logger.LogLevel.QUIET);
    }

    @Override
    public State execute() {

        // Create the request message
        RequestMessage initialReq = new RequestMessage(MessageType.RRQ, filename);

        // Create and run session
        ReceiveSession rSession = new ReceiveSession(this, initialReq, serverAddress);
        LOG.logQuiet("Session Success: " + rSession.getSessionSuccess());

        socket.close();
        return new InputState();
    }

    @Override
    public ResourceManager getSessionResourceManager() {
        return this.resourceManager;
    }

    @Override
    public TFTPDatagramSocket getSessionTFTPSocket() {
        return this.socket;
    }

    /**
     * Handles a Session Error.
     * We will create the resource file if the file was not found.
     * We will stop the session if the file already exists.
     * We will send the ERROR message AND stop the session if any other error occurs
     *
     * @param session The TFTPSession where the error occurred.
     * @param message The ERROR message representing the error that occurred.
     * @throws IOException
     * @throws SessionException
     */
    @Override
    public void sessionErrorOccurred(TFTPSession session, ErrorMessage message) throws IOException, SessionException {

        switch (message.getErrorType())
        {
            case FILE_NOT_FOUND:
                // This should not raise an error. It is correct behaviour.
                // Create the file.
                ResourceFile file = session.getResourceFile();
                LOG.logVerbose("Creating File: " + file.getAbsolutePath());
                file.createNewFile();
                break;
            case FILE_EXISTS:
                // If the file already exists, stop the session but do not send an error
                LOG.logQuiet("The file " + session.getSessionRequest().getFileName() + " already exists and will not be overwritten. No session will be started with the server.");
                throw new SessionException();
            default:
                LOG.logVerbose("Error Occurred: " + message.getMessage());

                // Delete file on Error
                session.getResourceFile().delete();

                // All other errors will be raised.
                session.raiseError(message);
                break;
        }
    }

    /**
     * We will just print out the error that was received. No additional action is needed.
     * This is to let us know that the session will finish due to an error.
     *
     * @param session The TFTPSession where the error was received.
     * @param message The ERROR message representing the error that was received by the destination.
     */
    @Override
    public void sessionErrorReceived(TFTPSession session, ErrorMessage message) {
        LOG.logQuiet("The following ERROR was received from the server: " + message.getMessage());
        LOG.logVerbose(message);

        LOG.logQuiet("Failed to receive correct file.");

        // Delete file on Error
        session.getResourceFile().delete();
    }
}
