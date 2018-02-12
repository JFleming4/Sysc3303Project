package parsing;

import logging.Logger;
import states.InputState;
import states.ReadState;
import states.State;

import java.net.UnknownHostException;
import java.nio.file.AccessDeniedException;
import java.util.List;

public class ReadCommand extends SocketCommand {
	private static final Logger LOG = new Logger("FTPClient - Read");
	private static final String READ_OPERATION = "read";

	public ReadCommand(List<String> tokens) {
		super(READ_OPERATION, tokens);
	}

	public ReadCommand(String[] tokens) {
		super(READ_OPERATION, tokens);
	}

	@Override
	public State execute() {
		if (this.tokens.size() < 3)
			LOG.logQuiet("Error: Not enough arguments");
		else
			try {

				return new ReadState(getServerAddress(), getFilename(), isVerbose());
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		return new InputState();
	}
}
