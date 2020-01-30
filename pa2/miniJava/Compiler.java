package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.SyntacticAnalyzer.*;
/*import miniJava.AbstractSyntaxTrees.*;

public class Compiler {
	
	public static void main(String[] args) {
		//InputStream inputStream = null;
		String sourceName = null;
		try {
			//inputStream = new FileInputStream(args[0]);
			sourceName = args[0];
		}
		catch (FileNotFoundException e) {
			System.out.println("Input file "+args[0]+" not found");
			System.exit(3);
		}
		
		ErrorReporter errorReporter = new ErrorReporter();
		//Scanner scanner = new Scanner(inputStream); //inputStream, errorReporter
		SourceFile source = new SourceFile(sourceName);
		Scanner scanner = new Scanner(source);
		Parser parser = new Parser(scanner, errorReporter);
		
		System.out.println("Syntactic analysis ... ");
		AST ast = parser.parse();
		System.out.print("Syntactic analysis complete: ");
		if (errorReporter.hasErrors()) {
			System.out.println("Invalid miniJava program");
			System.exit(4);
		} else {
			System.out.println("Valid miniJava program");
			new ASTDisplay().showTree(ast);
			System.exit(0);
		}
	}
}*/

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
			System.exit(3);
		}

		String sourceName = args[0];

		compiledOK = compileProgram(sourceName);

		AST ast = parser.parse();
		if (compiledOK) {
			System.out.println("Compilation was successful.");
			new ASTDisplay().showTree(ast);
			System.exit(0);
		} else {
			System.out.println("Compilation was unsuccessful.");
			System.exit(4);
		}
	}
}
