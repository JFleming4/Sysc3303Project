package parsing;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CommandTest {
	
	protected List<String> tokens;
	protected Command cmd;
	
	public CommandTest(String[] tokens) {
		this.cmd = Parser.parse(tokens);
		this.tokens = Arrays.asList(tokens);
	}

	@Test
	public void testIsValid() {
		assertTrue(cmd.isValid());
	}
	
	 @Parameters
	 public static Collection<Object[]> data() {
		 Object[][] tokensArray = new Object[][] {
			 { new String[] { "read", "file.txt", "localhost:3000" } },
			 { new String[] { "write", "file.txt", "localhost:3000" } },
			 { new String[] {"help" } },
			 { new String[] { "exit" } }
		 };
		 
		 
		 return Arrays.asList(tokensArray);
	 }
}
