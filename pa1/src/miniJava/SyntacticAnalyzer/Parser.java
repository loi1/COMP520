package miniJava.SyntacticAnalyzer;

import java.util.Arrays;
import java.util.List;

import miniJava.ErrorReporter;

public class Parser { // creates tree out of tokens
	private Scanner lexicalAnalyzer;
	private ErrorReporter errorReporter;
	private Token currentToken;
	private SourcePosition previousTokenPosition;

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

	public void parse() {
		currentToken = lexicalAnalyzer.scan();

		try {
			parseProgram();
		} catch (SyntaxError s) {
			System.out.println("The syntax error has been caught...");
		}
	}

	private void parseProgram() throws SyntaxError {
		while (currentToken.kind == Token.CLASS) {
			parseClassDeclaration();
		}
		accept(Token.EOT);
	}

	private void parseClassDeclaration() throws SyntaxError {
		accept(Token.CLASS);
		parseIdentifier();
		accept(Token.LCURLY); // ClassDeclaration ::= class id {

		while (isDeclarator(currentToken.kind)) {
			parseDeclarators();
			parseIdentifier();

			switch (currentToken.kind) {
			case Token.SEMICOLON:
				acceptIt();
				break;

			case Token.LPAREN:
				acceptIt();

				if (isParameterList(currentToken.kind))
					parseParameterList();

				accept(Token.RPAREN);
				accept(Token.LCURLY);

				while (isStatement(currentToken.kind))
					parseStatement();

				if (currentToken.kind == Token.RETURN) {
					acceptIt();
					parseExpression();
					accept(Token.SEMICOLON);
				}

				accept(Token.RCURLY);
				break;
			default:
				syntacticError("\";\" or \"(\" expected here, instead of " + "\"%\"", currentToken.spelling);
				break;
			}
		}
		accept(Token.RCURLY);
	}

	private void parseDeclarators() throws SyntaxError { // Visibility Access Type
		if (currentToken.kind == Token.PUBLIC || currentToken.kind == Token.PRIVATE)
			acceptIt();
		if (currentToken.kind == Token.STATIC)
			acceptIt();
		parseType();
	}

	private void parseType() throws SyntaxError {
		switch (currentToken.kind) {
		case Token.BOOLEAN:
		case Token.VOID:
			acceptIt();
			break;

		case Token.IDENTIFIER:
			parseIdentifier();
			if (currentToken.kind == Token.LBRACKET) {
				acceptIt();
				accept(Token.RBRACKET);
			}
			break;

		case Token.INT:
			acceptIt();
			if (currentToken.kind == Token.LBRACKET) {
				acceptIt();
				accept(Token.RBRACKET);
			}
			break;

		default:
			syntacticError("\"%\" cannot start a type", currentToken.spelling);
			break;
		}
	}

	private void parseParameterList() throws SyntaxError {
		parseType();
		parseIdentifier();

		while (currentToken.kind == Token.COMMA) {
			acceptIt();
			parseType();
			parseIdentifier();
		}
	}

	private void parseArgumentList() throws SyntaxError {
		parseExpression();

		while (currentToken.kind == Token.COMMA) {
			acceptIt();
			parseExpression();
		}
	}

	private void parseReference() throws SyntaxError {
		if (currentToken.kind == Token.THIS)
			acceptIt();
		else if (currentToken.kind == Token.IDENTIFIER)
			acceptIt();
		else
			syntacticError("\"%\" cannot start a reference", currentToken.spelling);

		while (currentToken.kind == Token.DOT) {
			acceptIt();
			parseIdentifier();
		}
	}

	private void parseStatement() throws SyntaxError {
		switch (currentToken.kind) {
		case Token.LCURLY:
			acceptIt();
			while (isStatement(currentToken.kind))
				parseStatement();
			accept(Token.RCURLY);
			break;

		case Token.IF: //Statement ::= if (Expression) Statement (else Statement)?
			acceptIt();
			accept(Token.LPAREN);
			parseExpression();
			accept(Token.RPAREN);
			parseStatement();
			if (currentToken.kind == Token.ELSE) {
				acceptIt();
				parseStatement();
			}
			break;

		case Token.WHILE: //Statement ::= while (Expression) Statement
			acceptIt();
			accept(Token.LPAREN);
			parseExpression();
			accept(Token.RPAREN);
			parseStatement();
			break;

		case Token.BOOLEAN: 
		case Token.VOID:
		case Token.INT: 
			parseType();
			parseIdentifier();
			accept(Token.ASSIGN);
			parseExpression();
			accept(Token.SEMICOLON);
			break;

		case Token.THIS: // Statement ::= Reference ([Expression])? =
			parseReference(); // Expression; | (ArgumentList?);
			switch (currentToken.kind) {
			case Token.LBRACKET:
			case Token.ASSIGN:
				if (currentToken.kind == Token.LBRACKET) {
					acceptIt();
					parseExpression();
					accept(Token.RBRACKET);
				}

				accept(Token.ASSIGN);
				parseExpression();
				accept(Token.SEMICOLON);
				break;

			case Token.LPAREN:
				acceptIt();
				if (isArgumentList(currentToken.kind))
					parseArgumentList();
				accept(Token.RPAREN);
				accept(Token.SEMICOLON);
				break;

			default:
				syntacticError("\"[\", \"=\", or \"(\" expected here, " + "instead of \"%\"", currentToken.spelling);
				break;
			}
			break;

		case Token.IDENTIFIER:
			parseIdentifier();

			switch (currentToken.kind) {
			case Token.LBRACKET:
				acceptIt();

				switch (currentToken.kind) {
				case Token.RBRACKET: // Statement ::= id [] id = Expression;
					acceptIt();
					parseIdentifier();
					accept(Token.ASSIGN);
					parseExpression();
					accept(Token.SEMICOLON);
					break;

				case Token.THIS: // Statement ::= id [Expression] = Expression;
				case Token.IDENTIFIER: // Starters of Expression
				case Token.NOT:
				case Token.MINUS:
				case Token.LPAREN:
				case Token.INT:
				case Token.TRUE:
				case Token.FALSE:
				case Token.NEW:
					parseExpression();
					accept(Token.RBRACKET);
					accept(Token.ASSIGN);
					parseExpression();
					accept(Token.SEMICOLON);
					break;

				default:
					syntacticError("\"]\" or an expression expected here, " + "instead of \"%\"",
							currentToken.spelling);
					break;
				}
				break;

			case Token.IDENTIFIER: // Statement ::= id id = Expression;
				parseIdentifier();
				accept(Token.ASSIGN);
				parseExpression();
				accept(Token.SEMICOLON);
				break;

			case Token.DOT: // Statement ::= id (. id)* ([Expression])? =
				while (currentToken.kind == Token.DOT) { // Expression; | (Ar?);
					acceptIt();
					parseIdentifier();
				}

				switch (currentToken.kind) {
				case Token.LBRACKET:
				case Token.ASSIGN:
					if (currentToken.kind == Token.LBRACKET) {
						acceptIt();
						parseExpression();
						accept(Token.RBRACKET);
					}
					accept(Token.ASSIGN);
					parseExpression();
					accept(Token.SEMICOLON);
					break;

				case Token.LPAREN:
					acceptIt();
					if (isArgumentList(currentToken.kind))
						parseArgumentList();
					accept(Token.RPAREN);
					accept(Token.SEMICOLON);
					break;

				default:
					syntacticError("\"[\", \"=\", or \"(\" expected here, " + "instead of \"%\"",
							currentToken.spelling);
					break;
				}
				break;

			case Token.ASSIGN: // Statement ::= id = Expression;
				acceptIt();
				parseExpression();
				accept(Token.SEMICOLON);
				break;

			case Token.LPAREN: // Statement ::= id (ArgumentList?);
				acceptIt();
				if (isArgumentList(currentToken.kind))
					parseArgumentList();
				accept(Token.RPAREN);
				accept(Token.SEMICOLON);
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
	}

	private void parseExpression() throws SyntaxError {
		switch (currentToken.kind) {
		case Token.THIS: // Reference
		case Token.IDENTIFIER:
			parseReference();
			if (currentToken.kind == Token.LBRACKET) {
				acceptIt();
				parseExpression();
				accept(Token.RBRACKET);
			} else if (currentToken.kind == Token.LPAREN) {
				acceptIt();
				if (isArgumentList(currentToken.kind))
					parseArgumentList();
				accept(Token.RPAREN);
			}
			break;

		case Token.NOT:
		case Token.MINUS:
			acceptIt();
			parseExpression();
			break;

		case Token.LPAREN:
			acceptIt();
			parseExpression();
			accept(Token.RPAREN);
			break;

		case Token.INTLITERAL:
		case Token.TRUE:
		case Token.FALSE:
			acceptIt();
			break;

		case Token.NEW:
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
	}

	private void parseIdentifier() throws SyntaxError {
		if (currentToken.kind == Token.IDENTIFIER) {
			previousTokenPosition = currentToken.position;
			currentToken = lexicalAnalyzer.scan();
		} else {
			syntacticError("\"%\" expected here, instead of \"" + currentToken.spelling + "\"",
					Token.spell(Token.IDENTIFIER));
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////

	private boolean isDeclarator(int kind) {
		List<Integer> tokens = Arrays.asList(Token.PUBLIC, Token.PRIVATE, Token.STATIC);
		return tokens.contains(kind) || isType(kind);
	}

	private boolean isType(int kind) {
		List<Integer> tokens = Arrays.asList(Token.INT, Token.BOOLEAN, Token.IDENTIFIER, Token.VOID);
		return tokens.contains(kind);
    }
	
	private boolean isParameterList(int kind) {
		return isType(kind);
	}
	
	private boolean isArgumentList(int kind) {
		List<Integer> tokens = Arrays.asList(Token.NOT, Token.MINUS, Token.LPAREN, Token.INTLITERAL, Token.TRUE, Token.FALSE, Token.NEW);
		return isReference(kind) || tokens.contains(kind);
	}
	
	private boolean isReference(int kind) {
		List<Integer> tokens = Arrays.asList(Token.IDENTIFIER, Token.THIS);
		return tokens.contains(kind);
	}

	private boolean isStatement(int kind) {
		List<Integer> tokens = Arrays.asList(Token.LCURLY, Token.IF, Token.WHILE, Token.THIS);
		return isType(kind) || tokens.contains(kind);
	}

	private boolean isBinop(int kind) {
		List<Integer> tokens = Arrays.asList(Token.GREATER, Token.LESS, Token.EQUAL, Token.GEQUAL, Token.LEQUAL,
				Token.NOTEQUAL, Token.AND, Token.OR, Token.PLUS, Token.MINUS, Token.TIMES, Token.DIV);
		return tokens.contains(kind);
	}
}
