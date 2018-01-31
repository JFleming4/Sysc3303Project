package components;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;

import logging.Logger;

public class ErrorSimulator extends Thread {
	private DatagramSocket connection;
	private SocketAddress serverAddress;

	private static final int ERROR_PORT = 23;
	private static final int BUFF_HEADER_SIZE = 516;
	private static final Logger LOG = new Logger("ErrorSimulator");

	public ErrorSimulator(SocketAddress serverAddreess, boolean verbose) throws SocketException {
		this.connection = new DatagramSocket(ERROR_PORT);
		this.serverAddress = serverAddreess;
		
		if (verbose)
			Logger.setLogLevel(Logger.LogLevel.VERBOSE);
		else
			Logger.setLogLevel(Logger.LogLevel.QUIET);
		
		LOG.logQuiet("Listening on port " + ERROR_PORT);
	}
	
	public DatagramPacket sendReceiveServer(DatagramPacket clientPacket, SocketAddress serverAddress) throws IOException {
		DatagramPacket serverSend = new DatagramPacket(
				clientPacket.getData(), 
				clientPacket.getLength(), 
				serverAddress);
		DatagramPacket serverReceive = new DatagramPacket(new byte[BUFF_HEADER_SIZE], BUFF_HEADER_SIZE);
		connection.send(serverSend);
		connection.receive(serverReceive);
		return serverReceive;
	}

	public DatagramPacket receiveClient() throws IOException {
		DatagramPacket clientPacket = new DatagramPacket(new byte[BUFF_HEADER_SIZE], BUFF_HEADER_SIZE);
		connection.receive(clientPacket);
		return clientPacket;
	}

	public void stopServer() {
		connection.close();
		LOG.logQuiet("Closing");
	}

	@Override
	public void run() {
		DatagramPacket clientPacket, response;

		while(!connection.isClosed()) {
			try {
				clientPacket = receiveClient();
				response = sendReceiveServer(clientPacket, serverAddress);
				serverAddress = response.getSocketAddress();
				response.setSocketAddress(clientPacket.getSocketAddress());
				connection.send(response);
			} catch (IOException e) {
				// Ignore exception
			}
		}
	}
}

