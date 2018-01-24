package parsing;

import java.util.Arrays;
import java.util.List;

import logging.Logger;


public abstract class Command {
	protected static final Logger LOG = new Logger("FTPClient");
	private static final int OPERATION_IDX = 0;

	protected final String operation;

	protected final String format;
	protected final List<String> tokens;
	
	public Command(String operation, String format, String[] tokens) {
		this(operation, format, Arrays.asList(tokens));
	}
	
	public Command(String operation, String format, List<String> tokens) {
		this.operation = operation;
		this.format = format;
		this.tokens = tokens;
	}
	
	public abstract void execute();
	
	public String getOperation() {
		return operation;
	}
	
	public boolean isValid() {
		return tokens.get(OPERATION_IDX).equals(operation);
	}
	
	public String toHelp() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(operation);
		buffer.append(" ");
		buffer.append(format);
		return buffer.toString();
	}
}