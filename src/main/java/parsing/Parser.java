package parsing;

public class Parser {
	public static Command parse(String[] tokens) {
		Command cmd;
		switch(tokens[0].toUpperCase()) {
		case "READ":
			cmd = new ReadCommand(tokens);
			break;
		case "WRITE":
			cmd = new WriteCommand(tokens);
			break;
		case "EXIT":
			cmd = new ExitCommand(tokens);
			break;
		case "HELP":
		default:
			cmd = new HelpCommand(tokens);
			break;
		}
		return cmd;
	}
}