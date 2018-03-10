package states;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import exceptions.InvalidPacketException;
import formats.AckMessage;
import formats.DataMessage;
import formats.Message;
import formats.RequestMessage;
import formats.Message.MessageType;
import socket.TFTPDatagramSocket;
import util.ErrorChecker;
import static resources.Configuration.GLOBAL_CONFIG;
public class DuplicateStateTest {
//  lose a packet; 2 : delay a packet, 3 : duplicate a
	// packet.
	private DuplicateState state;
	private TFTPDatagramSocket socket;
	private ErrorChecker checker;
	private InetAddress serverAddress;
	private InetSocketAddress serverSocketAddress;
	
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
			state = new DuplicateState(socket, serverAddress);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	@After
	public void tearDown() {
		System.setOut(System.out);
	}
	@Test
	public void testDuplicateRRQ() {
		try {
			checker = new ErrorChecker(MessageType.RRQ);
			state.setErrorChecker(checker);
			byte[] expectedRRQBytes = new RequestMessage(MessageType.RRQ, StateTestConfig.FILENAME).toByteArray();
			byte[] expectedDataBytes = new DataMessage(1, new byte[] { 0 }).toByteArray();
			DatagramPacket expectedPacket = new DatagramPacket(expectedRRQBytes, expectedRRQBytes.length, serverSocketAddress);
			DatagramPacket serverResponse = new DatagramPacket(expectedDataBytes, expectedDataBytes.length, serverSocketAddress);
			Mockito.when(socket.receive())
				.thenReturn(expectedPacket)
				.thenReturn(serverResponse)
				.thenThrow(new RuntimeException("TEST EXCEPTION"));
			
			state.execute();
			DatagramPacketMatcher matcher = new DatagramPacketMatcher(expectedPacket);
			Mockito.verify(socket, Mockito.times(2)).forwardPacket(Mockito.argThat(matcher), Mockito.eq(serverAddress), Mockito.eq(GLOBAL_CONFIG.SERVER_PORT));
					
		} catch (IOException e) {
            Assert.fail(e.getMessage());
        }
	}
	
	@Test
	public void testDuplicateWRQ() {
		try {
			checker = new ErrorChecker(MessageType.WRQ);
			state.setErrorChecker(checker);
			byte[] expectedWRQBytes = new RequestMessage(MessageType.WRQ, StateTestConfig.FILENAME).toByteArray();
			byte[] expectedDataBytes = new DataMessage(1, new byte[] { 0 }).toByteArray();
			DatagramPacket expectedPacket = new DatagramPacket(expectedWRQBytes, expectedWRQBytes.length, serverSocketAddress);
			DatagramPacket serverResponse = new DatagramPacket(expectedDataBytes, expectedDataBytes.length, serverSocketAddress);
			Mockito.when(socket.receive())
				.thenReturn(expectedPacket)
				.thenReturn(serverResponse)
				.thenThrow(new RuntimeException("TEST EXCEPTION"));
			
			state.execute();
			DatagramPacketMatcher matcher = new DatagramPacketMatcher(expectedPacket);
			Mockito.verify(socket, Mockito.times(2)).forwardPacket(Mockito.argThat(matcher), Mockito.eq(serverAddress), Mockito.eq(GLOBAL_CONFIG.SERVER_PORT));
					
		} catch (IOException e) {
            Assert.fail(e.getMessage());
        }
	}
	
	@Test
	public void testDuplicateData() {
		try {
			checker = new ErrorChecker(MessageType.DATA, 1);
			state.setErrorChecker(checker);
			byte[] expectedDataBytes = new DataMessage(1, new byte[] { 0 }).toByteArray();
			byte[] expectedACKBytes = new AckMessage(1).toByteArray();
			DatagramPacket expectedPacket = new DatagramPacket(expectedDataBytes, expectedDataBytes.length, serverSocketAddress);
			DatagramPacket serverResponse = new DatagramPacket(expectedACKBytes, expectedACKBytes.length, serverSocketAddress);
			Mockito.when(socket.receive())
				.thenReturn(expectedPacket)
				.thenReturn(serverResponse)
				.thenThrow(new RuntimeException("TEST EXCEPTION"));
			state.setServerWorkerPort(3000);
			state.setClientAddress(serverSocketAddress);
			state.execute();
			DatagramPacketMatcher matcher = new DatagramPacketMatcher(expectedPacket);
			Mockito.verify(socket, Mockito.times(2)).forwardPacket(Mockito.argThat(matcher), Mockito.eq(serverAddress), Mockito.eq(3000));
		} catch (IOException e) {
            Assert.fail(e.getMessage());
        }
	}
	
	@Test
	public void testDuplicateACK() {
		try {
			checker = new ErrorChecker(MessageType.ACK, 1);
			state.setErrorChecker(checker);
			byte[] expectedDataBytes = new DataMessage(2, new byte[] { 0 }).toByteArray();
			byte[] expectedACKBytes = new AckMessage(1).toByteArray();
			DatagramPacket serverResponse = new DatagramPacket(expectedDataBytes, expectedDataBytes.length, serverSocketAddress);
			DatagramPacket expectedPacket = new DatagramPacket(expectedACKBytes, expectedACKBytes.length, serverSocketAddress);
			Mockito.when(socket.receive())
				.thenReturn(expectedPacket)
				.thenReturn(serverResponse)
				.thenThrow(new RuntimeException("TEST EXCEPTION"));
			state.setServerWorkerPort(3000);
			state.setClientAddress(serverSocketAddress);
			state.execute();
			DatagramPacketMatcher matcher = new DatagramPacketMatcher(expectedPacket);
			Mockito.verify(socket, Mockito.times(2)).forwardPacket(Mockito.argThat(matcher), Mockito.eq(serverAddress), Mockito.eq(3000));
					
		} catch (IOException e) {
            Assert.fail(e.getMessage());
        }
	}

}

class DatagramPacketMatcher implements ArgumentMatcher<DatagramPacket> {
	private DatagramPacket packet;
	
	public DatagramPacketMatcher(DatagramPacket packet) {
		this.packet = packet;
	}
	
	@Override
	public boolean matches(DatagramPacket p) {
		try {
			return Message.parseGenericMessage(packet)
					.equals(Message.parseGenericMessage(p));
		} catch (InvalidPacketException e) {
			e.printStackTrace();
			return false;
		}
	}
}
