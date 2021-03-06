package parsing;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import logging.Logger;
import states.State;

import static resources.Configuration.GLOBAL_CONFIG;


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
		setLogLevel();
	}
	
	public abstract State execute() throws IOException;
	
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
	
	protected void setLogLevel() {
		if(GLOBAL_CONFIG.DEBUG_MODE)
			Logger.setLogLevel(Logger.LogLevel.VERBOSE);
		else
			Logger.setLogLevel(Logger.LogLevel.QUIET);

	}
}