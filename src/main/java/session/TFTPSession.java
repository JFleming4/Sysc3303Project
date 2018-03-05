package session;

import exceptions.InvalidPacketException;
import exceptions.ResourceException;
import exceptions.SessionException;
import formats.ErrorMessage;
import formats.Message;
import formats.Message.MessageType;
import formats.RequestMessage;
import logging.Logger;
import resources.ResourceFile;
import socket.TFTPDatagramSocket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public abstract class TFTPSession{

    private static final Logger LOG = new Logger("TFTPSession");
    protected ISessionHandler sessionHandler;
    private TFTPDatagramSocket socket;
    private RequestMessage sessionRequest;
    private SocketAddress currentDestAdr;
    private boolean sessionComplete;
    private boolean sessionSuccess;
    private ResourceFile resourceFile;
    private MessageType incomingMessageType;

    /**
     * Initializes a TFTP Session Object with a SessionHandler and the incoming message type.
     * @param sessionHandler The session handler to handle errors etc.
     * @param incomingMessageType The message type to expect when receiving messages.
     */
    protected TFTPSession(ISessionHandler sessionHandler, MessageType incomingMessageType) {
        this.sessionHandler = sessionHandler;
        this.socket = sessionHandler.getSessionTFTPSocket();
        this.sessionComplete = false;
        this.sessionSuccess = false;
        this.incomingMessageType = incomingMessageType;
    }

    /**
     * @return True if the session has completed and was successful. False if the session
     * is not complete OR if the session was unsuccessful.
     */
    public synchronized boolean getSessionSuccess() {
        return sessionComplete && sessionSuccess;
    }

    /**
     * Runs the TFTPSession.
     * @param requestMessage The Initial request message
     * @param destAdr The socket to send the initial request to.
     * @return True if the session ran successfully, False otherwise
     */
    public synchronized boolean runSession(RequestMessage requestMessage, SocketAddress destAdr) {

        // Set current destination
        this.currentDestAdr = destAdr;
        this.sessionRequest = requestMessage;

        try {
            try {
                this.resourceFile = sessionHandler.getSessionResourceManager().getFile(sessionRequest.getFileName());

                LOG.logQuiet("---- Beginning TFTP Session ----");

                // Check if the file already exists. If so, notify handlers. It is up to the handler to determine
                // if the session should be stopped
                if (resourceFile.exists() && resourceFile.isFile())
                    sessionHandler.sessionErrorOccurred(this, new ErrorMessage(ErrorMessage.ErrorType.FILE_EXISTS, "File (" + requestMessage.getFileName() + ") already exists."));
                else
                    sessionHandler.sessionErrorOccurred(this, new ErrorMessage(ErrorMessage.ErrorType.FILE_NOT_FOUND, "File (" + requestMessage.getFileName() + ") does not exist or is not a file."));

                // Run the template method to initialize the session.
                initialize();

                // Run the session
                while (!sessionComplete) {
                    run();
                }

                this.sessionSuccess = true;

            } catch (SocketException | SocketTimeoutException sE) {
                // SocketExceptions should be handled differently from IOExceptions
                // SocketExceptions should NOT send an ErrorMessage (because socket exceptions typically
                // show issues in sending the message packets)
                LOG.logQuiet("Socket Exception occurred: " + sE);
                throw new SessionException();
            }catch (ResourceException rE) {
                // Stop the session without sending an ERROR message to the destination
                LOG.logVerbose("A Resource Exception has Occurred: " + rE);
                throw new SessionException();
            } catch (IOException ioE) {

                if (ioE.getLocalizedMessage().toLowerCase().contains("access is denied") ||
                        ioE.getLocalizedMessage().toLowerCase().contains("permission denied")) {
                    LOG.logQuiet("You do not have permissions to access this file");
                    ErrorMessage errMsg = new ErrorMessage(ErrorMessage.ErrorType.ACCESS_VIOLATION, "You do not have the correct permissions for this file");
                    sessionHandler.sessionErrorOccurred(this, errMsg);
                } else if (ioE.getMessage().toLowerCase().contains("not enough usable space")) {
                    LOG.logQuiet("Not enough usable disk space");
                    ErrorMessage diskFullMessage = new ErrorMessage(ErrorMessage.ErrorType.DISK_FULL, "Not enough free space on disk");
                    sessionHandler.sessionErrorOccurred(this, diskFullMessage);
                }

                // Send ERROR message back (NOT_DEFINED)
                ErrorMessage otherMessage = new ErrorMessage(ErrorMessage.ErrorType.NOT_DEFINED, "An IOException Occurred while receiving the data. Message: " + ioE.getLocalizedMessage());
                LOG.logQuiet("IOException Occurred: " + ioE.getLocalizedMessage());
                raiseError(otherMessage);

            } catch (InvalidPacketException iPE) {

                // Send ERROR message back (ILLEGAL_OPERATION)
                ErrorMessage otherMessage = new ErrorMessage(ErrorMessage.ErrorType.ILLEGAL_OPERATION, "Invalid Message Received. Message: " + iPE.getLocalizedMessage());
                LOG.logQuiet("InvalidPacketException Occurred: " + iPE.getLocalizedMessage());
                raiseError(otherMessage);
            }

        } catch (IOException sE) {
            // Occurs when sending on the socket during a previous exception. Do nothing here other than log.
            LOG.logQuiet("Failed to send ERROR message. IOException: " + sE.getLocalizedMessage());
        } catch (SessionException sSE) {
            // Do nothing. The session has failed to complete successfully.
        } finally {
            setSessionComplete();
        }

        if(sessionSuccess)
            LOG.logQuiet("The TFTP Session has completed successfully.");
        else
            LOG.logQuiet("The TFTP Session Failed.");


        LOG.logQuiet("---- End TFTP Session ----");
        return sessionSuccess;
    }

    /**
     * Runs one iteration of the Session (Receives a message and delegates the handling to the
     * template method {@link #messageReceived(Message)}). Properly handles all Exceptions.
     * @throws InvalidPacketException
     * @throws IOException
     * @throws SessionException
     */
    private synchronized void run() throws InvalidPacketException, IOException, SessionException {
        // After initialize, we expect a message to be received
        // Wait for Message
        DatagramPacket packet = socket.receive();

        // Set current destination to most recent packet socket address
        this.currentDestAdr = packet.getSocketAddress();

        Message receivedMessage = Message.parseGenericMessage(packet);

        LOG.logVerbose("Received Message: ");
        LOG.logVerbose(receivedMessage);

        // Handle incoming error message (if applicable)
        if (receivedMessage.getMessageType().equals(MessageType.ERROR)) {

            // Handle error Received and stop the session
            sessionHandler.sessionErrorReceived(this, ErrorMessage.parseMessage(packet));
            LOG.logVerbose("An Error Message was received. Stopping session.");
            throw new SessionException();
        }

        // Ensure we are receiving the correct message type (according to the implementation)
        if (!receivedMessage.getMessageType().equals(incomingMessageType))
            throw new InvalidPacketException("Expected " + incomingMessageType + " (or ERROR). Actual: " + receivedMessage.getMessageType());

        // At this point, the MessageType of the received message will be the expected receive type
        messageReceived(receivedMessage);
    }

    /**
     * Sends a Message to the destination.
     * @param message The Message Object to send. If type is ERROR, equivalent to call to {@link #raiseError(ErrorMessage)}
     * @throws IOException
     */
    protected synchronized final void sendMessage(Message message) throws IOException, SessionException {

        // Any ERROR messages passed in will be passed to raiseError
        if(message.getMessageType().equals(MessageType.ERROR)) {
            raiseError((ErrorMessage) message);
            return;
        }

        socket.sendMessage(message, currentDestAdr);
    }

    /**
     * Allows subclasses to determine when the session is completed
     */
    protected synchronized final void setSessionComplete() {

        this.sessionComplete = true;
    }

    /**
     * @return The initial session request message
     */
    public synchronized RequestMessage getSessionRequest() {

        return sessionRequest;
    }

    /**
     * @return The ResourceFile corresponding to the Session Request
     */
    public synchronized ResourceFile getResourceFile() {
        return resourceFile;
    }

    /**
     * Allows an ErrorMessage to be raised and sent to the destination. Stops the
     * session immediately. A SessionException will always be thrown.
     * @param errMsg The ErrorMessage to raise
     * @throws IOException
     * @throws SessionException Always thrown to stop the session
     */
    public synchronized void raiseError(ErrorMessage errMsg) throws IOException, SessionException {
        LOG.logQuiet("Raised Error: " + errMsg.getMessage());
        LOG.logVerbose("Stopping Session. Sending Session Error:");
        LOG.logVerbose(errMsg);

        socket.sendMessage(errMsg, currentDestAdr);
        throw new SessionException();
    }

    /**
     * Allows subclasses to perform any initialization.
     * After calling this method, the session will expect to receive the next Message
     * This means that initialize() should initialize the session up to the point of expecting
     * a message from the destination. See subclasses for more details.
     *
     * @throws IOException
     * @throws SessionException
     */
    protected abstract void initialize() throws IOException, SessionException;

    /**
     * Allows subclasses to handle a message being received from the destination.
     * This message is guaranteed to be the same type as {@link #incomingMessageType}
     * This message will also never be an ERROR message.
     * @param message The message that was received. (Will be the same type as {@link #incomingMessageType})
     * @throws IOException
     * @throws InvalidPacketException
     * @throws SessionException
     */
    protected abstract void messageReceived(Message message) throws IOException, InvalidPacketException, SessionException;

}
