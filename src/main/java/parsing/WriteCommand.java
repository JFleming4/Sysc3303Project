package parsing;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import exceptions.InvalidPacketException;
import formats.AckMessage;
import formats.DataMessage;
import formats.Message.MessageType;
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
	public void execute() {
        if(this.tokens.size() < 3) {
            LOG.logQuiet("Error: Not enough arguments");
        }
        else {
            ResourceManager resourceManager = new ResourceManager(RESOURCE_DIR);
            AckMessage ack;
            try {
                if(!resourceManager.fileExists(this.getFilename())) throw new FileNotFoundException("File Not Found");
                socket = new TFTPDatagramSocket();
                socket.setSoTimeout(SOCKET_TIMETOUT);
                try {
                    socket.sendRequest(MessageType.WRQ ,this.getFilename(), this.getServerAddress());
                    LOG.logVerbose("Waiting to receive initial ack");
                    ack = socket.receiveAck();

                    if(ack.getBlockNum() != 0) throw new IOException("Incorrect Initial Block Number");

                    sendDataBlock(resourceManager.readFileToBytes(this.getFilename()), ack.getSocketAddress());
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

        // Send each data block sequentially
        for(DataMessage m : messages) {
            socket.sendMessage(m, socketAddress);
            AckMessage ack;
            try {
                ack = socket.receiveAck();
                if(ack.getBlockNum() != m.getBlockNum()) {
                    throw new IOException("Invalid Block Number");
                }
            } catch (InvalidPacketException e) {
                e.printStackTrace();
            }
        }
    }
}
