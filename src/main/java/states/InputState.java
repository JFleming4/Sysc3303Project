package states;

import java.util.Scanner;

import parsing.Command;
import parsing.Parser;

public class InputState extends State {
	@Override
	public State execute() {
		System.out.println("Type 'help' for a list of commands");
		
		Scanner sc = new Scanner(System.in);
		String input = sc.nextLine();
		Command command = Parser.parse(input.split(" "));
		
		return command.execute();
	}
}
