package states;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;

import socket.TFTPDatagramSocket;
import util.ErrorChecker;

public class LostPacketState extends ForwardState {
	public static final String MODE = "LOSE";
	private ErrorChecker checker;
	
	public LostPacketState(TFTPDatagramSocket socket, InetAddress serverAddress, ErrorChecker checker) throws SocketException {
		super(socket, serverAddress);
		this.checker = checker;
	}
	
	public LostPacketState(TFTPDatagramSocket socket, InetAddress serverAddress) throws SocketException {
		this(socket, serverAddress, null);
	}

	@Override
	public String getMode() {
		return MODE;
	}
	
	public void setErrorChecker(ErrorChecker checker) {
		this.checker = checker;
	}

	@Override
	protected void forwardPacket(DatagramPacket packet) throws IOException {
		if (checker.check(packet)) {
			LOG.logQuiet("Dropping packet.");
			LOG.logVerbose(packet);
			return;
		}
		super.forwardPacket(packet);
	}
}
