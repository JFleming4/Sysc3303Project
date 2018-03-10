package states;
import java.io.IOException;
import java.net.SocketAddress;

import exceptions.SessionException;
import formats.*;
import formats.Message.MessageType;
import formats.RequestMessage;
import logging.Logger;
import resources.ResourceManager;
import session.ISessionHandler;
import session.TFTPSession;
import session.TransmitSession;
import socket.TFTPDatagramSocket;

import static resources.Configuration.GLOBAL_CONFIG;

public class WriteState extends State implements ISessionHandler{
	private TFTPDatagramSocket socket;
	private ResourceManager resourceManager;
	private SocketAddress serverAddress;
	private String filename;

    public WriteState(SocketAddress serverAddress, ResourceManager resourceManager, String filename, boolean isVerbose)throws IOException {
        this(serverAddress, resourceManager, filename, isVerbose, new TFTPDatagramSocket());
    }


    public WriteState(SocketAddress serverAddress, ResourceManager resourceManager, String filename, boolean isVerbose, TFTPDatagramSocket socket) throws IOException {
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

        // Ensure the selected resource file is valid.
        if(!resourceManager.isValidResource(filename))
        {
            LOG.logVerbose("The file '" + filename + "' is an invalid resource file.");
            return new InputState();
        }

        // Create the request message
        RequestMessage initialReq = new RequestMessage(MessageType.WRQ, filename);

        // Create & Run Transmit Session
        new TransmitSession(this, initialReq, serverAddress);

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

    @Override
    public void sessionErrorOccurred(TFTPSession session, ErrorMessage message) throws IOException, SessionException {
        switch (message.getErrorType()) {
            case FILE_EXISTS:
                // File exists is normal behaviour, since we are writing the file
                break;
            case FILE_NOT_FOUND:
                LOG.logQuiet("The file " + session.getSessionRequest().getFileName() + " does not exist (or is not a file). No session will be started with the server.");
                throw new SessionException();
            default:
                session.raiseError(message);
                break;
        }
    }

    @Override
    public void sessionErrorReceived(TFTPSession session, ErrorMessage message) {

    }
}
