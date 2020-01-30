package miniJava.SyntacticAnalyzer;

import java.util.Arrays;
import java.util.List;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class Parser { // creates tree out of tokens
	private Scanner lexicalAnalyzer;
	private ErrorReporter errorReporter;
	private Token currentToken;
	private SourcePosition previousTokenPosition;
	private boolean isVoid = false;

	public Parser(Scanner lexer, ErrorReporter reporter) {
		lexicalAnalyzer = lexer;
		errorReporter = reporter;
		previousTokenPosition = new SourcePosition();
	}

	// accept checks whether the current token matches tokenExpected.
	// If so, fetches the next token.
	// If not, reports a syntactic error.

	void accept(int tokenExpected) throws SyntaxError {
		if (currentToken.kind == tokenExpected) {
			previousTokenPosition = currentToken.position;
			currentToken = lexicalAnalyzer.scan();
		} else {
			syntacticError("\"%\" expected here", Token.spell(tokenExpected));
		}
	}

	void acceptIt() {
		previousTokenPosition = currentToken.position;
		currentToken = lexicalAnalyzer.scan();
	}

	// start records the position of the start of a phrase.
	// This is defined to be the position of the first
	// character of the first token of the phrase.

	void start(SourcePosition position) {
		position.start = currentToken.position.start;
	}

	// finish records the position of the end of a phrase.
	// This is defined to be the position of the last
	// character of the last token of the phrase.

	void finish(SourcePosition position) {
		position.finish = previousTokenPosition.finish;
	}

	void syntacticError(String messageTemplate, String tokenQuoted) throws SyntaxError {
		SourcePosition pos = currentToken.position;
		errorReporter.reportError(messageTemplate, tokenQuoted, pos);
		throw (new SyntaxError());
	}

	///////////////////////////////////////////////////////////////////////////

	public AST parse() {
		currentToken = lexicalAnalyzer.scan();
		Package pack = null; 
		try {
			pack = new Package(parseProgram(), currentToken.position);
		} catch (SyntaxError s) {
			System.out.println("The syntax error has been caught...");
		}
		return pack;
	}

	private ClassDeclList parseProgram() throws SyntaxError {
		ClassDeclList cdl = null;
		ClassDecl cd = null;
		while (currentToken.kind == Token.CLASS) {
			cd = parseClassDeclaration();
			cdl.add(cd);
		}
		accept(Token.EOT);
		return cdl;
	}

	private ClassDecl parseClassDeclaration() throws SyntaxError {
		ClassDecl cd = null;
		FieldDeclList fdl = null;
		//MemberDecl mbd = null;
		MethodDeclList mdl = null;
		FieldDecl fd = null;
		MethodDecl md = null;
		Identifier classId = null;
		Identifier declId = null;
		ParameterDeclList pdl = null;
		StatementList sl = null;
		Statement s = null;
		Expression e = null;
		
		accept(Token.CLASS);
		classId = parseIdentifier();
		accept(Token.LCURLY);

		while (isDeclarator(currentToken.kind)) {
			fd = parseDeclarators();
			declId = parseIdentifier();
			fd.name = declId.spelling;
			
			if (isVoid && currentToken.kind == Token.SEMICOLON) {
				syntacticError("\";\" or \"(\" expected here, instead of " + "\"%\"", currentToken.spelling);
				break;
			}

			switch (currentToken.kind) {
			case Token.SEMICOLON:
				fdl.add(fd); //add fd because ';'
				acceptIt();
				break;

			case Token.LPAREN:
				acceptIt();

				if (isTypeOrParameterList(currentToken.kind))
					pdl = parseParameterList();
				accept(Token.RPAREN);
				accept(Token.LCURLY);

				while (isStatement(currentToken.kind)) {
					s = parseStatement();
					sl.add(s);
				}
					

				if (currentToken.kind == Token.RETURN) {
					acceptIt();
					e = parseExpression();
					s = new ReturnStmt(e, e.posn);
					sl.add(s);
					accept(Token.SEMICOLON);
				}
				
				md = new MethodDecl(fd, pdl, sl, fd.posn);
				mdl.add(md);
				accept(Token.RCURLY);
				break;
			default:
				syntacticError("\";\" or \"(\" expected here, instead of " + "\"%\"", currentToken.spelling);
				break;
			}
		}
		accept(Token.RCURLY);
		
		cd = new ClassDecl(classId.spelling, fdl, mdl, classId.posn);
		return cd;
	}

	private FieldDecl parseDeclarators() throws SyntaxError {
		TypeDenoter t = null;
		FieldDecl fd = null;
		boolean isPrivate = false;
		boolean isStatic = false;
		if (currentToken.kind == Token.PUBLIC || currentToken.kind == Token.PRIVATE) {
			if (currentToken.kind == Token.PRIVATE) {
				isPrivate = true;
			}
			acceptIt();
		}
		if (currentToken.kind == Token.STATIC) {
			isStatic = true;
			acceptIt();
		}
		t = parseType();
		fd = new FieldDecl(isPrivate, isStatic, t, null, currentToken.position);
		return fd;
	}

	private TypeDenoter parseType() throws SyntaxError {
		TypeDenoter t = null;
		Identifier id = null;
		
		switch (currentToken.kind) {
		case Token.BOOLEAN:
			t = new BaseType(TypeKind.BOOLEAN, currentToken.position);
			acceptIt();
			break;
		case Token.VOID:
			t = new BaseType(TypeKind.VOID, currentToken.position);
			isVoid = true;
			acceptIt();
			break;

		case Token.IDENTIFIER:
			id = parseIdentifier();
			t = new ClassType(id, id.posn);
			if (currentToken.kind == Token.LBRACKET) {
				t = new ArrayType(new ClassType(id, id.posn), id.posn);
				acceptIt();
				accept(Token.RBRACKET);
			}
			break;

		case Token.INT:
			t = new BaseType(TypeKind.INT, currentToken.position);
			acceptIt();
			if (currentToken.kind == Token.LBRACKET) {
				t = new ArrayType(new BaseType(TypeKind.INT, currentToken.position), t.posn);
				acceptIt();
				accept(Token.RBRACKET);
			}
			break;

		default:
			syntacticError("\"%\" cannot start a type", currentToken.spelling);
			break;
		}
		return t;
	}

	private ParameterDeclList parseParameterList() throws SyntaxError {
		ParameterDeclList pdl = null;
		ParameterDecl pd = null;
		TypeDenoter t = null;
		Identifier id = null;
		if (currentToken.kind == Token.VOID) {
			syntacticError("\"%\" cannot start a type", currentToken.spelling);
		}
		t = parseType();
		id = parseIdentifier();
		pd = new ParameterDecl(t, id.spelling, id.posn);
		pdl.add(pd);

		while (currentToken.kind == Token.COMMA) {
			if (currentToken.kind == Token.VOID) {
				syntacticError("\"%\" cannot start a type", currentToken.spelling);
				break;
			}
			acceptIt();
			t = parseType();
			id = parseIdentifier();
			pd = new ParameterDecl(t, id.spelling, t.posn);
			pdl.add(pd);
		}
		return pdl;
	}

	private ExprList parseArgumentList() throws SyntaxError {
		ExprList el = null;
		Expression e = null;
		e = parseExpression();
		el.add(e);
		
		while (currentToken.kind == Token.COMMA) {
			acceptIt();
			e = parseExpression();
			el.add(e);
		}
		
		return el;
	}

	private Reference parseReference() throws SyntaxError {
		Reference r = null;
		Identifier id = null;
		
		
		if (currentToken.kind == Token.THIS) {
			acceptIt();
			r = new ThisRef(currentToken.position);
		} else if (currentToken.kind == Token.IDENTIFIER) {
			acceptIt();
			r = new IdRef(new Identifier(currentToken), currentToken.position);
		} else {
			syntacticError("\"%\" cannot start a reference", currentToken.spelling);
		}
		
		while (currentToken.kind == Token.DOT) {
			acceptIt();
			id = parseIdentifier();
			r = new QualRef(r, id, r.posn);
		}
		
		return r;
	}

	private Statement parseStatement() throws SyntaxError {
		BlockStmt bs = null;
		Statement s = null;
		Expression e = null;
		TypeDenoter t = null;
		Identifier id = null;
		Reference r = null;
		
		switch (currentToken.kind) {
		case Token.LCURLY: //{ Statement* }
			acceptIt();
			StatementList sl = null;
			while (isStatement(currentToken.kind))
				s = parseStatement();
				sl.add(s);
			accept(Token.RCURLY);
			bs = new BlockStmt(sl, currentToken.position);
			s = bs;
			break;

		case Token.IF: //if ( Expression ) Statement (else Statement)? 
			acceptIt();
			accept(Token.LPAREN);
			e = parseExpression();
			accept(Token.RPAREN);
			Statement s1 = null;
			Statement s2 = null;
			s1 = parseStatement();
			if (currentToken.kind == Token.ELSE) {
				acceptIt();
				s2 = parseStatement();
			}
			s = new IfStmt(e, s1, s2, e.posn);
			break;

		case Token.WHILE: //while ( Expression ) Statement 
			acceptIt();
			accept(Token.LPAREN);
			e = parseExpression();
			accept(Token.RPAREN);
			Statement S1 = null;
			s1 = parseStatement();
			s = new WhileStmt(e, s1, e.posn);
			break;

		case Token.BOOLEAN: //Type id = Expression;
		//case Token.VOID: --> void is not a Type
			t = parseType();
			id = parseIdentifier();
			accept(Token.ASSIGN);
			e = parseExpression();
			accept(Token.SEMICOLON);
			s = new VarDeclStmt(new VarDecl(new BaseType(TypeKind.BOOLEAN, id.posn), id.spelling, id.posn), e, t.posn);
			break;
		case Token.INT:
			t = parseType();
			id = parseIdentifier();
			accept(Token.ASSIGN);
			e = parseExpression();
			accept(Token.SEMICOLON);
			s = new VarDeclStmt(new VarDecl(new BaseType(TypeKind.INT, id.posn), id.spelling, id.posn), e, t.posn);
			break;

		case Token.THIS: // Reference ([Expression])? =
			r = parseReference(); // Expression; | (ArgumentList?);
			Expression e1 = null;
			switch (currentToken.kind) {
			case Token.LBRACKET:
			case Token.ASSIGN:
				if (currentToken.kind == Token.LBRACKET) {
					acceptIt();
					e1 = parseExpression();
					accept(Token.RBRACKET);
				}

				accept(Token.ASSIGN);
				e = parseExpression();
				accept(Token.SEMICOLON);
				s = new AssignStmt(new IxRef(r, e1, r.posn), e, e.posn);
				break;

			case Token.LPAREN:
				acceptIt();
				ExprList el = null;
				if (isArgumentList(currentToken.kind))
					el = parseArgumentList();
				accept(Token.RPAREN);
				accept(Token.SEMICOLON);
				s = new CallStmt(r, el, r.posn);
				break;

			default:
				syntacticError("\"[\", \"=\", or \"(\" expected here, " + "instead of \"%\"", currentToken.spelling);
				break;
			}
			break;

		case Token.IDENTIFIER:
			id = parseIdentifier();
			
			switch (currentToken.kind) {
			case Token.LBRACKET:
				acceptIt();

				switch (currentToken.kind) {
				case Token.RBRACKET: // Reference ::= id [] id = Expression;
					acceptIt();
					Identifier id1 = null;
					id1 = parseIdentifier();
					accept(Token.ASSIGN);
					e = parseExpression();
					accept(Token.SEMICOLON);
					IdRef ir = new IdRef(id, id.posn);
					IxRef ix = new IxRef(ir, e, ir.posn);
					t = new ArrayType(new BaseType(TypeKind.ARRAY, currentToken.position), currentToken.position);
					s = new VarDeclStmt(new VarDecl(t, id1.spelling, t.posn), e, ix.posn);
					break;

				case Token.THIS: // Statement ::= id [Expression] = Expression;
					e = parseExpression();
					accept(Token.RBRACKET);
					accept(Token.ASSIGN);
					e1 = parseExpression();
					accept(Token.SEMICOLON);
					s = new AssignStmt(new IxRef(new ThisRef(currentToken.position), e, currentToken.position), e1, currentToken.position);
					break;
				case Token.IDENTIFIER: // Starters of Expression
					e = parseExpression();
					accept(Token.RBRACKET);
					accept(Token.ASSIGN);
					e1 = parseExpression();
					accept(Token.SEMICOLON);
					s = new AssignStmt(new IxRef(new IdRef(id, id.posn), e, currentToken.position), e1, currentToken.position);
					break;
				case Token.NOT:
				case Token.MINUS:
				case Token.LPAREN:
				case Token.INT:
				case Token.TRUE:
				case Token.FALSE:
				case Token.NEW:
					e = parseExpression();
					accept(Token.RBRACKET);
					accept(Token.ASSIGN);
					e1 = parseExpression();
					accept(Token.SEMICOLON);
					s = new AssignStmt(new IxRef(new IdRef(id, id.posn), e, currentToken.position), e1, currentToken.position);
					break;

				default:
					syntacticError("\"]\" or an expression expected here, " + "instead of \"%\"",
							currentToken.spelling);
					break;
				}
				break;

			case Token.IDENTIFIER: // Statement ::= id id = Expression;
				id = parseIdentifier();
				accept(Token.ASSIGN);
				e = parseExpression();
				accept(Token.SEMICOLON);
				s = new AssignStmt(new IdRef(id, id.posn), e, currentToken.position);
				break;

			case Token.DOT: // Statement ::= id (. id)* ([Expression])? =
				QualRef q = null;
				while (currentToken.kind == Token.DOT) { // Expression; | (Ar?);
					acceptIt();
					id = parseIdentifier();
					q = new QualRef(q, id, q.posn);
					r = q;
				}

				switch (currentToken.kind) {
				case Token.LBRACKET:
				case Token.ASSIGN:
					if (currentToken.kind == Token.LBRACKET) {
						acceptIt();
						parseExpression();
						r = new IxRef(r, e, r.posn);
						accept(Token.RBRACKET);
					}
					accept(Token.ASSIGN);
					e = parseExpression();
					accept(Token.SEMICOLON);
					s = new AssignStmt(r, e, currentToken.position);
					break;

				case Token.LPAREN:
					acceptIt();
					ExprList el = null;
					if (isArgumentList(currentToken.kind))
						el = parseArgumentList();
					accept(Token.RPAREN);
					accept(Token.SEMICOLON);
					s = new CallStmt(r, el, r.posn);
					break;

				default:
					syntacticError("\"[\", \"=\", or \"(\" expected here, " + "instead of \"%\"",
							currentToken.spelling);
					break;
				}
				break;

			case Token.ASSIGN: // Statement ::= id = Expression;
				acceptIt();
				e = parseExpression();
				accept(Token.SEMICOLON);
				s = new AssignStmt(new IdRef(id, id.posn), e, currentToken.position);
				break;

			case Token.LPAREN: // Statement ::= id (ArgumentList?);
				acceptIt();
				ExprList el = null;
				if (isArgumentList(currentToken.kind))
					el = parseArgumentList();
				accept(Token.RPAREN);
				accept(Token.SEMICOLON);
				s = new CallStmt(new IdRef(id, id.posn), el, currentToken.position);
				break;

			default:
				syntacticError("Type or reference expected here, instead " + "of \"%\"", currentToken.spelling);
				break;
			}
			break;

		default:
			syntacticError("\"%\" cannot start a statement", currentToken.spelling);
			break;
		}
		return s;
	}

	private Expression parseExpression() throws SyntaxError {
		Expression e = null;
		Reference r = null;
		
		switch (currentToken.kind) {
		case Token.THIS: // Reference ::= id | this | Reference.id
		case Token.IDENTIFIER:
			parseReference(); //Reference
			if (currentToken.kind == Token.LBRACKET) { //Reference [ Expression ]
				acceptIt();
				parseExpression();
				accept(Token.RBRACKET);
			} else if (currentToken.kind == Token.LPAREN) { //Reference ( ArgumentList? )
				acceptIt();
				if (isArgumentList(currentToken.kind))
					parseArgumentList();
				accept(Token.RPAREN);
			}
			break;

		case Token.NOT: // unop Expression
		case Token.MINUS:
			Token token = currentToken;
			acceptIt();
			Expression e1 = null;
			e1 = parseExpression();
			e = new UnaryExpr(new Operator(token), e1, previousTokenPosition);
			break;

		case Token.LPAREN: //(Expression)
			acceptIt();
			Expression esomething = null;
			e1 = parseExpression();
			//e = new CallExpr(new );
			accept(Token.RPAREN);
			break;

		case Token.INTLITERAL: //num | true | false
		case Token.TRUE:
		case Token.FALSE:
			e = new LiteralExpr(new IntLiteral(currentToken), currentToken.position);
			acceptIt();
			break;

		case Token.NEW: //new ( id () | int [ Expression ] | id [ Expression ] )
			acceptIt();
			if (currentToken.kind == Token.INT) {
				acceptIt();
				accept(Token.LBRACKET);
				parseExpression();
				accept(Token.RBRACKET);
			} else if (currentToken.kind == Token.IDENTIFIER) {
				acceptIt();
				if (currentToken.kind == Token.LBRACKET) {
					acceptIt();
					parseExpression();
					accept(Token.RBRACKET);
				} else if (currentToken.kind == Token.LPAREN) {
					acceptIt();
					accept(Token.RPAREN);
				}
			}
			break;

		default:
			syntacticError("\"%\" cannot start an expression", currentToken.spelling);
			break;
		}

		while (isBinop(currentToken.kind)) {
			acceptIt();
			parseExpression();
		}
		return e;
	}

	private Identifier parseIdentifier() throws SyntaxError {
		if (currentToken.kind == Token.IDENTIFIER) {
			previousTokenPosition = currentToken.position;
			currentToken = lexicalAnalyzer.scan();
			return new Identifier(currentToken);
		} else {
			syntacticError("\"%\" expected here, instead of \"" + currentToken.spelling + "\"",
					Token.spell(Token.IDENTIFIER));
			return null;
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////

	private boolean isDeclarator(int kind) {
		List<Integer> tokens = Arrays.asList(Token.PUBLIC, Token.PRIVATE, Token.STATIC);
		return tokens.contains(kind) || isTypeOrParameterList(kind);
	}

	private boolean isTypeOrParameterList(int kind) {
		List<Integer> tokens = Arrays.asList(Token.INT, Token.BOOLEAN, Token.IDENTIFIER, Token.VOID);
		return tokens.contains(kind);
	}

	private boolean isArgumentList(int kind) {
		List<Integer> tokens = Arrays.asList(Token.LPAREN, Token.INTLITERAL, Token.TRUE, Token.FALSE, Token.NEW);
		return isReference(kind) || isUnop(kind) // unop
				|| tokens.contains(kind);
	}

	private boolean isReference(int kind) {
		List<Integer> tokens = Arrays.asList(Token.IDENTIFIER, Token.THIS, Token.DOT);
		return tokens.contains(kind);
	}

	private boolean isStatement(int kind) {
		List<Integer> tokens = Arrays.asList(Token.LCURLY, Token.IF, Token.WHILE, Token.THIS);
		return isTypeOrParameterList(kind) || tokens.contains(kind);
	}

	private boolean isUnop(int kind) {
		List<Integer> tokens = Arrays.asList(Token.NOT, Token.MINUS);
		return tokens.contains(kind);
	}

	private boolean isBinop(int kind) {
		List<Integer> tokens = Arrays.asList(Token.GREATER, Token.LESS, Token.EQUAL, Token.GEQUAL, Token.LEQUAL,
				Token.NOTEQUAL, Token.AND, Token.OR, Token.PLUS, Token.MINUS, Token.TIMES, Token.DIV);
		return tokens.contains(kind);
	}
}
