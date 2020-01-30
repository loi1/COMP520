/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class BaseType extends TypeDenoter {
	public BaseType(TypeKind t, SourcePosition posn) {
		super(t, posn);
	}

	public <A, R> R visit(Visitor<A, R> v, A o) {
		return v.visitBaseType(this, o);
	}

	// pa3
	@Override
	public boolean equals(Object obj) {
		if (this.typeKind == TypeKind.NULL) {
            return true;
        } else if (this.typeKind == TypeKind.UNSUPPORTED || this.typeKind == TypeKind.ERROR) {
            return false;
        } else if (obj == null || ((TypeDenoter)obj).typeKind == TypeKind.UNSUPPORTED){
            return false;
        } else if (((TypeDenoter)obj).typeKind == TypeKind.NULL || ((TypeDenoter)obj).typeKind == TypeKind.ERROR){
            return true;
        } else if (obj instanceof BaseType && this.typeKind == ((BaseType)obj).typeKind){
            return true;
        } else{
            return false;
        }
	}

	@Override
	public Declaration getDecl() {
		return null;
	}
}
