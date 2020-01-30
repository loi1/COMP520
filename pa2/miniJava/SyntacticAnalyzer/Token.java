package miniJava.SyntacticAnalyzer;

/**
 * A token has a kind and a spelling In a compiler it would also have a source
 * position
 */
public class Token extends Object {
	public int kind;
	public String spelling;
	public SourcePosition position;

	public Token(int kind, String spelling, SourcePosition position) {
        this.kind = kind;
        this.spelling = spelling;
        this.position = position;

        if(kind == IDENTIFIER) {
            for(int k = firstReservedWord; k <= lastReservedWord; k++) {
                if(spelling.equals(tokenTable[k])) {
                    this.kind = k;
                    break;
                }
            }
        }
    }

	public static String spell(int kind) {
		return tokenTable[kind];
	}

	public String toString() {
		return "Kind=" + kind + ", spelling=" + spelling + ", position=" + position;
	}

	// Token classes...
	public static final int
	// literals, identifiers, operators
	INTLITERAL = 0, IDENTIFIER = 1, OPERATOR = 2, GREATER = 3, LESS = 4, EQUAL = 5, LEQUAL = 6, GEQUAL = 7,
			NOTEQUAL = 8, AND = 9, OR = 10, NOT = 11, PLUS = 12, MINUS = 13, TIMES = 14, DIV = 15, ASSIGN = 16,
			// Reserved words
			CLASS = 17, RETURN = 18, PUBLIC = 19, PRIVATE = 20, STATIC = 21, INT = 22, BOOLEAN = 23, VOID = 24,
			THIS = 25, IF = 26, ELSE = 27, WHILE = 28, TRUE = 29, FALSE = 30, NEW = 31,
			// Punctuation
			DOT = 32, COMMA = 33, SEMICOLON = 34,
			// Parenthesis/Brackets/Braces
			LPAREN = 35, RPAREN = 36, LBRACKET = 37, RBRACKET = 38, LCURLY = 39, RCURLY = 40,
			// EOT/Error tokens
			EOT = 41, ERROR = 42;

	private static String[] tokenTable = new String[] { "<int>", "<identifier>", "<operator>", ">", "<", "==", "<=",
			">=", "!=", "&&", "||", "!", "+", "-", "*", "/", "=", "class", "return", "public", "private", "static",
			"int", "boolean", "void", "this", "if", "else", "while", "true", "false", "new", ".", ",", ";", "(", ")",
			"[", "]", "{", "}", "", "<error>" };

	private final static int firstReservedWord = Token.CLASS, lastReservedWord = Token.NEW;
}
