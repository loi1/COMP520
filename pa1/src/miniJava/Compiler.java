package miniJava;

import miniJava.SyntacticAnalyzer.*;

public class Compiler {

	private static Scanner scanner;
	private static Parser parser;
	private static ErrorReporter errorReporter;

	static boolean compileProgram(String sourceName) {
		SourceFile source = new SourceFile(sourceName);
		scanner = new Scanner(source);
		errorReporter = new ErrorReporter();
		parser = new Parser(scanner, errorReporter);
		parser.parse();
		return errorReporter.numErrors == 0;
	}

	public static void main(String[] args) {
		boolean compiledOK;
		if (args.length != 1) {
			System.out.println("Usage: tc filename");
			System.exit(1);
		}

		String sourceName = args[0];

		compiledOK = compileProgram(sourceName);

		if (compiledOK) {
			System.out.println("Compilation was successful.");
			System.exit(0);
		} else {
			System.out.println("Compilation was unsuccessful.");
			System.exit(4);
		}
	}
}
