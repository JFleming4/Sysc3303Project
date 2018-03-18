package states;

import formats.AckMessage;
import formats.DataMessage;
import formats.Message;
import formats.RequestMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import exceptions.InvalidPacketException;
import socket.TFTPDatagramSocket;
import util.ErrorChecker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.*;

import static resources.Configuration.GLOBAL_CONFIG;

public class InvalidOpCodeStateTest {
    private InvalidOpCodeState state;
    private TFTPDatagramSocket socket;
    private ErrorChecker checker;
    private InetAddress serverAddress;
    private InetSocketAddress serverSocketAddress;
    private byte[] invalidOPData;

    @Before
    public void setup() {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outStream));
        socket = Mockito.mock(TFTPDatagramSocket.class);
        try {
            serverAddress = InetAddress.getByName(StateTestConfig.SERVER_HOST);
            serverSocketAddress = new InetSocketAddress(InetAddress.getByName(StateTestConfig.SERVER_HOST), GLOBAL_CONFIG.SERVER_PORT);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            state = new InvalidOpCodeState(socket, serverAddress);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
    @After
    public void tearDown() {
        System.setOut(System.out);
    }

    @Test
    public void testInvalidRRQ() {
        try {
            checker = new ErrorChecker(Message.MessageType.RRQ);
            state.setErrorChecker(checker);
            byte[] expectedRRQBytes = new RequestMessage(Message.MessageType.RRQ, StateTestConfig.FILENAME).toByteArray();
            invalidOPData = duplicateDataInvOp(expectedRRQBytes);
            DatagramPacket expectedPacket = new DatagramPacket(expectedRRQBytes, expectedRRQBytes.length, serverSocketAddress);
            DatagramPacket invalidPacket = new DatagramPacket(invalidOPData, invalidOPData.length, serverSocketAddress);
            Mockito.when(socket.receive())
                    .thenReturn(expectedPacket)
                    .thenThrow(new RuntimeException("TEST EXCEPTION"));

            state.execute();
            InvalidPacketMatcher matcher = new InvalidPacketMatcher(invalidPacket);
            Mockito.verify(socket, Mockito.times(0)).forwardPacket(expectedPacket, serverAddress, GLOBAL_CONFIG.SERVER_PORT);
            Mockito.verify(socket, Mockito.times(1)).forwardPacket(Mockito.argThat(matcher), Mockito.eq(serverAddress), Mockito.eq(GLOBAL_CONFIG.SERVER_PORT));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testInvalidWRQ() {
        try {
            checker = new ErrorChecker(Message.MessageType.WRQ);
            state.setErrorChecker(checker);
            byte[] expectedWRQBytes = new RequestMessage(Message.MessageType.WRQ, StateTestConfig.FILENAME).toByteArray();
            invalidOPData = duplicateDataInvOp(expectedWRQBytes);
            DatagramPacket expectedPacket = new DatagramPacket(expectedWRQBytes, expectedWRQBytes.length, serverSocketAddress);
            DatagramPacket invalidPacket = new DatagramPacket(invalidOPData, invalidOPData.length, serverSocketAddress);
            Mockito.when(socket.receive())
                    .thenReturn(expectedPacket)
                    .thenThrow(new RuntimeException("TEST EXCEPTION"));

            state.execute();
            InvalidPacketMatcher matcher = new InvalidPacketMatcher(invalidPacket);
            Mockito.verify(socket, Mockito.times(0)).forwardPacket(expectedPacket, serverAddress, GLOBAL_CONFIG.SERVER_PORT);
            Mockito.verify(socket, Mockito.times(1)).forwardPacket(Mockito.argThat(matcher), Mockito.eq(serverAddress), Mockito.eq(GLOBAL_CONFIG.SERVER_PORT));

        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testInvalidData() {
        try {
            checker = new ErrorChecker(Message.MessageType.DATA, 1);
            state.setErrorChecker(checker);
            byte[] expectedDataBytes = new DataMessage(1, new byte[] { 0 }).toByteArray();
            invalidOPData = duplicateDataInvOp(expectedDataBytes);
            DatagramPacket expectedPacket = new DatagramPacket(expectedDataBytes, expectedDataBytes.length, serverSocketAddress);
            DatagramPacket invalidPacket = new DatagramPacket(invalidOPData, invalidOPData.length, serverSocketAddress);
            Mockito.when(socket.receive())
                    .thenReturn(expectedPacket)
                    .thenThrow(new RuntimeException("TEST EXCEPTION"));

            state.setServerWorkerPort(3000);
            state.setClientAddress(serverSocketAddress);
            state.execute();

            InvalidPacketMatcher matcher = new InvalidPacketMatcher(invalidPacket);
            Mockito.verify(socket, Mockito.times(0)).forwardPacket(Mockito.eq(expectedPacket), Mockito.eq(serverAddress), Mockito.eq(3000));
            Mockito.verify(socket, Mockito.times(1)).forwardPacket(Mockito.argThat(matcher), Mockito.eq(serverAddress), Mockito.eq(3000));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testInvalidACK() {
        try {
            checker = new ErrorChecker(Message.MessageType.ACK, 1);
            state.setErrorChecker(checker);

            byte[] expectedACKBytes = new AckMessage(1).toByteArray();
            invalidOPData = duplicateDataInvOp(expectedACKBytes);
            
            DatagramPacket expectedPacket = new DatagramPacket(expectedACKBytes, expectedACKBytes.length, serverSocketAddress);
            DatagramPacket invalidPacket = new DatagramPacket(invalidOPData, invalidOPData.length, serverSocketAddress);
            Mockito.when(socket.receive())
                    .thenReturn(expectedPacket)
                    .thenThrow(new RuntimeException("TEST EXCEPTION"));

            state.setServerWorkerPort(3000);
            state.setClientAddress(serverSocketAddress);
            state.execute();

            InvalidPacketMatcher matcher = new InvalidPacketMatcher(invalidPacket);
            Mockito.verify(socket, Mockito.times(0)).forwardPacket(Mockito.eq(expectedPacket), Mockito.eq(serverAddress), Mockito.eq(3000));
            Mockito.verify(socket, Mockito.times(1)).forwardPacket(Mockito.argThat(matcher), Mockito.eq(serverAddress), Mockito.eq(3000));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
    
    public byte[] duplicateDataInvOp(byte[] arr) {
    	byte [] arrVal = new byte[arr.length];
    	for(int i = 0; i < arr.length; i++) {
    		if( i != 1) {
    			arrVal[i] = arr[i];
    		} else {
    			arrVal[i] = 7;
    		}
    	}
    	return arrVal;
    }

}

class InvalidPacketMatcher implements ArgumentMatcher<DatagramPacket> {
	private DatagramPacket packet;
	
	public InvalidPacketMatcher(DatagramPacket packet) {
		this.packet = packet;
	}
	
	@Override
	public boolean matches(DatagramPacket p) {
		byte [] packetData = packet.getData();
		byte [] pData = p.getData();
		if( p.getLength() == packet.getLength() ) {
			for(int i = 0; i < p.getLength(); i++) {
				if(pData[i] != packetData[i]) return false;
			}
			return true;
		}
		return false;
	}
}
