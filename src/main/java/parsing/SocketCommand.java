package parsing;


import resources.ResourceManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;

import static resources.Configuration.GLOBAL_CONFIG;


public abstract class SocketCommand extends Command {
    private static final int FILENAME_IDX = 1;
    private static final int SERVER_ADDR_IDX = 2;
    private static final String SOCKET_COMMAND_FORMAT = "FILENAME SERVER_ADDRESS [--verbose|-v] [--test|-t]";
    protected static final int SOCKET_TIMEOUT = 5000;
    protected ResourceManager resourceManager;

	private enum Options {
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

	public SocketCommand(String operation, String[] tokens) throws IOException {
		super(operation, SOCKET_COMMAND_FORMAT, tokens);
		resourceManager = new ResourceManager(GLOBAL_CONFIG.CLIENT_RESOURCE_DIR);
	}

	public SocketCommand(String operation, List<String> tokens) throws IOException {
		super(operation, SOCKET_COMMAND_FORMAT, tokens);
		resourceManager = new ResourceManager(GLOBAL_CONFIG.CLIENT_RESOURCE_DIR);
	}

	public InetSocketAddress getServerAddress() throws UnknownHostException {
		if (isTest())
			return errorSimulatorAddress();
		return serverAddress(); 
	}

	public String getFilename() {
		return tokens.get(FILENAME_IDX);
	}

	public void setFileName(String fileName) {
		tokens.set(FILENAME_IDX, fileName);
	}

	public boolean isVerbose() {
		if(GLOBAL_CONFIG.DEBUG_MODE)
			return true;

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
	
	private InetSocketAddress errorSimulatorAddress() {
		return new InetSocketAddress(InetAddress.getLoopbackAddress(), GLOBAL_CONFIG.SIMULATOR_PORT);
	}
	
	private InetSocketAddress serverAddress() throws UnknownHostException {
		return new InetSocketAddress(InetAddress.getByName(tokens.get(SERVER_ADDR_IDX)), GLOBAL_CONFIG.SERVER_PORT);
	}
}
