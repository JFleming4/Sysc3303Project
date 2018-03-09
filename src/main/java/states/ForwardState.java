package states;

import static resources.Configuration.GLOBAL_CONFIG;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import logging.Logger;
import socket.TFTPDatagramSocket;

public class ForwardState extends State {
	private static final int SOCKET_TIMEOUT = 1000;
	protected static final Logger LOG = new Logger("ErrorSimulator");

	private TFTPDatagramSocket connection;
	protected InetAddress serverAddress;
	private InetSocketAddress clientAddress;
	protected int currentServerWorkerPort;
	private boolean stopping;
	
	public ForwardState(TFTPDatagramSocket connection, InetAddress serverAddress) throws SocketException {
		this.connection = connection;
		this.connection.setSoTimeout(SOCKET_TIMEOUT);
		this.serverAddress = serverAddress;
		this.stopping = false;
	}

	@Override
	public State execute() {
		DatagramPacket incomingPacket;

		LOG.logQuiet("Error Simulator is running.");
		LOG.logVerbose("Waiting for request from client");
		while (!connection.isClosed() && !stopping) {
			try {
				incomingPacket = connection.receive();
				forwardPacket(incomingPacket);				
			} catch (SocketException sE)
			{
				// Socket closed exception
				if(!connection.isClosed())
					sE.printStackTrace();
			}
			catch (SocketTimeoutException sTE)
			{
				continue;
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
			DatagramPacket serverResponsePacket = connection.receive();

			// Now we learn the server workers PORT
			currentServerWorkerPort = serverResponsePacket.getPort();
			LOG.logVerbose("Received server response. Worker thread port: " + currentServerWorkerPort);

			// Now forward the initial response back to client
			LOG.logQuiet("Forwarding initial response to client");
			forwardPacket(serverResponsePacket);
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
	
	public void stopState() {
		this.stopping = true;
	}
	
	public void setServerWorkerPort(int port) {
		this.currentServerWorkerPort = port;
	}
	
	public void setClientAddress(InetSocketAddress clientAddress) {
		this.clientAddress = clientAddress;
	}
	
	public TFTPDatagramSocket getConnection() {
		return this.connection;
	}
	
	public InetAddress getServerAddress() {
		return this.serverAddress;
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
