package parsing;

import java.io.IOException;
import java.net.DatagramPacket;
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

public class ReadCommand extends SocketCommand {
	private static final Logger LOG = new Logger("FTPClient - Read");
	private static final String READ_OPERATION = "read";
	private TFTPDatagramSocket socket;

	public ReadCommand(List<String> tokens) {
		super(READ_OPERATION, tokens);
	}

	public ReadCommand(String[] tokens) {
		super(READ_OPERATION, tokens);
	}

	@Override
	public void execute_operation() {
		if(this.tokens.size() < 3) {
			LOG.logQuiet("Error: Not enough arguments");
		}
		else {
            ResourceManager resourceManager = new ResourceManager(RESOURCE_DIR);

			int expBlockNum = -1;
			try {
			    socket = new TFTPDatagramSocket();
				socket.setSoTimeout(SOCKET_TIMETOUT);

				// Set up + Send RRQ Message
                RequestMessage rrqMessage = new RequestMessage(MessageType.RRQ ,this.getFilename());
				socket.sendMessage(rrqMessage, this.getServerAddress());
                LOG.logQuiet("Read request sent!");
                LOG.logQuiet("Waiting to receive data");

				for(;;) {

					try {
					    // Receive read data from server
                        DatagramPacket recv = socket.receiveMessage();
                        DataMessage dataMessage = DataMessage.parseDataFromPacket(recv);
                        LOG.logVerbose("Received data block: " + dataMessage.getBlockNum());
            
                        // Initialize block number to one sent from server
                        if(expBlockNum == -1)
                            expBlockNum = dataMessage.getBlockNum();

                        // Confirm expected block number
                        if(dataMessage.getBlockNum() != expBlockNum)
                            throw new Error("Unexpected Block Number");

                        // Write to file
                        resourceManager.writeBytesToFile(this.getFilename(), dataMessage.getData());

                        // Send ACK and increment block num
                        AckMessage ackMsg = new AckMessage(expBlockNum++);
                        socket.sendMessage(ackMsg, recv.getSocketAddress());
                        LOG.logVerbose("Sent Ack for block: " + ackMsg.getBlockNum());

                        // Check if this was the last block
                        if(dataMessage.isFinalBlock()) {
                            LOG.logVerbose("End of read file reached");
                            break;
                        }

                    } catch (InvalidPacketException e) {
                        e.printStackTrace();
                    }
				}
			} catch(UnknownHostException uHE) {
				LOG.logQuiet("Error: Unknown Host Entered");
			}catch(IOException ioE) {
				ioE.printStackTrace();
			}
		    socket.close();
		}
	}
}
