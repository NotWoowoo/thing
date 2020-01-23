package interpreter;

public class Run{

	public static void main(String[] args) {
		Environment world = new Environment();
		
		Interpreter program = new Interpreter("scripts/main.ls");
		
		program.executeOn(world);
	}

}
