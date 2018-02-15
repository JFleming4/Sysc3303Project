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

public class WriteStateTest {

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
	public void shortWriteRequestSuccess() {
	    try {

	        // Expected WRQ Ack block num
            int mockAckBlockNum = 0;

            // Mocked and expected Messages to be passed in 1 block read file transfer
            RequestMessage expectedWRQ = new RequestMessage(MessageType.WRQ,FILENAME);
            AckMessage mockedAckRequest = new AckMessage(mockAckBlockNum);
            DatagramPacket mockedAckRequestPacket = new DatagramPacket(mockedAckRequest.toByteArray(), mockedAckRequest.toByteArray().length, connectionManagerSocketAddress);
            DataMessage expectedData = new DataMessage(mockAckBlockNum+1,FILE_SHORT.getBytes());
            AckMessage mockedAckData = new AckMessage(mockAckBlockNum+1);
            DatagramPacket mockedAckDataPacket = new DatagramPacket(mockedAckData.toByteArray(), mockedAckData.toByteArray().length, connectionManagerSocketAddress);

            // Mock mechanism to capture arguments in order
            InOrder inOrder = Mockito.inOrder(socket);
            ArgumentCaptor <Message> argument = ArgumentCaptor.forClass(Message.class);

            // Return the mock Ack Packet to WRQ on first call, return mock Ack to Data Message on second call
            Mockito.when(socket.receivePacket())
                .thenReturn(mockedAckRequestPacket)
                .thenReturn(mockedAckDataPacket);
            // Return proper
            Mockito.when(resourceManager.fileExists(FILENAME)).thenReturn(true);
            Mockito.when(resourceManager.readFileToBytes(FILENAME)).thenReturn(FILE_SHORT.getBytes());


            // Execute function
            WriteState writeState = new WriteState(serverAddress, resourceManager, FILENAME, false, socket);
            writeState.execute();

            // Verify first sent request is a RRQ
            inOrder.verify(socket).sendMessage(argument.capture(), Mockito.eq(serverAddress));
            Assert.assertEquals("Created Write Request Does Not Match", new String(expectedWRQ.toByteArray()), new String(argument.getValue().toByteArray()));

            // Verify second sent request is an ACK with block #1
            inOrder.verify(socket).sendMessage(argument.capture(), Mockito.eq(connectionManagerSocketAddress));
            Assert.assertEquals("Expected DATA Message Does Not Match", new String(expectedData.toByteArray()), new String(argument.getValue().toByteArray()));

            Mockito.verify(socket, Mockito.times(2)).receivePacket();
            Mockito.verify(resourceManager).readFileToBytes((FILENAME));

        } catch ( IOException e) {
            Assert.fail(e.getMessage());
        }
	}
}
