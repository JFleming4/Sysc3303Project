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

	public boolean isOption(int index) {
		for(Options option: Options.values())
		{
			if(tokens.get(index).equalsIgnoreCase(option.option))
				return true;
		}

		return false;
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

	/**
	 * This method gets the true argument size (ignores options)
	 * @return The number of arguments that exist within the token
	 */
	public int getArgumentsSize()
	{
		int size = 0;
		// Otherwise, iterate the tokens
		for(int i = 0; i < tokens.size(); i++)
		{
			if(!isOption(i))
				size++;
		}

		return size;
	}

	/**
	 * This method gets the true argument index (ignoring any options)
	 * This allows us to place options before and after the argument. Example:
	 * read -t -v read-test.txt
	 * @param index The relative argument index of the argument
	 * @return The argument value
	 */
	public String getArgument(int index)
	{
		// Otherwise, iterate the tokens
		for(int i = 0; i < tokens.size(); i++)
		{
			if(!isOption(i))
			{
				if(index == 0)
					return tokens.get(i);
				else
					index--;
			}
		}

		return "";
	}
	
	private InetSocketAddress errorSimulatorAddress() {
		return new InetSocketAddress(InetAddress.getLoopbackAddress(), GLOBAL_CONFIG.SIMULATOR_PORT);
	}
	
	private InetSocketAddress serverAddress() throws UnknownHostException {
		String serverAdrArg = getArgument(SERVER_ADDR_IDX);

		// Use loopback if no server address is provided
		InetAddress serverAdr = serverAdrArg.isEmpty() ? InetAddress.getLocalHost() : InetAddress.getByName(serverAdrArg);

		LOG.logVerbose("Using the following server address: " + serverAdr);


		return new InetSocketAddress(serverAdr, GLOBAL_CONFIG.SERVER_PORT);
	}
}
