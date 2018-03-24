package states;

import socket.TFTPDatagramSocket;
import util.ErrorChecker;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;

public class InvalidTIDState extends ForwardState{
	public static final String MODE = "INVTID";
    private ErrorChecker checker;
    private TFTPDatagramSocket invalidTIDSocket;

    public InvalidTIDState(TFTPDatagramSocket socket, InetAddress serverAddress, ErrorChecker checker) throws SocketException {
        super(socket, serverAddress);
        this.checker = checker;
    }

    public InvalidTIDState(TFTPDatagramSocket socket, InetAddress serverAddress) throws SocketException {
        this(socket, serverAddress, null);
    }

    public void setInvalidTIDSocket(TFTPDatagramSocket invalidTIDSocket) {
        this.invalidTIDSocket = invalidTIDSocket;
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
            LOG.logQuiet("Sending packet with invalid TID.");
            LOG.logVerbose(packet);
            if (invalidTIDSocket == null) invalidTIDSocket = new TFTPDatagramSocket();
            invalidTIDSocket.forwardPacket(packet,packet.getAddress(), packet.getPort());
            invalidTIDSocket.receive();
            LOG.logQuiet("Discarding packet with invalid TID.");
            invalidTIDSocket.close();
            invalidTIDSocket = null;
        }
        super.forwardPacket(packet);
    }


}
