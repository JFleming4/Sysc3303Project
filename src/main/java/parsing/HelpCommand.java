package parsing;

import java.util.List;

import states.InputState;
import states.State;

public class HelpCommand extends Command {
	private static final String HELP_OPERATION = "help";
	private static final String HELP_FORMAT = "";
	
	public HelpCommand(List<String> tokens) {
		super(HELP_OPERATION, HELP_FORMAT, tokens);
	}
	
	public HelpCommand(String[] tokens) {
		super(HELP_OPERATION, HELP_FORMAT, tokens);
	}
	
	@Override
	public State execute() {
		Command cmd;
		
		cmd = new ReadCommand(tokens);
		System.out.println(">> " + cmd.toHelp());
		
		cmd = new WriteCommand(tokens);
		System.out.println(">> " + cmd.toHelp());
		
		cmd = new HelpCommand(tokens);
		System.out.println(">> " + cmd.toHelp());

		cmd = new ExitCommand(tokens);
		System.out.println(">> " + cmd.toHelp());
		return new InputState();
	}
}