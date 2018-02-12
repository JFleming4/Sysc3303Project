package states;

public class ExitState extends State {

	@Override
	public State execute() {
		System.exit(0);
		return null;
	}

}
