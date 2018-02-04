package parsing;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import logging.Logger;


public abstract class SocketCommand extends Command {
    private static final int FILENAME_IDX = 1;
    private static final int SERVER_ADDR_IDX = 2;
    public static final int SERVER_ADDR_PORT = 69;
    public static final int ERROR_PORT = 23;
    private static final String SOCKET_COMMAND_FORMAT = "FILENAME SERVER_ADDRESS [--verbose|-v] [--test|-t]";
    protected static final int BUFF_HEADER_SIZE = 516;
    protected static final String RESOURCE_DIR = "client";
    protected static final int SOCKET_TIMETOUT = 5000;

	private static enum Options {
		VERBOSE_LONG("--verbose"),
		VERBOSE_SHORT("-v"),
		TEST_LONG("--test"),
		TEST_SHORT("-t");

		private String option;

		Options(String option) {
			this.option = option;
		}

		public static boolean isVerbose(String option) {
			if (option.equals(VERBOSE_SHORT.option))
				return true;
			else if (option.equals(VERBOSE_LONG.option))
				return true;
			return false;
		}

		public static boolean isTest(String option) {
			if (option.equals(TEST_SHORT.option))
				return true;
			else if (option.equals(TEST_LONG.option))
				return true;
			return false;
		}
	}

	public SocketCommand(String operation, String[] tokens) {
		super(operation, SOCKET_COMMAND_FORMAT, tokens);
	}

	public SocketCommand(String operation, List<String> tokens) {
		super(operation, SOCKET_COMMAND_FORMAT, tokens);
	}

	public InetSocketAddress getServerAddress() throws UnknownHostException {
		if (isTest())
			return errorSimulatorAddress();
		return serverAddress(); 
	}

	public String getFilename() {
		return tokens.get(FILENAME_IDX);
	}

	public boolean isVerbose() {
		for (String option : tokens) {
			if (Options.isVerbose(option))
				return true;
		}
		return false;
	}

	public boolean isTest() {
		for (String option : tokens) {
			if (Options.isTest(option))
				return true;
		}
		return false;
	}
	
	@Override
	protected void setLogLevel() {
		if (isVerbose())
			Logger.setLogLevel(Logger.LogLevel.VERBOSE);
		else
			Logger.setLogLevel(Logger.LogLevel.QUIET);
	}
	
	private InetSocketAddress errorSimulatorAddress() throws UnknownHostException {
		return new InetSocketAddress(InetAddress.getByName(tokens.get(SERVER_ADDR_IDX)), ERROR_PORT);
	}
	
	private InetSocketAddress serverAddress() throws UnknownHostException {
		return new InetSocketAddress(InetAddress.getByName(tokens.get(SERVER_ADDR_IDX)), SERVER_ADDR_PORT);
	}
}
