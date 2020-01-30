/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class Declaration extends AST {

	public Declaration(Identifier id, TypeDenoter type, SourcePosition posn) {
		super(posn);
		this.id = id;
		this.type = type;
	}

	public TypeDenoter type;
	public Identifier id; // pa3: was public String name;

	// pa3
	public boolean setIdBinding() {
		id.decl = this;
		id.type = this.type;
		return true;
	}
}
