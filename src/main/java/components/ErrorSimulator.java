package components;
import java.io.IOException;
import java.net.*;
import java.util.NoSuchElementException;
import java.util.Scanner;

import logging.Logger;

public class ErrorSimulator extends Thread {
	private DatagramSocket connection;
	private InetAddress serverAddress;
	private InetSocketAddress clientAddress;

	private static final int ERROR_PORT = 23;
	private static final int SERVER_PORT = 69;
	private static final int BUFF_HEADER_SIZE = 516;
	private int currentServerWorkerPort;
	private static final Logger LOG = new Logger("ErrorSimulator");

	public ErrorSimulator(InetAddress serverAddress) throws SocketException {
		this.connection = new DatagramSocket(ERROR_PORT);
		this.serverAddress = serverAddress;

		LOG.logQuiet("Listening on port " + ERROR_PORT);
		LOG.logQuiet("Server Address: " + serverAddress);
	}

	public void forwardPacket(DatagramPacket clientPacket, InetAddress address, int port) throws IOException {
		DatagramPacket sendPacket = new DatagramPacket(
				clientPacket.getData(),
				clientPacket.getLength(),
				address,
				port);

		LOG.logVerbose("Sending packet to address: " + address + ", Port: " + port);
		LOG.logVerbose(sendPacket);
		connection.send(sendPacket);
	}

	public void forwardPacket(DatagramPacket clientPacket, InetSocketAddress address) throws IOException {
		forwardPacket(clientPacket, address.getAddress(), address.getPort());
	}

	public DatagramPacket receivePacket() throws IOException {
		DatagramPacket clientPacket = new DatagramPacket(new byte[BUFF_HEADER_SIZE], BUFF_HEADER_SIZE);
		connection.receive(clientPacket);
		LOG.logVerbose("Received Message Packet from " + clientPacket.getSocketAddress());
		LOG.logVerbose(clientPacket);
		return clientPacket;
	}

	public void stopServer() {
		connection.close();
		LOG.logQuiet("The simulator connection has been closed.");
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

	@Override
	public void run() {
		DatagramPacket incomingPacket;

		LOG.logQuiet("Error Simulator is running.");
		while (!connection.isClosed()) {
			try {
				LOG.logVerbose("Waiting for request from client");
				incomingPacket = receivePacket();
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
					forwardPacket(incomingPacket, serverAddress, SERVER_PORT);

					LOG.logVerbose("Waiting for initial response from server");
					DatagramPacket serverResponsePacket = receivePacket();

					// Now we learn the server workers PORT
					currentServerWorkerPort = serverResponsePacket.getPort();
					LOG.logVerbose("Received server response. Worker thread port: " + currentServerWorkerPort);

					// Now forward the initial response back to client
					forwardPacket(serverResponsePacket, clientAddress);
					LOG.logQuiet("Forwarding initial response to client");
				}
				// If the packet is from the server worker
				// We are going to forward the packet to the current client
				else if(!isFromClient(incomingAddr, incomingPort) && isFromServerWorker(incomingAddr, incomingPort))
				{
					LOG.logQuiet("Received message from client. Forwarding to server.");
					forwardPacket(incomingPacket, clientAddress);
				}
				// If the packet is from the current client
				// We are going to forward the packet to the server
				else if(!isFromServerWorker(incomingAddr, incomingPort) && isFromClient(incomingAddr, incomingPort))
				{
					LOG.logQuiet("Received message from server. Forwarding to client.");
					forwardPacket(incomingPacket, serverAddress, currentServerWorkerPort);
				}

			} catch (SocketException sE)
			{
				// Socket closed exception
				if(!connection.isClosed())
					sE.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Prints CLI help
	 */
	private static void printHelp() {
		System.out.println("Command Line Arguments:");
		System.out.println("ErrorSimulator [-s server_ip] [-v]");
		System.out.println("\t[-h]: Shows Help page");
		System.out.println("\t[-s server_address]: Where server_address is the server's IP Address. Default is localhost. Port: " + SERVER_PORT);
		System.out.println("\t[-v]: Sets logging to verbose");
	}

	/**
	 * Parses the server address from the command line arguments
	 * @param args Command Line Arguments
	 * @return The SocketAddress to use for the connection to the server.
	 */
	private static InetAddress getServerAddress(String[] args)
	{
		// Create default socket address
		InetAddress serverAddr = InetAddress.getLoopbackAddress();

		if(args == null || args.length < 2)
			return serverAddr;

		int optionIndex = getOption(args, 's');

		// Make sure server option exists
		if(optionIndex == -1)
		{
			LOG.logQuiet("No server arguments provided. Using default server address.");
			return serverAddr;
		}

		// Read next argument
		if(optionIndex + 1 == args.length) {
			LOG.logQuiet("Missing server address argument for option -s. Using default server address.");
			return serverAddr;
		}

		// Create address out of parameter value
		return new InetSocketAddress(args[optionIndex + 1], SERVER_PORT).getAddress();
	}

	/**
	 * @return The index of a specified option argument, or -1 if not found
	 */
	private static int getOption(String[] args, char option)
	{
		if(args == null || args.length < 1)
			return -1;

		// Iterate through arguments to get -v option
		for (int i = 0; i < args.length; i++) {

			if (args[i].equalsIgnoreCase("-" + option)) {
				return i;
			}
		}
		return -1;
	}

	public static void main(String[] args) {

		// Check for help arg
		if(getOption(args, 'h') > -1)
		{
			printHelp();
			return;
		}

		ErrorSimulator errorSim;

		// Set verbosity
		Logger.LogLevel level = getOption(args, 'v') > -1 ? Logger.LogLevel.VERBOSE : Logger.LogLevel.QUIET;
		Logger.setLogLevel(level);

		LOG.logQuiet("Current Log Level: " + Logger.getLogLevel().name());

		try {

			errorSim = new ErrorSimulator(getServerAddress(args));
			errorSim.start();

			boolean runSim = true;
			Scanner input = new Scanner(System.in);
			System.out.println("Type 'help' for a list of commands");

			// Prompt the user for input and check to see if they would like to shutdown the server
			while (runSim) {
				System.out.print(">> ");
				String command = input.nextLine();
				if (command.trim().isEmpty())
					continue;

				switch (command.toLowerCase()) {
					case "exit":
					case "stop":
						LOG.logQuiet("Stopping error simulator");
						runSim = false;
						errorSim.stopServer();
						// Join the server thread to wait until it is finished
						errorSim.join();
						LOG.logQuiet("Simulator stopped successfully.");
						break;
					case "l":
						level = Logger.getLogLevel().equals(Logger.LogLevel.QUIET) ? Logger.LogLevel.VERBOSE : Logger.LogLevel.QUIET;
						Logger.setLogLevel(level);
						LOG.logQuiet("Set Log Level to: " + Logger.getLogLevel().name());
						break;
					case "help":
						System.out.println("Commands:\n'exit' -> Shutdown the simulator\n'l' -> Toggle verbose / quiet logging");
						break;
					default:
						System.out.println("'" + command + "' is not a valid command.");
						System.out.println("Type 'help' for a list of commands");
						break;
				}
			}

			input.close();

		} catch (SocketException | InterruptedException sE) {
			sE.printStackTrace();
		} catch (NoSuchElementException nSEE)
		{
			// Ignore this. Result of empty line buffer in scanner when exiting
		}
	}
}
