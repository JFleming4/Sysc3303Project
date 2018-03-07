package states;

import logging.Logger;


public abstract class State {
	protected static final Logger LOG = new Logger("FTPClient");
		
	public abstract State execute();
	public void stopState() {}
}