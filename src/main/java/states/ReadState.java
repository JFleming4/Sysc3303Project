package states;

import static resources.Configuration.GLOBAL_CONFIG;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.file.AccessDeniedException;

import exceptions.InvalidPacketException;
import formats.AckMessage;
import formats.DataMessage;
import formats.ErrorMessage;
import formats.RequestMessage;
import logging.Logger;
import formats.Message.MessageType;
import resources.ResourceManager;
import socket.TFTPDatagramSocket;

public class ReadState extends State {
	private static final int SOCKET_TIMEOUT = 5000;
	
	private TFTPDatagramSocket socket;
	private SocketAddress serverAddress;
	private String filename;
	
	public ReadState(SocketAddress serverAddress, String filename, boolean isVerbose) {
		this.serverAddress = serverAddress;
		this.filename = filename;
		if (isVerbose)
			Logger.setLogLevel(Logger.LogLevel.VERBOSE);
		else
			Logger.setLogLevel(Logger.LogLevel.QUIET);
	}

	@Override
	public State execute() {
		int expBlockNum = -1;
		try {
			// Create the resource manager, handle IOException if failed to get resource directory
			ResourceManager resourceManager = new ResourceManager(GLOBAL_CONFIG.CLIENT_RESOURCE_DIR);

		    socket = new TFTPDatagramSocket();
			socket.setSoTimeout(SOCKET_TIMEOUT);

			// Set up + Send RRQ Message
            RequestMessage rrqMessage = new RequestMessage(MessageType.RRQ ,filename);
			socket.sendMessage(rrqMessage, serverAddress);
			LOG.logQuiet("---- Begin File Transaction ---");
            LOG.logQuiet("Read request sent!");
            LOG.logQuiet("Waiting to receive data");

			for(;;) {

				try {
				    // Receive read data from server
                    DatagramPacket recv = socket.receivePacket();
                    if(ErrorMessage.isErrorMessage(recv)) {
                    	ErrorMessage.logErrorPacket(recv);
                    	break;
                    }
                    DataMessage dataMessage = DataMessage.parseMessageFromPacket(recv);
                    LOG.logVerbose("Received data block: " + dataMessage.getBlockNum());
        
                    // Initialize block number to one sent from server
                    if(expBlockNum == -1)
                        expBlockNum = dataMessage.getBlockNum();

                    // Confirm expected block number
                    if(dataMessage.getBlockNum() != expBlockNum)
                        throw new Error("Unexpected Block Number");

                    // Write to file
                    resourceManager.writeBytesToFile(filename, dataMessage.getData());

                    // Send ACK and increment block number
                    AckMessage ackMsg = new AckMessage(expBlockNum++);
                    socket.sendMessage(ackMsg, recv.getSocketAddress());
                    LOG.logVerbose("Sent Ack for block: " + ackMsg.getBlockNum());

                    // Check if this was the last block
                    if(dataMessage.isFinalBlock()) {
                    		LOG.logVerbose("End of read file reached");
						LOG.logQuiet("Successfully received file");
						LOG.logQuiet("---- End File Transaction ---");
						break;
                    }

                } catch (InvalidPacketException e) {
                    e.printStackTrace();
                }
			}
		} catch(UnknownHostException uHE) {
			LOG.logQuiet("Error: Unknown Host Entered");
		} catch(IOException ioE) {
			LOG.logVerbose("An IOException has occurred: " + ioE.getLocalizedMessage());
			ioE.printStackTrace();
		} finally {
			socket.close();
		}
		
		return new InputState();
	}
}
