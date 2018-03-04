package states;

import static resources.Configuration.GLOBAL_CONFIG;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

import formats.Message;
import logging.Logger;
import socket.TFTPDatagramSocket;

public class ForwardState extends State {
	protected static final Logger LOG = new Logger("ErrorSimulator");

	private TFTPDatagramSocket connection;
	protected InetAddress serverAddress;
	private InetSocketAddress clientAddress;
	protected int currentServerWorkerPort;
	
	public ForwardState(TFTPDatagramSocket connection, InetAddress serverAddress) {
		this.connection = connection;
		this.serverAddress = serverAddress;
	}

	@Override
	public State execute() {
		DatagramPacket incomingPacket;

		LOG.logQuiet("Error Simulator is running.");
		while (!connection.isClosed()) {
			try {
				LOG.logVerbose("Waiting for request from client");
				incomingPacket = connection.receivePacket();
				forwardPacket(incomingPacket);				
			} catch (SocketException sE)
			{
				// Socket closed exception
				if(!connection.isClosed())
					sE.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			catch (Exception e) {
				if (e.getMessage().equals("TEST EXCEPTION"))
					break;
			}
		}
		return this;
	}
	
	protected void forwardRequest(DatagramPacket incomingPacket, InetAddress serverAddress) throws IOException {
		connection.forwardPacket(incomingPacket, serverAddress, GLOBAL_CONFIG.SERVER_PORT);
	}
	protected void forwardPacket(DatagramPacket incomingPacket) throws IOException {
		InetAddress incomingAddr = incomingPacket.getAddress();
		int incomingPort = incomingPacket.getPort();
		
		// Check to see what the source IP is
		// If the request is not from the current client AND not from the server worker,
		// We have a new client (and will forward this ONE packet to server port 69)
		if(!isFromClient(incomingAddr, incomingPort) && !isFromServerWorker(incomingAddr, incomingPort))
		{
			LOG.logQuiet("New Client Detected.");
			clientAddress = new InetSocketAddress(incomingPacket.getAddress(), incomingPacket.getPort());

			LOG.logQuiet("Forwarding initial request to server");
			forwardRequest(incomingPacket, serverAddress);

			LOG.logVerbose("Waiting for initial response from server");
			DatagramPacket serverResponsePacket = connection.receivePacket();

			// Now we learn the server workers PORT
			currentServerWorkerPort = serverResponsePacket.getPort();
			LOG.logVerbose("Received server response. Worker thread port: " + currentServerWorkerPort);

			// Now forward the initial response back to client
			connection.forwardPacket(serverResponsePacket, clientAddress);
			LOG.logQuiet("Forwarding initial response to client");
		}
		// If the packet is from the server worker
		// We are going to forward the packet to the current client
		else if(!isFromClient(incomingAddr, incomingPort) && isFromServerWorker(incomingAddr, incomingPort))
		{
			LOG.logQuiet("Received message from client. Forwarding to server.");
			connection.forwardPacket(incomingPacket, clientAddress);
		}
		// If the packet is from the current client
		// We are going to forward the packet to the server
		else if(!isFromServerWorker(incomingAddr, incomingPort) && isFromClient(incomingAddr, incomingPort))
		{
			LOG.logQuiet("Received message from server. Forwarding to client.");
			connection.forwardPacket(incomingPacket, serverAddress, currentServerWorkerPort);
		}

	}
	
	/**
	 * Checks to see if a request is coming from the current client.
	 * @param addr The address to check
	 * @param port The port to check
	 * @return True if addr/port match current client
	 */
	private boolean isFromClient(InetAddress addr, int port)
	{
		return clientAddress != null && clientAddress.getAddress().equals(addr) && clientAddress.getPort() == port;
	}

	/**
	 * Checks to see if a request is coming from the current server worker.
	 * @param addr The address to check
	 * @param port The port to check
	 * @return True if addr/port match current server worker
	 */
	private boolean isFromServerWorker(InetAddress addr, int port)
	{
		return serverAddress != null && serverAddress.equals(addr) && currentServerWorkerPort == port;
	}
}
