package session;

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

import static formats.Message.MessageType.DATA;
import static resources.Configuration.GLOBAL_CONFIG;

/**
 * A 'ReceiveSession' is an abstract concept, and is not specific to the client or the server.
 * This class is used to represent a TFTP Session, where an entity is receiving DATA packets,
 * and sending out ACK packets.
 *
 * This Occurs when the Client sends a RRQ, and when the Server receives a WRQ.
 * Both the Client and Server would be receiving entities in this situation.
 */
public class ReceiveSession extends TFTPSession {

    private static final Logger LOG = new Logger("ReceiveSession");
    private static final MessageType INCOMING_MESSAGE_TYPE = DATA;
    private int lastBlockAcked;
    private int lastBlockReceivedCount;

    /**
     * Creates a new Session given a Session Handler
     * @param sessionHandler The session handler that will handle errors.
     */
    public ReceiveSession(ISessionHandler sessionHandler) {
        super(sessionHandler, INCOMING_MESSAGE_TYPE);
    }

    /**
     * Creates and RUNS a new Session given a Session Handler
     * @param sessionHandler The session handler that will handle errors.
     */
    public ReceiveSession(ISessionHandler sessionHandler, RequestMessage requestMessage, SocketAddress destAdr) {
        this(sessionHandler);
        this.runSession(requestMessage, destAdr);
        this.lastBlockReceivedCount = 0;
    }


    /**
     * This function is called when a message is received by the TFTP session object.
     * @param message The message received by TFTPSession
     * @throws SessionException
     * @throws IOException
     */
    protected synchronized void messageReceived(Message message) throws SessionException, IOException {

        // It is safe to assume that the message passed in will be of type DataMessage
        DataMessage dataMessage = (DataMessage) message;

        if (lastBlockAcked == DataMessage.MAX_BLOCK_NUM && dataMessage.getBlockNum() == 1) {
        	    // Looping
        } else if (dataMessage.getBlockNum() < lastBlockAcked) {
            LOG.logVerbose("Received DATA with old block: " + dataMessage.getBlockNum() + ". Ignoring DATA block");
            return;
        }

        if (dataMessage.getBlockNum() == lastBlockAcked) {
            LOG.logVerbose("Received Retransmitted DATA with block: " + dataMessage.getBlockNum());
            sendAckForData(dataMessage);
            return;
        }

        ResourceFile resourceFile = getResourceFile();

        // Check to see if there is enough usable space
        int numBytesToWrite = dataMessage.getDataSize();

        // Check for usable bytes before writing bytes to file
        if (resourceFile.getUsableSpace() < numBytesToWrite) {
            LOG.logQuiet("" + resourceFile.getUsableSpace());
            LOG.logQuiet("There is not enough usable space to write DATA block to file.");
            sessionHandler.sessionErrorOccurred(this, new ErrorMessage(ErrorMessage.ErrorType.DISK_FULL, "Not enough usable space on disk. Failed to write block #" + dataMessage.getBlockNum()));
        }

        // Check to ensure we have the proper permissions to write to file
        if (!resourceFile.canWrite()) {
            LOG.logQuiet("Do not have permissions to write to file '" + getSessionRequest().getFileName() + "'.");
            sessionHandler.sessionErrorOccurred(this, new ErrorMessage(ErrorMessage.ErrorType.ACCESS_VIOLATION, "Write permissions denied on file: " + getSessionRequest().getFileName()));
        }

        // Write block to file
        resourceFile.writeBytesToFile(dataMessage.getData());

        // Send ack if write was successful
        sendAckForData(dataMessage);
    }

    /**
     * Sends ACK block for DATA
     * @param dataMessage The DATA message to Acknowledge.
     * @throws IOException
     * @throws SessionException
     */
    private synchronized void sendAckForData(DataMessage dataMessage) throws IOException, SessionException {
        // Send ACK for data
        AckMessage ackMsg = new AckMessage(dataMessage.getBlockNum());
        sendMessage(ackMsg);

        // Set the last block acknowledged
        this.lastBlockAcked = dataMessage.getBlockNum();

        LOG.logVerbose("Sent Ack for block: " + ackMsg.getBlockNum());

        // Check if this was the last block
        if (dataMessage.isFinalBlock()) {

            lastBlockReceivedCount++;

            // Fail session if the destination fails to receive the last ACK multiple times
            if(lastBlockReceivedCount > GLOBAL_CONFIG.MAX_TRANSMIT_ATTEMPTS)
            {
                LOG.logQuiet("Failed to complete receive session. Destination failed to receive last block acknowledgement");
                throw new SessionException();
            }

            if(lastBlockReceivedCount == 1)
            {
                LOG.logVerbose("End of read file reached");
                LOG.logQuiet("Successfully received file.");
            }
            else if(lastBlockReceivedCount > 1)
            {
                LOG.logVerbose("Attempting to send last block ACK. Attempt # " + lastBlockReceivedCount);
            }

            // Tell the session it is now complete once we timeout on the next receive
            setSessionCompleteOnTimeout();
        }
    }

    /**
     * Initialize a Receive Session. Based on the session request message, we can determine if
     * we need to send the request message OR if we need to ACK the request message.
     * The following is Client/Server specific, so maybe we could delegate this to the
     * session handler in the future. For now it seemed fine.
     * @throws IOException
     */
    @Override
    protected synchronized void initialize() throws IOException, SessionException {

        RequestMessage sessionRequest = getSessionRequest();
        MessageType requestType = sessionRequest.getMessageType();

        // Use the request message type to determine if we are receiving / sending the request
        // Since we are creating a receive session:
        // RRQ + Receive Session = CLIENT (Receiving from server)
        // WRQ + Receive Session = SERVER (Receiving from client)

        if (requestType.equals(MessageType.RRQ)) {

            LOG.logVerbose("Sending Read Request");
            // Since we are on the client side, we need to first send out the request
            sendMessage(sessionRequest);

            // Update socket address on the next receive
            // (since it will be the address from the new server worker)
            setShouldUpdateSocketAddress();

        } else if (requestType.equals(MessageType.WRQ)) {

            LOG.logVerbose("Sending WRQ ACK");
            // Since we are on the server side, we have already received the request
            // We need to send back a WRQ ACK
            AckMessage wrqAck = new AckMessage(0);
            sendMessage(wrqAck);
        }
    }
}
