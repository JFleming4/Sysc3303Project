package states;

import static resources.Configuration.GLOBAL_CONFIG;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.AccessDeniedException;
import java.util.List;

import exceptions.InvalidPacketException;
import formats.AckMessage;
import formats.DataMessage;
import formats.RequestMessage;
import formats.Message.MessageType;
import logging.Logger;
import resources.ResourceManager;
import socket.TFTPDatagramSocket;

public class WriteState extends State {
	private static final int SOCKET_TIMEOUT = 5000;
	
	private TFTPDatagramSocket socket;
	private SocketAddress serverAddress;
	private String filename;
	
	public WriteState(SocketAddress serverAddress, String filename, boolean isVerbose) {
		this.serverAddress = serverAddress;
		this.filename = filename;
		if (isVerbose)
			Logger.setLogLevel(Logger.LogLevel.VERBOSE);
		else
			Logger.setLogLevel(Logger.LogLevel.QUIET);
	}

	@Override
	public State execute() {
        try {
            ResourceManager resourceManager = new ResourceManager(GLOBAL_CONFIG.CLIENT_RESOURCE_DIR);
            if(!resourceManager.fileExists(filename))
                throw new FileNotFoundException("File Not Found");

            socket = new TFTPDatagramSocket();
            socket.setSoTimeout(SOCKET_TIMEOUT);

            try {

                // Create the write request & send
                RequestMessage wrqMessage = new RequestMessage(MessageType.WRQ, filename);
                socket.sendMessage(wrqMessage, serverAddress);
                LOG.logQuiet("---- Begin File Transaction ---");
                LOG.logQuiet("Write Request has been sent!");


                // Wait for WRQ ack
                DatagramPacket recv = socket.receivePacket();
                AckMessage ack = AckMessage.parseMessageFromPacket(recv);
                LOG.logVerbose("Received WRQ ACK");

                if(ack.getBlockNum() != 0) throw new IOException("Incorrect Initial Block Number");

                sendDataBlock(resourceManager.readFileToBytes(filename), recv.getSocketAddress());
                LOG.logQuiet("Successfully completed write operation");
                LOG.logQuiet("---- End File Transaction ---");
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
        } catch (IOException ioE) {
            if(ioE instanceof AccessDeniedException) {
                LOG.logQuiet("You do not have the correct permisions to access this file");
            }
            LOG.logVerbose("IOException occurred. " + ioE.getLocalizedMessage());
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
                AckMessage ack = AckMessage.parseMessageFromPacket(packet);
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
