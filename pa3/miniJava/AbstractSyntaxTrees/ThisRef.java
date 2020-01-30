/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ThisRef extends BaseRef {
	
	public ThisRef(SourcePosition posn) {
		super(posn);
	}

	@Override
	public <A, R> R visit(Visitor<A, R> v, A o) {
		return v.visitThisRef(this, o);
	}
	
	// pa3 - to assign ref decl type and decl itself
	@Override
	public TypeDenoter getType() {
		return declaration.type;
	}

	@Override
	public Declaration getDecl() {
		return declaration;
	}

	public Declaration declaration;
}
