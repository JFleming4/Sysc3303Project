package parsing;

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
import formats.Message.MessageType;
import formats.RequestMessage;
import logging.Logger;
import resources.ResourceManager;
import socket.TFTPDatagramSocket;

public class WriteCommand extends SocketCommand {
    private static final Logger LOG = new Logger("FTPClient - Write");
	private static final String WRITE_OPERATION = "write";
	private TFTPDatagramSocket socket;

	public WriteCommand(List<String> tokens) {
		super(WRITE_OPERATION, tokens);
	}

	public WriteCommand(String[] tokens) {
		super(WRITE_OPERATION, tokens);
	}

	@Override
	public void execute_operation() {
        if(this.tokens.size() < 3) {
            LOG.logQuiet("Error: Not enough arguments");
        }
        else {
            ResourceManager resourceManager = new ResourceManager(RESOURCE_DIR);

            try {
                if(!resourceManager.fileExists(this.getFilename()))
                    throw new FileNotFoundException("File Not Found");

                socket = new TFTPDatagramSocket();
                socket.setSoTimeout(SOCKET_TIMETOUT);

                try {

                    // Create the write request & send
                    RequestMessage wrqMessage = new RequestMessage(MessageType.WRQ ,this.getFilename());
                    socket.sendMessage(wrqMessage, this.getServerAddress());
                    LOG.logQuiet("Write Request has been sent!");


                    // Wait for WRQ ack
                    DatagramPacket recv = socket.receiveMessage();
                    AckMessage ack = AckMessage.parseDataFromPacket(recv);
                    LOG.logVerbose("Received WRQ ACK");

                    if(ack.getBlockNum() != 0) throw new IOException("Incorrect Initial Block Number");

                    sendDataBlock(resourceManager.readFileToBytes(this.getFilename()), recv.getSocketAddress());
                    LOG.logQuiet("Successfully completed write operation");
                } catch(InvalidPacketException ipE) {
                    LOG.logQuiet(ipE.getMessage());
                } catch(UnknownHostException uHE) {
                    LOG.logQuiet("Error: Unknown Host Entered");
                } catch(IOException ioE) {
                    ioE.printStackTrace();
                }
                socket.close();
            } catch (SocketException sE) {
                    sE.printStackTrace();
            } catch (FileNotFoundException fNFE) {
                LOG.logQuiet("Error: " + fNFE.getMessage());
            }
        }
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
                DatagramPacket packet = socket.receiveMessage();
                AckMessage ack = AckMessage.parseDataFromPacket(packet);
                LOG.logVerbose("Ack Received: " + ack.getBlockNum());

                if(ack.getBlockNum() != m.getBlockNum()) {
                    throw new InvalidPacketException("Invalid Block Number");
                }
            }
        } catch (InvalidPacketException e) {
            e.printStackTrace();
            throw new IOException("Failed to parse Ack Message", e);
        }
    }
}
