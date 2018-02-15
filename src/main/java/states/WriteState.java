package states;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import exceptions.InvalidPacketException;
import formats.AckMessage;
import formats.DataMessage;
import formats.ErrorMessage;
import formats.ErrorMessage.ErrorType;
import formats.Message;
import formats.Message.MessageType;
import formats.RequestMessage;
import logging.Logger;
import resources.ResourceManager;
import socket.TFTPDatagramSocket;

public class WriteState extends State {

	private static final int SOCKET_TIMEOUT = 5000;

	private TFTPDatagramSocket socket;
	private ResourceManager resourceManager;
	private SocketAddress serverAddress;
	private String filename;

    public WriteState(SocketAddress serverAddress, ResourceManager resourceManager, String filename, boolean isVerbose)throws IOException {
        this(serverAddress, resourceManager, filename, isVerbose, new TFTPDatagramSocket());
    }


    public WriteState(SocketAddress serverAddress, ResourceManager resourceManager, String filename, boolean isVerbose, TFTPDatagramSocket socket) throws SocketException {
        this.serverAddress = serverAddress;
        this.filename = filename;

        this.socket = socket;
        this.socket.setSoTimeout(SOCKET_TIMEOUT);
        this.resourceManager = resourceManager;
        if (isVerbose)
            Logger.setLogLevel(Logger.LogLevel.VERBOSE);
        else
            Logger.setLogLevel(Logger.LogLevel.QUIET);
    }

	@Override
	public State execute() {
        try {
            if(!resourceManager.fileExists(filename))
                throw new FileNotFoundException("File Not Found");

            if(!resourceManager.fileExists(filename))
                throw new FileNotFoundException("File (" + filename + ") Not Found");

            try {
                // Create the write request & send
                RequestMessage wrqMessage = new RequestMessage(MessageType.WRQ ,filename);
                socket.sendMessage(wrqMessage, serverAddress);
                LOG.logQuiet("---- Begin File Transaction ---");
                LOG.logQuiet("Write Request has been sent!");

                // Wait for WRQ ack
                DatagramPacket recv = socket.receivePacket();

                try
                {
                    switch (Message.getMessageType(recv.getData())) {
                        case ACK:
                            AckMessage ack = AckMessage.parseMessageFromPacket(recv);
                            LOG.logVerbose("Received WRQ ACK");

                            if (ack.getBlockNum() != 0) throw new IOException("Incorrect Initial Block Number");

                            sendDataBlock(resourceManager.readFileToBytes(filename), recv.getSocketAddress());
                            LOG.logQuiet("Successfully completed write operation");
                            LOG.logQuiet("---- End File Transaction ---");
                            break;

                        case ERROR:
                            handleErrorMessage(recv);
                            break;

                        default:
                            break;
                    }
                } catch (InvalidPacketException ipE) {

                }
            } catch(UnknownHostException uHE) {
                LOG.logQuiet("Error: Unknown Host Entered");
            } catch(IOException ioE) {
                if (ioE.getLocalizedMessage().toLowerCase().contains("access is denied") || ioE.getLocalizedMessage().toLowerCase().contains("permission denied"))
                {
                    LOG.logQuiet("You do not have permissions to read this file");
                    ErrorMessage errMsg = new ErrorMessage(ErrorType.ACCESS_VIOLATION, "You do not have the correct permissions for this file");
                    socket.sendMessage(errMsg, this.serverAddress);
                } else {
                    ioE.printStackTrace();
                }
            }
        } catch (SocketException sE) {
            sE.printStackTrace();
        } catch (FileNotFoundException fNFE) {
            LOG.logQuiet("Error: " + fNFE.getMessage());
        } catch (IOException ioE) {
            LOG.logQuiet("IOException occurred. " + ioE.getLocalizedMessage());
        } finally {
            socket.close();
        }

        return new InputState();
    }

    /**
     * Sends an array of bytes over TFTP, splicing the data in blocks if necessary
     * @param data The array of data
     * @param socketAddress The Socket address used in sending packet
     * @throws IOException
     */
    private void sendDataBlock(byte[] data, SocketAddress socketAddress) throws IOException {

        // Create a sequence of data for the byte array
        List<DataMessage> messages = DataMessage.createDataMessageSequence(data);
        LOG.logVerbose("Sending " + messages.size() + " Data messages to " + socketAddress);

        try {
            // Send each data block sequentially
            for(DataMessage m : messages) {

                LOG.logVerbose("Block: " + m.getBlockNum() + ", Data size: " + m.getDataSize() + ", Final Block: " + m.isFinalBlock());
                // Send Data block message
                socket.sendMessage(m, socketAddress);

                // Wait for Ack message before continuing
                DatagramPacket packet = socket.receivePacket();
                switch (Message.getMessageType(packet.getData())) {
                    case ERROR:
                        handleErrorMessage(packet);
                        break;

                    case ACK:
                        AckMessage ack = AckMessage.parseMessageFromPacket(packet);
                        LOG.logVerbose("Ack Received: " + ack.getBlockNum());

                        if(ack.getBlockNum() != m.getBlockNum()) {
                            throw new InvalidPacketException("Invalid Block Number");
                        }
                        break;

                    default:
                        break;
                }
            }
        } catch (InvalidPacketException e) {
            e.printStackTrace();
            throw new IOException("Failed to parse Ack Message", e);
        }
    }

    /**
     * Handle a received error message
     * @param recv DatagramPacket to be received
     * @throws InvalidPacketException
     */
    private void handleErrorMessage(DatagramPacket recv) throws InvalidPacketException {
        ErrorMessage errorMessage = ErrorMessage.parseMessageFromPacket(recv);
        LOG.logVerbose("Received error (" + errorMessage.getErrorType() + ") with message: " + errorMessage.getMessage());

        // Determine the Type of the ErrorMessage
        switch (errorMessage.getErrorType()) {
            case FILE_EXISTS:
                break;

            case DISK_FULL:
                // Justin's code
                break;

            default:
                break;
        }

    }

}
