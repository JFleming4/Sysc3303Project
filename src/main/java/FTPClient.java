import java.util.Scanner;

import parsing.Command;
import parsing.Parser;

public class FTPClient {
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		Command command = null;
		String input;
		
		for (;;) {
			System.out.print(">> ");
			input = sc.nextLine();
			command = Parser.parse(input.split(" "));
			command.execute();
		}
	}

}
