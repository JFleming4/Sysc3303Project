package parsing;

import logging.Logger;
import states.InputState;
import states.ReadState;
import states.State;
import states.StateOfDepression;

import java.io.IOException;
import java.util.List;

/**
 * Test Command Class for Duplicate DATA messages (pre-error sim integration)
 */
public class DepressedCommand extends SocketCommand {
	private static final Logger LOG = new Logger("Depresso");
	private static final String READ_OPERATION = "depresso";


	public DepressedCommand(List<String> tokens) throws IOException {
		super(READ_OPERATION, tokens);
	}

	public DepressedCommand(String[] tokens) throws IOException {
		super(READ_OPERATION, tokens);
	}

	@Override
	public State execute() {
		try {
			return new StateOfDepression(getServerAddress(), resourceManager, getFilename(), isVerbose());
		} catch (IOException e) {
			e.printStackTrace();

			// Flush stderr so it doesn't get mangled with stdout
			System.err.flush();
		}

		return new InputState();
	}
}
