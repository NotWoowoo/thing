package interpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;


public class Interpreter {
	
	private boolean runnable = false;
	private Environment environment;
	
	private String[] statements; // { "make(1, 2, 3)", "say("hi")" }
	private String[] funcs = {}; // { "make", "say" }
	private Object[][] args = {}; // { {1, 2, 3}, {"hi"} }
	//TODO vars, preprocess, math
	
	private static class Variable{
		public String name;
		public Variable(String name) {this.name = name;}
	}
	
	private Object getVarFromName(String varName) {
		if(variables.containsKey(varName)) {
			return variables.get(varName);
		}else {
			System.err.println("[ERROR] Unknown variable: " + varName);
			return "nil";
		}
	}
	
	private HashMap<String, Object> variables = new HashMap<String, Object>();
	
	@FunctionalInterface
	public interface Function{
		public void call(Object[] args);
	}
	
	private static HashMap<String, Function> stdLibFuncs = new HashMap<String, Function>();
	
	public Interpreter(String filepath){
		try {
			//Read every command into statements//
			File file = new File(filepath);
			BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			
			String st = "";
			String cs = "";
			while((st = r.readLine()) != null) cs += st + "\n";
			statements = cs.split(";");
			
			r.close();
			
			//Parse statements into tokens//
			for(String statement : statements){
				if(statement.length() > 0){ // only do something if statement contains text
					//handle comments
					if(statement.contains("//")) {
						statement = removeCommentFrom(statement);
					}
					
					//System.out.print("|"+statement+"|");
					
					//handle functions
					if(statement.contains("(")) {
						String funcName = statement.substring(firstLetterIndex(statement), statement.indexOf("(")); //isolate function name
						//append function name to funcs
						funcs = Arrays.copyOf(funcs, funcs.length + 1); //extend funcs
						funcs[funcs.length-1] = funcName; //set last index to func name
						
						//append arguments to args at [func index][argument index]
						args = Arrays.copyOf(args, funcs.length); //extend args
						args[args.length-1] = parseArgs(statement); //set args[func index] to arguments via parseArgs
					}
					
					//handle variable assignments
					if(statement.contains("=")) {
						statement = statement.replace(" ", ""); //remove spaces
						String varName = statement.substring(firstLetterIndex(statement), statement.indexOf("=")); //get var name
						String varValue = statement.substring(statement.indexOf("=")+1, statement.length()); //get value
						
						if(varValue.contains("\"")) {
							varValue = isolateStringExpression(varValue);
						}else if(!isExpressionNumber(varValue)) {
							varValue = (String)variables.get(varValue);
						}
						
						variables.put(varName, varValue); //set var in memory
					}
					
				}
			}
			
			initStdLib(); //impliment functions
			runnable = true;
		} catch (FileNotFoundException e) { e.printStackTrace(); } catch (IOException e) {e.printStackTrace();}
	}
	
	private static boolean isExpressionNumber(String expression) {
		boolean isNum = false; //initialize argIsVar
		//test numbers 0...9
		for(int num = 0; num <= 9; ++num) {
			isNum = isNum || expression.contains(String.valueOf(num));
		}
		
		return isNum;
	}
	
	private static String isolateStringExpression(String st) {
		st = st.substring(st.indexOf("\"")+1);
		st = st.substring(0, st.indexOf("\""));
		return st;
	}
	
	private static int firstLetterIndex(String s) {
		for(int i = 0; i < s.length(); ++i) {
			char c = s.charAt(i);
			if(!Character.isWhitespace(c)) return i;
		}
		return -1;
	}
	
	private static String removeCommentFrom(String s) {
		while(s.contains("//")) {
			s = s.substring(firstLetterIndex(s));
			s = s.replace(s.substring(s.indexOf("//"),s.indexOf("\n")), "");
		}
		return s;
	}
	
	private static Object[] parseArgs(String statement){
		//isolate arguments from raw code
		statement = statement.substring(statement.indexOf("(")+1,statement.indexOf(")"));
		String[] tempStatements = statement.split(",");
		//set up output array - size is number of args
		Object[] output = new Object[tempStatements.length];
		
		//loop thru args
		for(int i = 0; i < tempStatements.length; ++i){
			String tempStatement = tempStatements[i];
			
			if(tempStatement.contains("\"")){
				output[i] = isolateStringExpression(tempStatement); //handdle string args if quotes - iscolate text
			}else{
				tempStatement = tempStatement.replace(" ", ""); //remove spaces
				
				if(!isExpressionNumber(tempStatement)) {
					output[i] = new Variable(tempStatement); //arg is variable if not a number
				}else {
					output[i] = tempStatement; //otherwise a number
				}
				
			}
		}
		
		return output;
	}
	
	public void executeOn(Environment e){
		environment = e; //set environment
		if(runnable){
			interpret();
		}else{
			System.err.println("Program can't be executed");
		}
	}
	
	private void interpret(){
		//for every function tokenized
		for(int i = 0; i < funcs.length; ++i) {
			String func = funcs[i]; //function name
			Object[] funcArgs = args[i]; //arguments
			if(stdLibFuncs.containsKey(func)) { //if library contains func name
				stdLibFuncs.get(func).call(funcArgs); //call that function
			}else {
				System.err.println("[ERROR] Unknown function: " + func);
			}
		}
	}
	
	//-----methods for casting arguments (or variables) to some datatype-----//
		
		//value of variable as obj (not intended for use)
		private Object argval(Object o) {
			if(o instanceof Variable) {
				String varName = ((Variable)o).name;
				return getVarFromName(varName);
			} else {
				return o;
			}
		}
	
		//string
		private String argstr(Object o) {
			return (String)argval(o);
		}
		
		//int
		private int argint(Object o) {
			return Integer.parseInt((String) argval(o));
		}
		
		//double
		private double argdouble(Object o) {
			return Double.parseDouble((String) argval(o));
		}
		
	///////////////////////////////////////////////////////////
		
	private static void implimentFunction(String name, Function f) {
		stdLibFuncs.put(name, f); //add function to library
	}
	
	//impliments standard library functions
	private void initStdLib() {
		implimentFunction("window", args -> {
			environment.createWindow(argint(args[0]), argint(args[1]), argstr(args[2]));
		});
		
		implimentFunction("circle", args -> {
			environment.createCircle(argdouble(args[0]), argdouble(args[1]), argdouble(args[2]));
		});
		
		implimentFunction("print", args -> {
			System.out.println(argstr(args[0]));
		});
		
		implimentFunction("printErr", args -> {
			System.err.println(argstr(args[0]));
		});
	}
}
