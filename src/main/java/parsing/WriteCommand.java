package parsing;

import logging.Logger;
import states.InputState;
import states.State;
import states.WriteState;

import java.io.IOException;
import java.util.List;

public class WriteCommand extends SocketCommand {
    private static final Logger LOG = new Logger("FTPClient - Write");
	private static final String WRITE_OPERATION = "write";

	public WriteCommand(List<String> tokens) throws IOException {
		super(WRITE_OPERATION, tokens);
	}

	public WriteCommand(String[] tokens) throws IOException {
		super(WRITE_OPERATION, tokens);
	}

	@Override
	public State execute() {
		if (this.tokens.size() < 2)
			LOG.logQuiet("Error: Not enough arguments");
		else
			try {
				return new WriteState(getServerAddress(), resourceManager, getFilename(), isVerbose());
			} catch (IOException e) {
				e.printStackTrace();

				// Flush stderr so it doesn't get mangled with stdout
				System.err.flush();
			}

		return new InputState();
	}
}
