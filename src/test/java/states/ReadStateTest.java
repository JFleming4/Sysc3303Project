package states;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import formats.*;
import formats.Message.MessageType;
import resources.ResourceManager;
import socket.TFTPDatagramSocket;

public class ReadStateTest {

	private static final String FILENAME = "file.txt";
    private static final String FILE_SHORT = "Hello World";
    private static final String SERVER_HOST = "localhost";

	private TFTPDatagramSocket socket;
	private ResourceManager resourceManager;
    private InetSocketAddress serverAddress;
    private InetSocketAddress connectionManagerSocketAddress;

	@Before
	public void setUp() {
	    socket = Mockito.mock(TFTPDatagramSocket.class);
	    resourceManager = Mockito.mock(ResourceManager.class);
        try {
            serverAddress = new InetSocketAddress(InetAddress.getByName(SERVER_HOST), 69);
            connectionManagerSocketAddress = new InetSocketAddress(InetAddress.getByName(SERVER_HOST), 1069);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
	}

	@Test
	public void shortReadRequestSuccess() {
	    try {

	        // Command for RRQ and expected RRQ
            int responseBlockNum = 1;

            // Mocked and expected Messages to be passed in 1 block read file transfer
            RequestMessage expectedRRQ = new RequestMessage(MessageType.RRQ,FILENAME);
            DataMessage mockedData = new DataMessage(responseBlockNum,FILE_SHORT.getBytes());
            AckMessage expectedAck = new AckMessage(responseBlockNum);

            // Mock mechanism to capture arguments in order
            InOrder inOrder = Mockito.inOrder(socket);
            ArgumentCaptor <Message> argument = ArgumentCaptor.forClass(Message.class);

            // Return the mock response on receivePacket
            Mockito.when(socket.receivePacket()).thenReturn(new DatagramPacket(mockedData.toByteArray(), mockedData.toByteArray().length, connectionManagerSocketAddress));

            // Execute function
            ReadState readState = new ReadState(serverAddress, resourceManager, FILENAME, false, socket);
            readState.execute();

            // Verify first sent request is a RRRQ
            inOrder.verify(socket).sendMessage(argument.capture(), Mockito.eq( serverAddress));
            Assert.assertEquals("Created Read Request Does Not Match", new String(expectedRRQ.toByteArray()), new String(argument.getValue().toByteArray()));

            // Verify second sent request is an ACK with block #1
            inOrder.verify(socket).sendMessage(argument.capture(), Mockito.eq(connectionManagerSocketAddress));
            Assert.assertEquals("Expected ACK Message Does Not Match", new String(expectedAck.toByteArray()), new String(argument.getValue().toByteArray()));

            Mockito.verify(resourceManager).writeBytesToFile(FILENAME,FILE_SHORT.getBytes());

        } catch ( IOException e) {
            Assert.fail(e.getMessage());
        }
	}
}
