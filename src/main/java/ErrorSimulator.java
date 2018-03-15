import logging.Logger;
import parsing.Parser;
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
		this(new TFTPDatagramSocket(GLOBAL_CONFIG.SIMULATOR_PORT), serverAddress);
	}
	
	public ErrorSimulator(TFTPDatagramSocket socket, InetAddress serverAddress) throws SocketException {
		this.connection = socket;
		this.state = new ForwardState(connection, serverAddress);
		
		LOG.logQuiet("Listening on port " + connection.getLocalPort());
		LOG.logQuiet("Server Address: " + connection.getInetAddress());
	}

	public void stopServer() {
		connection.close();
		setState(new ExitState());
		LOG.logQuiet("The simulator connection has been closed.");
	}
	
	public ForwardState getSimulatorState() {
		if (state instanceof ForwardState)
			return (ForwardState) this.state;
		return null;
	}
	
	public void setState(states.State state) {
		this.state.stopState();
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
		ForwardState state;

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
			state = errorSim.getSimulatorState();
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
						System.out.println(toHelp());
						break;
					default:
						state = Parser.parseStateInformation(
								command.split(" "),
								errorSim.getSimulatorState().getConnection(),
								errorSim.getSimulatorState().getServerAddress()
								);
						if (state != null) errorSim.setState(state);;
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
	
	public static String toHelp() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("\n==== Commands: ====\n");
		buffer.append("exit -> Shutdown the simulator\n");
		buffer.append("l -> Toggle verbose / quiet logging\n");
		buffer.append("\n==== Error Mode States ====\n");
		buffer.append("normal\n");
		buffer.append("dup TYPE [BLOCK_NUM] [REPEAT_INTERVAL]\n");
		buffer.append("lose TYPE [BLOCK_NUM] [REPEAT_INTERVAL]\n");
		buffer.append("delay TYPE [BLOCK_NUM] [REPEAT_INTERVAL] DELAY_IN_MILLISECONDS\n");
		buffer.append("extend TYPE [BLOCK_NUM] [REPEAT_INTERVAL]\n");
		buffer.append("\n==== Packet Types for Error Mode States ====\n");
		buffer.append("ack, data, rrq, wrq\n");
		buffer.append("\n==== Example Commands for Error Mode States ====\n");
		buffer.append("dup ack 4 2 - Duplicate every second packet beginning with number 4.\n");
		buffer.append("lose data 1 - Lose the first data packet.\n");
		buffer.append("delay rrq 6000 - Delay RRQ by 6 seconds.\n");
		buffer.append("delay ack 1 4 6000 - Delay every 4th Ack by 6 seconds.\n");
		buffer.append("extend data 1 4 - Extend every 4th Data Message with fake data.\n");
		return buffer.toString();
	}
}
