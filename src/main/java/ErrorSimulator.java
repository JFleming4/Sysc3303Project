import logging.Logger;
import socket.TFTPDatagramSocket;
import states.ExitState;
import states.ForwardState;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.NoSuchElementException;
import java.util.Scanner;

import static resources.Configuration.GLOBAL_CONFIG;

public class ErrorSimulator extends Thread {
	private TFTPDatagramSocket connection;
	private states.State state;
	
	private static final Logger LOG = new Logger("ErrorSimulator");

	public ErrorSimulator(InetAddress serverAddress) throws SocketException {
		this.connection = new TFTPDatagramSocket(GLOBAL_CONFIG.SIMULATOR_PORT);
		setState(new ForwardState(connection, serverAddress));

		LOG.logQuiet("Listening on port " + connection.getLocalPort());
		LOG.logQuiet("Server Address: " + serverAddress);
	}

	public void stopServer() {
		connection.close();
		setState(new ExitState());
		LOG.logQuiet("The simulator connection has been closed.");
	}
	
	public void setState(states.State state) {
		this.state = state;
	}

	@Override
	public void run() {
		while(true)
			state.execute();
	}

	/**
	 * Prints CLI help
	 */
	private static void printHelp() {
		System.out.println("Command Line Arguments:");
		System.out.println("ErrorSimulator [-s server_ip] [-v]");
		System.out.println("\t[-h]: Shows Help page");
		System.out.println("\t[-s server_address]: Where server_address is the server's IP Address. Default is localhost. Port: " + GLOBAL_CONFIG.SERVER_PORT);
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
		return new InetSocketAddress(args[optionIndex + 1], GLOBAL_CONFIG.SERVER_PORT).getAddress();
	}

	/**
	 * @return The index of a specified option argument, or -1 if not found
	 */
	private static int getOption(String[] args, char option)
	{
		if(args == null || args.length < 1)
			return -1;

		// Iterate through arguments to get option index
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

		// Override log level to VERBOSE on debug mode
		if(GLOBAL_CONFIG.DEBUG_MODE)
			Logger.setLogLevel(Logger.LogLevel.VERBOSE);

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
