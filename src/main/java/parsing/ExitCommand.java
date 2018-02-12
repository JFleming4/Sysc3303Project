package parsing;

import java.util.List;

import states.ExitState;
import states.State;

public class ExitCommand extends Command {
	private static final String EXIT_OPERATION = "exit";
	private static final String EXIT_FORMAT = "";
	
	public ExitCommand(List<String> tokens) {
		super(EXIT_OPERATION, EXIT_FORMAT, tokens);
	}
	
	public ExitCommand(String[] tokens) {
		super(EXIT_OPERATION, EXIT_FORMAT, tokens);
	}
	
	@Override
	public State execute() {
		LOG.logQuiet("Shutting down.");
		return new ExitState();
	}
}
