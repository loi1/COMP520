/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class IxRef extends Reference {
	
	public IxRef(Reference ref, Expression expr, SourcePosition posn){
		super(posn);
		this.ref = ref;
		this.indexExpr = expr;
		// this.id = ???
	}

	public <A,R> R visit(Visitor<A,R> v, A o){
		return v.visitIxRef(this, o);
	}
	
	public Reference ref;
	public Expression indexExpr;
	
	// pa3
	public Identifier id;
	@Override
	public TypeDenoter getType() {
		TypeDenoter type = id.decl.type;
		if (type instanceof ArrayType) {
			return ((ArrayType)type).eltType;
		} else {
			return null;
		}
	}

	@Override
	public Declaration getDecl() {
		return id.decl;
	}
}
