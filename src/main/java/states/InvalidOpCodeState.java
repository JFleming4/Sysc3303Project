package states;

import socket.TFTPDatagramSocket;
import util.ErrorChecker;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;

public class InvalidOpCodeState extends ForwardState{
    private ErrorChecker checker;

    public InvalidOpCodeState(TFTPDatagramSocket socket, InetAddress serverAddress, ErrorChecker checker) throws SocketException {
        super(socket, serverAddress);
        this.checker = checker;
    }

    public InvalidOpCodeState(TFTPDatagramSocket socket, InetAddress serverAddress) throws SocketException {
        this(socket, serverAddress, null);
    }

    public void setErrorChecker(ErrorChecker checker) {
        this.checker = checker;
    }

    @Override
    protected void forwardPacket(DatagramPacket packet) throws IOException {
        if (checker.check(packet)) {
            LOG.logQuiet("Sending Invalid OpCode.");
            LOG.logVerbose(packet);
            byte [] data = new byte[] {0,7,0,1};
            super.forwardPacket(new DatagramPacket(data, data.length, packet.getSocketAddress()));
        }
        super.forwardPacket(packet);
    }


}
