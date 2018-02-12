import states.InputState;

public class FTPClient extends Thread {
	private states.State state;
	
	public FTPClient() {
		this.state = new InputState();
	}
	
	@Override
	public void run() {	
		while(true)
			state = state.execute();
	}
	
	public static void main(String[] args) {
		FTPClient client = new FTPClient();
		client.start();
	}
}
