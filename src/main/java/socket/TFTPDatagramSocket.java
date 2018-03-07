package socket;

import formats.Message;
import logging.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public class TFTPDatagramSocket extends DatagramSocket {
	public final static Logger LOG = new Logger("TFTPDatagramSocket");

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
		boolean transmit = true;
		while(transmit)
		{
			try {
				byte[] data = msg.toByteArray();
				DatagramPacket packet = new DatagramPacket(data, data.length, socketAddress);
				LOG.logVerbose("Sending Message to " + socketAddress);
				LOG.logVerbose("===== Packet Information ====");
				LOG.logVerbose(packet);
				LOG.logVerbose("===== End Packet Information ====");
				LOG.logVerbose(System.lineSeparator());
				send(packet);
				transmit = false;
			} catch (SocketTimeoutException sTD) {
				LOG.logVerbose("Retransmitting " + msg.getMessageType());
				LOG.logVerbose(msg);
			}
		}
	}

	/**
	 * Takes a packet with pre-existing data and forwards it to another host
	 * @param clientPacket The packet to forward
	 * @param address The address to forward the packet to
	 * @param port The port to forward the packet to
	 * @throws IOException
	 */
	public void forwardPacket(DatagramPacket clientPacket, InetAddress address, int port) throws IOException {
		clientPacket.setAddress(address);
		clientPacket.setPort(port);

		LOG.logVerbose("Forwarding packet to address: " + address + ", Port: " + port);
		LOG.logVerbose(clientPacket);
		send(clientPacket);
	}

	public void forwardPacket(DatagramPacket clientPacket, InetSocketAddress address) throws IOException {
		forwardPacket(clientPacket, address.getAddress(), address.getPort());
	}

	/**
	 * Receives a TFTP message over the socket
	 * @throws IOException
	 */
	public DatagramPacket receive() throws IOException {
		DatagramPacket packet = new DatagramPacket(new byte[Message.MAX_PACKET_SIZE], Message.MAX_PACKET_SIZE);
		super.receive(packet);

		// Trim and set byte array
		byte[] trimmedData = Arrays.copyOf(packet.getData(), packet.getLength());
		packet.setData(trimmedData);

		LOG.logVerbose("Received Packet from " + packet.getSocketAddress());
		LOG.logVerbose("===== Packet Information ====");
		LOG.logVerbose(packet);
		LOG.logVerbose("===== End Packet Information ====");
		LOG.logVerbose(System.lineSeparator());


		return packet;
	}
}

