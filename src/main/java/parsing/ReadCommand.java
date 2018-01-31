package parsing;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

import exceptions.InvalidPacketException;
import formats.DataMessage;
import formats.Message.MessageType;
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
				socket.sendRequest(MessageType.RRQ ,this.getFilename(), this.getServerAddress());
				for(;;) {
					DataMessage dataMessage;
                    try {
                        dataMessage = socket.receiveData();
                        // Initialize block number to one sent from server
                        if(expBlockNum == -1) expBlockNum = dataMessage.getBlockNum();
                        if(dataMessage.getBlockNum() != expBlockNum) throw new Error("Unexpected Block Number");

                        resourceManager.writeBytesToFile(this.getFilename(), dataMessage.getData());

                        if(dataMessage.getData().length < BUFF_HEADER_SIZE - 4) {
                            LOG.logVerbose("End of read file reached");
                            break;
                        }

                        // Update Block number and send
                        expBlockNum = dataMessage.getBlockNum() + 1;
                        socket.sendAck(dataMessage.getBlockNum(), dataMessage.getSocketAddress());
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
