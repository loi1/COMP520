/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ParameterDecl extends LocalDecl {
	
	public ParameterDecl(TypeDenoter t, Identifier id, SourcePosition posn){
		super(id, t, posn);
	}
	
	public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitParameterDecl(this, o);
    }
}

