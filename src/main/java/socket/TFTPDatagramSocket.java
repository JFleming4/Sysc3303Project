package socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import formats.Message;

public class TFTPDatagramSocket extends DatagramSocket {


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
	public DatagramPacket receiveMessage() throws IOException
	{
		DatagramPacket packet = new DatagramPacket(new byte [Message.MAX_PACKET_SIZE], Message.MAX_PACKET_SIZE);
		receive(packet);
		return packet;
	}
}
