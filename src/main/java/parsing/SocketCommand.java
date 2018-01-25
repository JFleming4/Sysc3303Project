package parsing;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

public abstract class SocketCommand extends Command {
	private static final int FILENAME_IDX = 1;
	private static final int SERVER_ADDR_IDX = 2;
	public static final int SERVER_ADDR_PORT = 69;
	private static final String SOCKET_COMMAND_FORMAT = "FILENAME SERVER_ADDRESS [--verbose|-v] [--test|-t]";
	
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
	
	public SocketAddress getServerAddress() {
		return new InetSocketAddress(tokens.get(SERVER_ADDR_IDX), SERVER_ADDR_PORT);
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
}