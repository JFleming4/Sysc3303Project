package parsing;

import java.util.List;

public class ReadCommand extends SocketCommand {
	private static final String READ_OPERATION = "read";
	
	public ReadCommand(List<String> tokens) {
		super(READ_OPERATION, tokens);
	}
	
	public ReadCommand(String[] tokens) {
		super(READ_OPERATION, tokens);
	}

	@Override
	public void execute() {
		
	}
}
