package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.AbstractSyntaxTrees.BaseType;
import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassType;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.FieldDeclList;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.MethodDeclList;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.ParameterDeclList;
import miniJava.AbstractSyntaxTrees.StatementList;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.ContextualAnalyzer.Identification;
import miniJava.ContextualAnalyzer.TypeChecking;
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
	private static AST ast;
	private static Identification idcheck;
	private static TypeChecking typecheck;

	static boolean compileProgram(String sourceName) {
		SourceFile source = new SourceFile(sourceName);
		scanner = new Scanner(source);
		errorReporter = new ErrorReporter();
		parser = new Parser(scanner, errorReporter);
		ast = parser.parse();
		idcheck = new Identification(ast, errorReporter);
		// StdEnvGenerator.genEnv(idcheck.idt);
		////////////////////////
		ParameterDecl n = new ParameterDecl(new BaseType(TypeKind.INT, null), new Identifier(new Token(1, "n", new SourcePosition())), null);
		ParameterDeclList printParams = new ParameterDeclList();
		printParams.add(n);
		MethodDecl println = new MethodDecl(
				new FieldDecl(false, false, new BaseType(TypeKind.VOID, null), new Identifier(new Token(1, "println", new SourcePosition())), null), printParams,
				new StatementList(), null);
		MethodDeclList printstreamMethods = new MethodDeclList();
		printstreamMethods.add(println);
		ClassDecl printstream = new ClassDecl(new Identifier(new Token(1, "_PrintStream", new SourcePosition())), new FieldDeclList(), printstreamMethods, null);
		// System
		FieldDecl out = new FieldDecl(false, true,
				new ClassType(new Identifier(new Token(1, "_PrintStream", new SourcePosition())), null), new Identifier(new Token(1, "out", new SourcePosition())), null);
		FieldDeclList sysFields = new FieldDeclList();
		sysFields.add(out);
		ClassDecl sys = new ClassDecl(new Identifier(new Token(1, "System", new SourcePosition())), sysFields, new MethodDeclList(), null);
		ClassDecl str = new ClassDecl(new Identifier(new Token(1, "String", new SourcePosition())), new FieldDeclList(), new MethodDeclList(), null);
		/*System.out.println(printstream.id.spelling);
		System.out.println(sys.id.spelling);
		System.out.println(str.id.spelling);*/
		idcheck.idt.enterDecl(printstream);
		idcheck.idt.enterDecl(sys);
		idcheck.idt.enterDecl(str);
		//////////////////////////
		ast = idcheck.check();
		typecheck = new TypeChecking(ast, errorReporter, idcheck.userDefinedString);
		if (!errorReporter.hasErrors())
			typecheck.check();
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

		// AST ast = parser.parse();
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
