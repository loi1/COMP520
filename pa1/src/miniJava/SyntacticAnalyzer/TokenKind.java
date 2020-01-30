package miniJava.SyntacticAnalyzer;

public enum TokenKind {
	// literals, identifiers, operators...
	INTLITERAL, CHARLITERAL, IDENTIFIER, OPERATOR,

	// reserved words - must be in alphabetical order...
	ARRAY, BEGIN, CONST, DO, ELSE, END, FUNC, IF, IN, LET, OF, PROC, RECORD, THEN, TYPE, VAR, WHILE,

	// punctuation...
	DOT, COLON, SEMICOLON, COMMA, BECOMES, IS,

	// brackets...
	LPAREN, RPAREN, LBRACKET, RBRACKET, LCURLY, RCURLY,

	// special tokens...
	EOT, ERROR
}
