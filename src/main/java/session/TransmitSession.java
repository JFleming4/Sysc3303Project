package session;

import exceptions.InvalidPacketException;
import exceptions.SessionException;
import formats.AckMessage;
import formats.DataMessage;
import formats.ErrorMessage;
import formats.Message;
import formats.Message.MessageType;
import formats.RequestMessage;
import logging.Logger;
import resources.ResourceFile;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;

import static formats.Message.MessageType.*;
import static formats.Message.MessageType.ACK;
import static formats.Message.MessageType.RRQ;

public class TransmitSession extends TFTPSession {

    private static final Logger LOG = new Logger("TransmitSession");
    private static final MessageType INCOMING_MESSAGE_TYPE = ACK;
    private List<DataMessage> messageList;
    private DataMessage currentData;
    private int expectedAckBlockNumber;

    /**
     * Creates a TransmitSession with the given handler
     * @param sessionHandler The session handler used to handle errors in the session
     */
    public TransmitSession(ISessionHandler sessionHandler) {
        super(sessionHandler, INCOMING_MESSAGE_TYPE);
    }

    /**
     * Creates and RUNS a TransmitSession with the given handler
     * @param sessionHandler The session handler used to handle errors in the session
     */
    public TransmitSession(ISessionHandler sessionHandler, RequestMessage requestMessage, SocketAddress destAdr) {
        this(sessionHandler);
        this.runSession(requestMessage, destAdr);
    }

    /**
     * Handles a Message received by the session.
     * @param message The message that was received.
     * @throws IOException
     * @throws InvalidPacketException
     * @throws SessionException
     */
    @Override
    protected void messageReceived(Message message) throws IOException, InvalidPacketException, SessionException {

        // It is safe to assume that the message passed in will be of type AckMessage
        AckMessage ackMessage = (AckMessage) message;

        if (ackMessage.getBlockNum() < expectedAckBlockNumber - 1) {
            LOG.logVerbose("Received ACK with block: " + ackMessage.getBlockNum() + ". Ignoring ACK block");
            return;
        }
        else if (ackMessage.getBlockNum() == expectedAckBlockNumber - 1)
        {
            LOG.logVerbose("Received ACK for Block: " + ackMessage.getBlockNum() + ". Retransmitting current DATA.");
            sendCurrentData();
        }
        else if (ackMessage.getBlockNum() == expectedAckBlockNumber) {
            LOG.logVerbose("Received ACK for DATA block: " + ackMessage.getBlockNum() + ". Sending next data block.");

            // Check to see if message list is empty
            if(messageList.size() == 0)
            {
                LOG.logVerbose("Received ACK for last DATA block. Ending session.");
                LOG.logQuiet("Successfully completed transmit session");
                LOG.logQuiet("---- End File Transaction ---");

                super.setSessionComplete();
                return;
            }

            sendNextData();
        }
    }

    /**
     * Sends next data message in message list.
     * @throws IOException
     * @throws SessionException
     */
    private void sendNextData() throws IOException, SessionException
    {
        if(messageList.size() == 0)
        {
            LOG.logVerbose("Could not send next data. Message list is empty.");
            return;
        }

        // Remove next block of data & send it
        currentData = messageList.remove(0);

        sendCurrentData();
    }

    /**
     * Sends current data message
     * @throws IOException
     * @throws SessionException
     */
    private void sendCurrentData() throws IOException, SessionException
    {
        if(currentData == null) {
            LOG.logVerbose("Attempted to send current data (null)");
            return;
        }

        expectedAckBlockNumber = currentData.getBlockNum();
        sendMessage(currentData);
    }

    /**
     * Initializes the Session to a state where the next received
     * message will be an ACK
     *
     * @throws IOException
     * @throws SessionException
     */
    @Override
    protected void initialize() throws IOException, SessionException {
        RequestMessage sessionRequest = getSessionRequest();
        ResourceFile resourceFile = getResourceFile();
        MessageType requestType = sessionRequest.getMessageType();

        // Use the request message type to determine if we are receiving / sending the request
        // Since we are creating a transmit session:
        // RRQ + Transmit Session = SERVER (Transmitting from server)
        // WRQ + Transmit Session = CLIENT (Transmitting from client)

        // Check read permissions
        if(!resourceFile.canRead())
            sessionHandler.sessionErrorOccurred(this, new ErrorMessage(ErrorMessage.ErrorType.ACCESS_VIOLATION, "Could not read file '" + sessionRequest.getFileName() + "'"));

        // Parse Data Message list from file
        this.messageList = DataMessage.createDataMessageSequence(resourceFile.readFileToBytes());


        // This is just a sanity check. This should never occur.
        // Even File Size = 0 should result in at least 1 DATA block (with 0 bytes of data)
        if(this.messageList.size() == 0) {
            LOG.logVerbose("There are no DATA blocks to transmit. This should not happen.");
            throw new SessionException();
        }

        LOG.logVerbose("Loaded " + this.messageList.size() + " DATA blocks");

        // If we are on the server side (RRQ), we will send the first data block
        if(requestType.equals(RRQ))
        {
            LOG.logQuiet("Read Request received. Sending first DATA block");
            sendNextData();
        }
        else if (requestType.equals(WRQ))
        {
            // Since we are on the Client side, we need to send the request
            // And expect a WRQ ACK
            LOG.logQuiet("Sending Write Request");
            sendMessage(sessionRequest);

            // Update socket address on the next receive
            // (since it will be the address from the new server worker)
            setShouldUpdateSocketAddress();

            expectedAckBlockNumber = 0;
        }
    }
}
