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
	private SourcePosition poslit;
	//private boolean isVoid = false;

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
			pack = new Package(parseProgram(), poslit);
		} catch (SyntaxError s) {
			System.out.println("The syntax error has been caught in parse...");
		}
		return pack;
	}

	private ClassDeclList parseProgram() throws SyntaxError {
		ClassDeclList cdl = new ClassDeclList();
		ClassDecl cd = null;
		poslit = new SourcePosition();
		start(poslit);
		
		while (currentToken.kind == Token.CLASS) {
			cd = parseClassDeclaration();
			cdl.add(cd); 
		}
		//System.out.println("cdl size: "+ cdl.size());
		if (cdl.size() == 0) {
			System.out.println("no cd's found");
		}
		
		accept(Token.EOT);
		finish(poslit);
		return cdl;
	}

	private ClassDecl parseClassDeclaration() throws SyntaxError {
		FieldDeclList fdl = new FieldDeclList();
		MethodDeclList mdl = new MethodDeclList();
		
		SourcePosition classPos = new SourcePosition();
		start(classPos);

		accept(Token.CLASS);
		Identifier classId = parseIdentifier();
		accept(Token.LCURLY);

		while (isDeclarator(currentToken.kind)) {
			ParameterDeclList pdl = new ParameterDeclList();
			StatementList sl = new StatementList();
			
			MemberDecl md = parseDeclarators();
			Identifier declId = parseIdentifier();
			
			FieldDecl fd = new FieldDecl(md.isPrivate, md.isStatic, md.type, md.id, md.posn); //md.name instead of declId
			fd.id = declId;
			
			//SourcePosition declPos = new SourcePosition();
			//start(declPos);

			if (md.type.typeKind == TypeKind.VOID && currentToken.kind == Token.SEMICOLON) {
				//isVoid = false;
				syntacticError("\";\" or \"(\" expected here, instead of " + "\"%\"", currentToken.spelling);
				break;
			}

			switch (currentToken.kind) {
			case Token.SEMICOLON:
				fdl.add(fd); // add fd because ';'
				acceptIt();
				break;

			case Token.LPAREN:
				acceptIt();

				if (isTypeOrParameterList(currentToken.kind))
					pdl = parseParameterList();
				accept(Token.RPAREN);
				accept(Token.LCURLY);

				while (isStatement(currentToken.kind)) {
					Statement s = parseStatement();
					sl.add(s);
				}
				//System.out.println("stmtlist size: "+sl.size());

				if (currentToken.kind == Token.RETURN) {
					acceptIt();
					Expression e = parseExpression();
					Statement s = new ReturnStmt(e, e.posn);
					sl.add(s);
					accept(Token.SEMICOLON);
				}

				MethodDecl mdd = new MethodDecl(md, pdl, sl, fd.posn);
				mdd.id = declId;
				mdl.add(mdd);
				accept(Token.RCURLY);
				break;
			default:
				syntacticError("\";\" or \"(\" expected here, instead of " + "\"%\"", currentToken.spelling);
				break;
			}
		}
		accept(Token.RCURLY);
		finish(classPos);

		ClassDecl cd = new ClassDecl(classId, fdl, mdl, classPos);
		return cd;
	}

	private MemberDecl parseDeclarators() throws SyntaxError {
		boolean isPrivate = false;
		boolean isStatic = false;
		SourcePosition declPos = new SourcePosition();
		start(declPos);
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
		TypeDenoter t = parseType();
		finish(declPos);
		FieldDecl fd = new FieldDecl(isPrivate, isStatic, t, null, declPos);
		return fd;
	}

	private TypeDenoter parseType() throws SyntaxError {
		TypeDenoter t = null;
		SourcePosition pos = new SourcePosition();
    	start(pos);

		switch (currentToken.kind) {
		case Token.BOOLEAN:
			acceptIt();
			finish(pos);
			t = new BaseType(TypeKind.BOOLEAN, pos);
			break;
		case Token.VOID:
			//isVoid = true;
			acceptIt();
			finish(pos);
			t = new BaseType(TypeKind.VOID, pos);
			break;

		case Token.IDENTIFIER:
			Identifier id = parseIdentifier();
			t = new ClassType(id, currentToken.position);
			if (currentToken.kind == Token.LBRACKET) {
				acceptIt();
				accept(Token.RBRACKET);
				finish(pos);
				t = new ArrayType(t, pos);
			}
			break;

		case Token.INT:
			t = new BaseType(TypeKind.INT, currentToken.position);
			acceptIt();
			if (currentToken.kind == Token.LBRACKET) {
				acceptIt();
				accept(Token.RBRACKET);
				finish(pos);
				t = new ArrayType(t, pos); 
			}
			break;

		default:
			syntacticError("\"%\" cannot start a type", currentToken.spelling);
			break;
		}

		if (t == null) {
			System.out.println("parseType wrong");
			syntacticError("\"%\" cannot start a type", currentToken.spelling);
		}
		return t;
	}

	private ParameterDeclList parseParameterList() throws SyntaxError {
		SourcePosition paraPos = new SourcePosition();
    	start(paraPos);
		if (currentToken.kind == Token.VOID) {
			syntacticError("\"%\" cannot start a type", currentToken.spelling);
		}
		ParameterDeclList pdl = new ParameterDeclList();
		TypeDenoter t = parseType();
		Identifier id = parseIdentifier();
		finish(paraPos);
		ParameterDecl pd = new ParameterDecl(t, id, paraPos);
		pdl.add(pd);

		while (currentToken.kind == Token.COMMA) {
			if (currentToken.kind == Token.VOID) {
				syntacticError("\"%\" cannot start a type", currentToken.spelling);
				break;
			}
			acceptIt();
			start(paraPos);
			t = parseType();
			id = parseIdentifier();
			finish(paraPos);
			pd = new ParameterDecl(t, id, paraPos);
			pdl.add(pd);
		}
		if (pdl.size() == 0) {
			System.out.println("ParameterDeclList wrong");
			syntacticError("\"%\" cannot start a ParameterList", currentToken.spelling);
		}
		return pdl;
	}

	private ExprList parseArgumentList() throws SyntaxError {
		ExprList el = new ExprList();
		Expression e = parseExpression();
		el.add(e);

		while (currentToken.kind == Token.COMMA) {
			acceptIt();
			e = parseExpression();
			el.add(e);
		}
		if (el.size() == 0) {
			System.out.println("parseArgList wrong");
			syntacticError("\"%\" cannot start an ArgList", currentToken.spelling);
		}

		return el;
	}

	private Reference parseReference() throws SyntaxError {
		SourcePosition pos = new SourcePosition();
    	start(pos);
    	
		Reference r = null;

		if (currentToken.kind == Token.THIS) {
			finish(pos);
			r = new ThisRef(pos);
			acceptIt();
		} else if (currentToken.kind == Token.IDENTIFIER) {
			finish(pos);
			r = new IdRef(new Identifier(currentToken), pos);
			acceptIt();
		} else {
			syntacticError("\"%\" cannot start a reference", currentToken.spelling);
		}

		while (currentToken.kind == Token.DOT) {
			acceptIt();
			finish(pos);
			Identifier id = parseIdentifier();
			r = new QualRef(r, id, pos);
		}

		if (r == null) {
			System.out.println("parseReference wrong");
			syntacticError("\"%\" cannot start a reference", currentToken.spelling);
		}

		return r;
	}

	private Statement parseStatement() throws SyntaxError {
		SourcePosition pos = new SourcePosition();
    	start(pos);
    	
		Statement s = null;
		Expression e = null;

		TypeDenoter t;
		Identifier id;
		Reference r = null;

		switch (currentToken.kind) {
		case Token.LCURLY: // { Statement* }
			acceptIt();
			StatementList sl = new StatementList();
			while (isStatement(currentToken.kind)) {
				Statement stmt = parseStatement();
				sl.add(stmt);
			}
			accept(Token.RCURLY);
			finish(pos);
			BlockStmt bs = new BlockStmt(sl, pos);
			s = bs;
			break;

		case Token.BOOLEAN: // Type id = Expression;
			// case Token.VOID: --> void is not a Type
			t = parseType();
			id = parseIdentifier();
			accept(Token.ASSIGN);
			e = parseExpression();
			accept(Token.SEMICOLON);
			finish(pos);
			s = new VarDeclStmt(new VarDecl(new BaseType(TypeKind.BOOLEAN, t.posn), id,
					id.posn), e, pos);
			break;
			
		case Token.INT:
			t = parseType();
			if (t.typeKind == TypeKind.ARRAY) {
				id = parseIdentifier();
				//System.out.println(id.spelling);
				accept(Token.ASSIGN);
				e = parseExpression();
				accept(Token.SEMICOLON);
				finish(pos);
				//IdRef ir = new IdRef(id, currentToken.position);
				t = new ArrayType(new BaseType(TypeKind.INT, t.posn), pos);
				s = new VarDeclStmt(new VarDecl(t, id, id.posn), e, pos);
						break; 
			} else {
			id = parseIdentifier();
			//System.out.println(id.spelling);
			accept(Token.ASSIGN);
			e = parseExpression();
			accept(Token.SEMICOLON);
			finish(pos);
			s = new VarDeclStmt(
					new VarDecl(new BaseType(TypeKind.INT, t.posn), id, id.posn),
					e, pos);
			break;
			}

		case Token.IF: // if ( Expression ) Statement (else Statement)?
			acceptIt();
			accept(Token.LPAREN);
			e = parseExpression();
			accept(Token.RPAREN);
			Statement s1 = parseStatement();
			if (currentToken.kind == Token.ELSE) {
				acceptIt();
				Statement s2 = parseStatement();
				finish(pos);
				s = new IfStmt(e, s1, s2, pos);
				break;
			}
			finish(pos);
			s = new IfStmt(e, s1, pos);
			break;

		case Token.WHILE: // while ( Expression ) Statement
			acceptIt();
			accept(Token.LPAREN);
			e = parseExpression();
			accept(Token.RPAREN);
			Statement stmt = parseStatement();
			finish(pos);
			s = new WhileStmt(e, stmt, pos);
			break;

		case Token.RETURN: // return Expression?;
			acceptIt();
			if (currentToken.kind != Token.SEMICOLON) {
				e = parseExpression();
			} else
				e = null;
			accept(Token.SEMICOLON);
			finish(pos);
			s = new ReturnStmt(e, pos);
			break;

		case Token.THIS: // Reference ([Expression])? = Expression; | (ArgumentList?);
			SourcePosition thisPos = currentToken.position;
			r = parseReference();
			switch (currentToken.kind) {
			case Token.LBRACKET: // (Ix?)Reference ([ Expression ])? = Expression;
			case Token.ASSIGN:
				Expression e1 = null;
				if (currentToken.kind == Token.LBRACKET) {
					acceptIt();
					e1 = parseExpression();
					accept(Token.RBRACKET);
					accept(Token.ASSIGN);
					e = parseExpression();
					accept(Token.SEMICOLON);
					finish(pos);
					s = new AssignStmt(new IxRef(r, e1, thisPos), e, pos);
					break;
				} else {
					accept(Token.ASSIGN);
					e = parseExpression();
					accept(Token.SEMICOLON);
					finish(pos);
					s = new AssignStmt(r, e, pos);
					break;
				}

			case Token.LPAREN: // Reference ( ArgumentList? );
				acceptIt();
				ExprList el = new ExprList();
				if (isArgumentList(currentToken.kind))
					el = parseArgumentList();
				accept(Token.RPAREN);
				accept(Token.SEMICOLON);
				finish(pos);
				s = new CallStmt(r, el, pos);
				break;

			default:
				syntacticError("\"[\", \"=\", or \"(\" expected here, " + "instead of \"%\"", currentToken.spelling);
				break;
			}
			break;

		case Token.IDENTIFIER:
			id = parseIdentifier();
			SourcePosition idPos = new SourcePosition();
			switch (currentToken.kind) {
			case Token.LBRACKET: // (Ix?)Reference ([ Expression ])? = Expression;
				acceptIt();
				switch (currentToken.kind) {
				case Token.INTLITERAL: // id[int] = expr
					e = parseExpression();
					accept(Token.RBRACKET);
					SourcePosition refPos = new SourcePosition();
                    refPos.start = pos.start;
                    finish(refPos);
					accept(Token.ASSIGN);
					Expression eint = parseExpression();
					accept(Token.SEMICOLON);
					s = new AssignStmt(new IxRef(new IdRef(id, id.posn), e, refPos), eint,
							currentToken.position);
					break;
				case Token.RBRACKET: // Reference ::= ( id [] ) id = Expression; **********************************
					acceptIt();
					//TypeDenoter idbracketid = new ClassType(id, currentToken.position);
					Identifier id1 = parseIdentifier();
					idPos.start = id.posn.start;
					idPos.finish = id1.posn.finish;
					accept(Token.ASSIGN);
					e = parseExpression();
					accept(Token.SEMICOLON);
					//IdRef ir = new IdRef(id, currentToken.position);
					// IxRef ix = new IxRef(ir, e, currentToken.position);
					t = new ArrayType(new ClassType(id, id.posn), id.posn); //id1 instead of id?
					s = new VarDeclStmt(new VarDecl(t, id1, idPos), e, currentToken.position);
					break;

				case Token.THIS: // Statement ::= id [this] = Expression;
					e = parseExpression();
					accept(Token.RBRACKET);
					SourcePosition refPos2 = new SourcePosition();
                    refPos2.start = pos.start;
                    finish(refPos2);
					accept(Token.ASSIGN);
					Expression ethis = parseExpression();
					accept(Token.SEMICOLON);
					s = new AssignStmt(new IxRef(new IdRef(id, id.posn), e, refPos2), ethis,
							currentToken.position);
					break;
				case Token.IDENTIFIER: // Starters of Expression: id [ id ] = Expression;
					e = parseExpression();
					accept(Token.RBRACKET);
					SourcePosition refPos3 = new SourcePosition();
                    refPos3.start = pos.start;
                    finish(refPos3);
					accept(Token.ASSIGN);
					Expression eid = parseExpression();
					accept(Token.SEMICOLON);
					s = new AssignStmt(new IxRef(new IdRef(id, id.posn), e, refPos3), eid,
							currentToken.position);
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
					SourcePosition refPos4 = new SourcePosition();
                    refPos4.start = pos.start;
                    finish(refPos4);
					accept(Token.ASSIGN);
					Expression eallelse = parseExpression();
					accept(Token.SEMICOLON);
					s = new AssignStmt(new IxRef(new IdRef(id, id.posn), e, refPos4),
							eallelse, currentToken.position);
					break;

				default:
					syntacticError("\"]\" or an expression expected here, " + "instead of \"%\"",
							currentToken.spelling);
					break;
				}
				break;

			case Token.IDENTIFIER: // Statement ::= id id = Expression; -> varDeclStmt
				Identifier idid = parseIdentifier();
				idPos.start = id.posn.start;
				idPos.finish = idid.posn.finish;
				accept(Token.ASSIGN);
				e = parseExpression();
				accept(Token.SEMICOLON);
				s = new VarDeclStmt(
						new VarDecl(new ClassType(id, id.posn), idid, idPos), e,
						pos);
				break;

			case Token.DOT: // Statement ::= id (. id)* ([Expression])? =
				SourcePosition idPos1 = new SourcePosition();
                idPos1.start = pos.start;
                finish(idPos1);
				acceptIt();
				Identifier id2 = parseIdentifier();
				QualRef q = new QualRef(new IdRef(id, idPos1), id2, pos);
				while (currentToken.kind == Token.DOT) { // Expression; | (Ar?);
					acceptIt();
					id2 = parseIdentifier();
					q = new QualRef(q, id2, q.posn);
				}

				switch (currentToken.kind) {
				case Token.LBRACKET:
					acceptIt();
					finish(pos);
					e = parseExpression();
					accept(Token.RBRACKET);
					r = new IxRef(q, e, pos);
					accept(Token.ASSIGN);
					e = parseExpression();
					accept(Token.SEMICOLON);
					s = new AssignStmt(r, e, pos);
					break;

				case Token.ASSIGN:
					accept(Token.ASSIGN);
					e = parseExpression();
					accept(Token.SEMICOLON);
					finish(pos);
					s = new AssignStmt(q, e, pos);
					break;

				case Token.LPAREN:
					acceptIt();
					ExprList el = new ExprList();
					if (isArgumentList(currentToken.kind))
						el = parseArgumentList();
					accept(Token.RPAREN);
					accept(Token.SEMICOLON);
					finish(pos);
					s = new CallStmt(q, el, pos);
					break;

				default:
					syntacticError("\"[\", \"=\", or \"(\" expected here, " + "instead of \"%\"",
							currentToken.spelling);
					break;
				}
				break;

			case Token.ASSIGN: // Statement ::= id = Expression;
				acceptIt();
				finish(pos);
				e = parseExpression();
				accept(Token.SEMICOLON);
				s = new AssignStmt(new IdRef(id, id.posn), e, pos);
				break;

			case Token.LPAREN: // Statement ::= id (ArgumentList?);
				acceptIt();
				finish(pos);
				ExprList el = new ExprList();
				if (isArgumentList(currentToken.kind))
					el = parseArgumentList();
				accept(Token.RPAREN);
				accept(Token.SEMICOLON);
				s = new CallStmt(new IdRef(id, id.posn), el, pos);
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
		if (s == null) {
			syntacticError("\"%\" cannot start a statement", currentToken.spelling);
		}
		return s;
	}

	private Expression parseExpr() throws SyntaxError {
		Expression e = null;
		Reference r = null;
		SourcePosition pos = new SourcePosition();
        start(pos);

		switch (currentToken.kind) {
		// CASE NULL
		case Token.NULL:
            NullLiteral nullLiteral = new NullLiteral(currentToken);
            acceptIt();
            finish(pos);
            e = new LiteralExpr(nullLiteral, pos);
            break;
		// CASE 1 + 2
		case Token.THIS: // Reference ::= id | this | Reference.id
			r = parseReference(); // Reference
			if (currentToken.kind == Token.LBRACKET) { // IxReference ::= Reference [ Expression ]
				acceptIt();
				finish(pos);
				r.posn = pos; //PA3
				e = parseExpression();
				accept(Token.RBRACKET);
				r = new IxRef(r, e, r.posn); //PA3
				e = new RefExpr(r, r.posn); //PA3
				// CASE 3
			} else if (currentToken.kind == Token.LPAREN) { // Reference ( ArgumentList? )
				ExprList el = new ExprList();
				acceptIt();
				if (isArgumentList(currentToken.kind))
					el = parseArgumentList();
				accept(Token.RPAREN);
				finish(pos);
				e = new CallExpr(r, el, pos);
			} else {
				finish(pos);
				e = new RefExpr(r, pos);
			}
			break;
		case Token.IDENTIFIER:
			// RefExpr --> ThisRef(SourcePosition posn)
			// RefExpr --> IdRef(Identifier id, SourcePosition posn)
			// RefExpr --> QualRef(Reference ref, Identifier id, SourcePosition posn)
			r = parseReference(); // Reference
			if (currentToken.kind == Token.LBRACKET) { // IxReference ::= Reference [ Expression ]
				acceptIt();
				e = parseExpression();
				accept(Token.RBRACKET);
				finish(pos);
				r = new IxRef(r, e, pos);
				e = new RefExpr(r, pos);
				// CASE 3
			} else if (currentToken.kind == Token.LPAREN) { // Reference ( ArgumentList? )
				ExprList el = new ExprList();
				acceptIt();
				if (isArgumentList(currentToken.kind))
					el = parseArgumentList();
				accept(Token.RPAREN);
				finish(pos);
				e = new CallExpr(r, el, pos);
			} else {
				finish(pos);
				e = new RefExpr(r, pos);
			}
			break;

		// CASE 4+5: Dealt with via precedence (parseExpression())

		// CASE 6
		case Token.LPAREN: // ( Expression )
			acceptIt();
			e = parseExpression();
			accept(Token.RPAREN);
			break;

		// CASE 7
		case Token.INTLITERAL: // num | true | false
			if (currentToken.spelling.length()>1 && currentToken.spelling.charAt(0) == '0') {
				syntacticError("octal number", currentToken.spelling);
				break;
			}
			finish(pos);
			e = new LiteralExpr(new IntLiteral(currentToken), pos);
			acceptIt();
			break;
		case Token.TRUE:
		case Token.FALSE:
			finish(pos);
			e = new LiteralExpr(new BooleanLiteral(currentToken), pos);
			acceptIt();
			break;

		// CASE 8
		case Token.NEW: // new ( id () | int [ Expression ] | id [ Expression ] )
			acceptIt();
			if (currentToken.kind == Token.INT) {
				SourcePosition intPos = currentToken.position;
				acceptIt();
				accept(Token.LBRACKET);
				Expression e1 = parseExpression();
				accept(Token.RBRACKET);
				finish(pos);
				e = new NewArrayExpr(new BaseType(TypeKind.INT, intPos), e1, pos);
			} else if (currentToken.kind == Token.IDENTIFIER) {
				Identifier id = new Identifier(currentToken);
				acceptIt();
				if (currentToken.kind == Token.LBRACKET) {
					acceptIt();
					Expression e1 = parseExpression();
					accept(Token.RBRACKET);
					finish(pos);
					e = new NewArrayExpr(new ClassType(id, id.posn), e1, pos);
				} else if (currentToken.kind == Token.LPAREN) {
					acceptIt();
					accept(Token.RPAREN);
					finish(pos);
					e = new NewObjectExpr(new ClassType(id, id.posn), pos);
				}
			} else {
				syntacticError("\"%\" cannot start a new expression", currentToken.spelling);
			}
			break;

		default:
			syntacticError("\"%\" cannot start an expression", currentToken.spelling);
			break;
		}
		
		if (e == null) {
			syntacticError("\"%\" cannot start an expression", currentToken.spelling);
		}

		return e;
	}

	private Identifier parseIdentifier() throws SyntaxError {
		Identifier id = null;
		if (currentToken.kind == Token.IDENTIFIER) {
			id = new Identifier(currentToken);
			previousTokenPosition = currentToken.position;
			currentToken = lexicalAnalyzer.scan();
		} else {
			syntacticError("\"%\" expected here, instead of \"" + currentToken.spelling + "\"",
					Token.spell(Token.IDENTIFIER));
		}
		return id;
	}

	private Expression parseExpression() throws SyntaxError {
		Expression e = parseOrExpr();
		return e;
	}

	private Expression parseOrExpr() throws SyntaxError { // ||
		SourcePosition pos = new SourcePosition();
        start(pos);
		Expression e1 = parseAndExpr();
		while (currentToken.kind == Token.OR) {
			Operator o = new Operator(currentToken);
			acceptIt();
			Expression e2 = parseAndExpr();
			finish(pos);
			e1 = new BinaryExpr(o, e1, e2, pos);
		}
		return e1;
	}

	private Expression parseAndExpr() throws SyntaxError { // &&
		SourcePosition pos = new SourcePosition();
        start(pos);
		Expression e1 = parseEqualExpr();
		while (currentToken.kind == Token.AND) {
			Operator o = new Operator(currentToken);
			acceptIt();
			Expression e2 = parseEqualExpr();
			finish(pos);
			e1 = new BinaryExpr(o, e1, e2, pos);
		}
		return e1;
	}

	private Expression parseEqualExpr() throws SyntaxError { // == | !=
		SourcePosition pos = new SourcePosition();
        start(pos);
		Expression e1 = parseLessGreaterExpr();
		while (currentToken.kind == Token.EQUAL || currentToken.kind == Token.NOTEQUAL) {
			Operator o = new Operator(currentToken);
			acceptIt();
			Expression e2 = parseLessGreaterExpr();
			finish(pos);
			e1 = new BinaryExpr(o, e1, e2, pos);
		}
		return e1;
	}

	private Expression parseLessGreaterExpr() throws SyntaxError { // <= | >= | < | >
		SourcePosition pos = new SourcePosition();
        start(pos);
		Expression e1 = parsePlusMinusExpr();
		while (currentToken.kind == Token.GEQUAL || currentToken.kind == Token.LEQUAL
				|| currentToken.kind == Token.GREATER || currentToken.kind == Token.LESS) {
			Operator o = new Operator(currentToken);
			acceptIt();
			Expression e2 = parsePlusMinusExpr();
			finish(pos);
			e1 = new BinaryExpr(o, e1, e2, pos);
		}
		return e1;
	}

	private Expression parsePlusMinusExpr() throws SyntaxError {
		SourcePosition pos = new SourcePosition();
        start(pos);
		Expression e1 = parseTimesDivExpr();
		while (currentToken.kind == Token.PLUS || currentToken.kind == Token.MINUS) {
			Operator o = new Operator(currentToken);
			acceptIt();
			Expression e2 = parseTimesDivExpr();
			finish(pos);
			e1 = new BinaryExpr(o, e1, e2, pos);
		}
		return e1;
	}

	private Expression parseTimesDivExpr() throws SyntaxError {
		SourcePosition pos = new SourcePosition();
        start(pos);
		Expression e1 = parseUnaryExpr();
		while (currentToken.kind == Token.TIMES || currentToken.kind == Token.DIV) {
			Operator o = new Operator(currentToken);
			acceptIt();
			Expression e2 = parseUnaryExpr();
			finish(pos);
			e1 = new BinaryExpr(o, e1, e2, pos);
		}
		return e1;
	}

	private Expression parseUnaryExpr() throws SyntaxError {
		Expression e;
		SourcePosition pos = new SourcePosition();
        start(pos);
		if (currentToken.kind == Token.NOT || currentToken.kind == Token.MINUS) {
			Operator o = new Operator(currentToken);
			acceptIt();
			Expression e1 = parseUnaryExpr();
			finish(pos);
			e = new UnaryExpr(o, e1, pos);
		} else {
			e = parseExpr();
		}
		return e;
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
