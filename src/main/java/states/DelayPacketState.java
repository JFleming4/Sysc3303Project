package states;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;

import socket.TFTPDatagramSocket;
import util.ErrorChecker;

public class DelayPacketState extends ForwardState {
	public static final String MODE = "DELAY";
	public static final int DEFAULT_DELAY = 1000;
	
	private ErrorChecker checker;
	private long delayInMilliseconds;
	
	public DelayPacketState(TFTPDatagramSocket socket, InetAddress serverAddress, ErrorChecker checker, long delayInMilliseconds) throws SocketException {
		super(socket, serverAddress);
		this.checker = checker;
		this.delayInMilliseconds = delayInMilliseconds;
	}
	
	public DelayPacketState(TFTPDatagramSocket socket, InetAddress serverAddress) throws SocketException {
		this(socket, serverAddress, null, DEFAULT_DELAY);
	}

	@Override
	public String getMode() {
		return MODE;
	}
	
	public void setErrorChecker(ErrorChecker checker) {
		this.checker = checker;
	}
	
	public void setDelay(long delay) {
		this.delayInMilliseconds = delay;
	}

	@Override
	protected void forwardPacket(DatagramPacket packet) throws IOException {
		if (checker.check(packet)) {
			LOG.logQuiet("Delaying packet by " + delayInMilliseconds + " ms.");
			LOG.logVerbose(packet);
			try {
				Thread.sleep(delayInMilliseconds);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			LOG.logQuiet("Continuing to forward packet.");
		}
		super.forwardPacket(packet);
	}
}
