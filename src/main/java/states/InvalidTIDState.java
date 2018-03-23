package states;

import socket.TFTPDatagramSocket;
import util.ErrorChecker;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;

import static resources.Configuration.GLOBAL_CONFIG;

public class InvalidTIDState extends ForwardState{
	public static final String MODE = "INVTID";
    private ErrorChecker checker;

    public InvalidTIDState(TFTPDatagramSocket socket, InetAddress serverAddress, ErrorChecker checker) throws SocketException {
        super(socket, serverAddress);
        this.checker = checker;
    }

    public InvalidTIDState(TFTPDatagramSocket socket, InetAddress serverAddress) throws SocketException {
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
            LOG.logQuiet("Sending packet with invalid TID.");
            LOG.logVerbose(packet);
            TFTPDatagramSocket invalidTIDSocket = new TFTPDatagramSocket(GLOBAL_CONFIG.SIMULATOR_PORT+500);
            invalidTIDSocket.forwardPacket(packet,packet.getAddress(), packet.getPort());
            invalidTIDSocket.receive();
            LOG.logQuiet("Discarding packet with invalid TID.");
            invalidTIDSocket.close();
        }
        super.forwardPacket(packet);
    }


}
