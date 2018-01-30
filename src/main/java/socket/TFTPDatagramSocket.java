package socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;

import exceptions.InvalidPacketException;
import formats.AckMessage;
import formats.DataMessage;
import formats.Message;
import formats.Message.MessageType;
import formats.RequestMessage;

public class TFTPDatagramSocket extends DatagramSocket {
	public static final int BUFF_HEADER_SIZE = 516;
	private static final String DEFAULT_MODE = "netascii";

	public TFTPDatagramSocket() throws SocketException {
		super();
	}

	public TFTPDatagramSocket(int port) throws SocketException {
		super(port);
	}



	/**
	 * Sends a TFTP message over the socket
	 * @param msg The message to send
	 * @param socketAddress The Socket address used in sending packet
	 * @throws IOException
	 */
	public void sendMessage(Message msg, SocketAddress socketAddress) throws IOException
	{
		byte[] data = msg.toByteArray();
		send(new DatagramPacket(data, data.length, socketAddress));
	}

	/**
	 * Receives a TFTP message over the socket
	 * @throws IOException
	 */
	private DatagramPacket receiveMessage() throws IOException
	{
		DatagramPacket packet = new DatagramPacket(new byte [BUFF_HEADER_SIZE], BUFF_HEADER_SIZE);
		receive(packet);
		return packet;
	}

	/**
	 * Send block acknowledgement message
	 * @param blockNum The block number to acknowledge
	 * @param socketAddress The Socket address used in sending packet
	 * @throws IOException
	 */
	public void sendAck(int blockNum, SocketAddress socketAddress) throws IOException {
		sendMessage(new AckMessage(blockNum), socketAddress);
	}

	 /**
     * Receive acknowledgement message
     * @param blockNum the block number expected
     * @throws IOException
     * @throws InvalidPacketException
     */
    public AckMessage receiveAck() throws InvalidPacketException, IOException {
        return AckMessage.parseDataFromPacket(receiveMessage());
    }

	/**
	 * Sends an array of bytes over TFTP, splicing the data in blocks if necessary
	 * @param data The array of data
	 * @param socketAddress The Socket address used in sending packet
	 * @throws IOException
	 */
	public void sendRequest(MessageType reqType, String filename, SocketAddress socketAddress) throws IOException {
		sendMessage(new RequestMessage(reqType, filename, DEFAULT_MODE), socketAddress);
	}

    /**
     * Return Data Message from socket
     * @return DataMessage
     * @throws InvalidPacketException
     * @throws IOException
     */
	public DataMessage receiveData() throws InvalidPacketException, IOException {
	    return DataMessage.parseDataFromPacket(receiveMessage());
	}

}
