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
import org.mockito.Mockito;

import formats.RequestMessage;
import formats.AckMessage;
import formats.DataMessage;
import formats.Message.MessageType;
import socket.TFTPDatagramSocket;
import util.ErrorChecker;

public class DelayPacketStateTest {
	private DelayPacketState state;
	private DelayStateThread thread;
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
			serverSocketAddress = new InetSocketAddress(InetAddress.getByName(StateTestConfig.SERVER_HOST), 1069);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		try {
			state = new DelayPacketState(socket, serverAddress);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		thread = new DelayStateThread(state);
	}
	@After
	public void tearDown() {
		System.setOut(System.out);
	}
	@Test
	public void testDelayedRRQ() {
		try {
			checker = new ErrorChecker(MessageType.RRQ);
			state.setErrorChecker(checker);

			byte[] expectedRRQBytes = new RequestMessage(MessageType.RRQ, StateTestConfig.FILENAME).toByteArray();

			DatagramPacket expectedPacket = new DatagramPacket(expectedRRQBytes, expectedRRQBytes.length, serverSocketAddress);

			Mockito.when(socket.receive())
				.thenReturn(expectedPacket)
				.thenThrow(new RuntimeException("TEST EXCEPTION"));
			
			thread.start();

			Mockito.verify(socket, Mockito.after(0).never()).forwardPacket(expectedPacket, serverAddress, 1069);
			Mockito.verify(socket, Mockito.after(DelayPacketState.DEFAULT_DELAY * 2).times(1)).forwardPacket(expectedPacket, serverAddress, 1069);
		} catch (IOException e) {
            Assert.fail(e.getMessage());
        }
	}
	
	@Test
	public void testDelayWRQ() {
		try {
			checker = new ErrorChecker(MessageType.WRQ);
			state.setErrorChecker(checker);
			byte[] expectedWRQBytes = new RequestMessage(MessageType.WRQ, StateTestConfig.FILENAME).toByteArray();

			DatagramPacket expectedPacket = new DatagramPacket(expectedWRQBytes, expectedWRQBytes.length, serverSocketAddress);

			Mockito.when(socket.receive())
				.thenReturn(expectedPacket)
				.thenThrow(new RuntimeException("TEST EXCEPTION"));
			
			thread.start();
			
			Mockito.verify(socket, Mockito.after(0).never()).forwardPacket(expectedPacket, serverAddress, 1069);
			Mockito.verify(socket, Mockito.after(DelayPacketState.DEFAULT_DELAY * 2).times(1)).forwardPacket(expectedPacket, serverAddress, 1069);	
		} catch (IOException e) {
            Assert.fail(e.getMessage());
        }
	}
	
	@Test
	public void testLostData() {
		try {
			checker = new ErrorChecker(MessageType.DATA, 1);
			state.setErrorChecker(checker);
			byte[] expectedDataBytes = new DataMessage(1, new byte[] { 0 }).toByteArray();

			DatagramPacket expectedPacket = new DatagramPacket(expectedDataBytes, expectedDataBytes.length, serverSocketAddress);

			Mockito.when(socket.receive())
				.thenReturn(expectedPacket)
				.thenThrow(new RuntimeException("TEST EXCEPTION"));
			
			state.setServerWorkerPort(3000);
			state.setClientAddress(serverSocketAddress);
			
			thread.start();
			
			Mockito.verify(socket, Mockito.after(0).never())
				.forwardPacket(Mockito.eq(expectedPacket), Mockito.eq(serverAddress), Mockito.eq(3000));
			Mockito.verify(socket, Mockito.after(DelayPacketState.DEFAULT_DELAY * 2).times(1))
				.forwardPacket(Mockito.eq(expectedPacket), Mockito.eq(serverAddress), Mockito.eq(3000));
		} catch (IOException e) {
            Assert.fail(e.getMessage());
        }
	}

	@Test
	public void testLostACK() {
		try {
			checker = new ErrorChecker(MessageType.ACK, 1);
			state.setErrorChecker(checker);

			byte[] expectedACKBytes = new AckMessage(1).toByteArray();

			DatagramPacket expectedPacket = new DatagramPacket(expectedACKBytes, expectedACKBytes.length, serverSocketAddress);
			Mockito.when(socket.receive())
				.thenReturn(expectedPacket)
				.thenThrow(new RuntimeException("TEST EXCEPTION"));

			state.setServerWorkerPort(3000);
			state.setClientAddress(serverSocketAddress);
			
			thread.start();
			
			Mockito.verify(socket, Mockito.after(0).never())
				.forwardPacket(Mockito.eq(expectedPacket), Mockito.eq(serverAddress), Mockito.eq(3000));
			Mockito.verify(socket, Mockito.after(DelayPacketState.DEFAULT_DELAY * 2).times(1))
				.forwardPacket(Mockito.eq(expectedPacket), Mockito.eq(serverAddress), Mockito.eq(3000));
		} catch (IOException e) {
            Assert.fail(e.getMessage());
        }
	}
}

class DelayStateThread extends Thread {
	private DelayPacketState state;
	public DelayStateThread(DelayPacketState state) {
		this.state = state;
	}
	
	@Override
	public void run() {
		state.execute();
	}
}
