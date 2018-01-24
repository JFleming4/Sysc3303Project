package parsing;

import java.util.List;

public class WriteCommand extends SocketCommand {
	private static final String WRITE_OPERATION = "write";
	
	public WriteCommand(List<String> tokens) {
		super(WRITE_OPERATION, tokens);
	}
	
	public WriteCommand(String[] tokens) {
		super(WRITE_OPERATION, tokens);
	}

	@Override
	public void execute() {
		
	}
}
