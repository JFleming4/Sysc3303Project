package parsing;

import java.io.IOException;
import java.util.List;

import logging.Logger;
import states.InputState;
import states.ReadState;
import states.State;

public class ReadCommand extends SocketCommand {
	private static final Logger LOG = new Logger("FTPClient - Read");
	private static final String READ_OPERATION = "read";


	public ReadCommand(List<String> tokens) throws IOException {
		super(READ_OPERATION, tokens);
	}

	public ReadCommand(String[] tokens) throws IOException {
		super(READ_OPERATION, tokens);
	}

	@Override
	public State execute() {
		if (this.tokens.size() < 2)
			LOG.logQuiet("Error: Not enough arguments");
		else
			try {

				return new ReadState(getServerAddress(), resourceManager, getFilename(), isVerbose());
			} catch (IOException e) {
				e.printStackTrace();

				// Flush stderr so it doesn't get mangled with stdout
				System.err.flush();
			}

		return new InputState();
	}

}
