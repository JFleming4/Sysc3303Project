package parsing;

import static org.junit.Assert.*;
import static resources.Configuration.GLOBAL_CONFIG;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SocketCommandTest extends CommandTest {
	
	private static final String FILENAME = "file.txt";
	private static final String SERVER_ADDRESS = "localhost";
	
	private SocketCommand socketCmd;
	
	public SocketCommandTest(String[] tokens) throws IOException {
		super(tokens);
		this.socketCmd = (SocketCommand) cmd;
	}
	
	@Test
	public void testserverAddress() {
		try {
			if (tokens.contains("-t") || tokens.contains("--test"))
				assertEquals(new InetSocketAddress(SERVER_ADDRESS, GLOBAL_CONFIG.SIMULATOR_PORT), socketCmd.getServerAddress());
			else
				assertEquals(new InetSocketAddress(SERVER_ADDRESS, GLOBAL_CONFIG.SERVER_PORT), socketCmd.getServerAddress());
        } catch (UnknownHostException e) {
            fail(e.getMessage());
        }
	}
	
	@Test
	public void testFilename() {
		assertEquals(FILENAME, socketCmd.getFilename());	
	}
	
	
	@Test
	public void testIsVerbose() { 
		if (tokens.contains("-v") || tokens.contains("--verbose"))
			assertTrue(socketCmd.isVerbose());
		else
			assertFalse(socketCmd.isVerbose());
	}
	
	@Test
	public void testIsTest() { 
		if (tokens.contains("-t") || tokens.contains("--test"))
			assertTrue(socketCmd.isTest());
		else
			assertFalse(socketCmd.isTest());
	}
	 @Parameters
	 public static Collection<Object[]> data() {
		 Object[][] tokensArray = new Object[][] {
			 { new String[] { "read", FILENAME, SERVER_ADDRESS, "-v", "-t" } },
			 { new String[] { "read", FILENAME, SERVER_ADDRESS } },
			 { new String[] { "write", FILENAME, SERVER_ADDRESS, "--verbose", "--test" } },
			 { new String[] { "write", FILENAME, SERVER_ADDRESS } }
		 };
		 
		 
		 return Arrays.asList(tokensArray);
	 }
}
