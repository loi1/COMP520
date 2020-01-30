/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import java.util.HashMap;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ClassDecl extends Declaration {

	public ClassDecl(Identifier id, FieldDeclList fdl, MethodDeclList mdl, SourcePosition posn) {
		super(id, null, posn); // new ClassType(new Identifier(new Token(TokenKind.IDENTIFIER, id.spelling,
								// posn)), posn)
		fieldDeclList = fdl;
		methodDeclList = mdl;
	}

	public <A, R> R visit(Visitor<A, R> v, A o) {
		return v.visitClassDecl(this, o);
	}

	public FieldDeclList fieldDeclList;
	public MethodDeclList methodDeclList;

	// pa3
	public boolean existsMember(String name, boolean expectStatic, boolean expectPublic) {
		for (FieldDecl fd : fieldDeclList) {
			if (name.equals(fd.id.spelling)) {
				if (expectStatic && !fd.isStatic) continue;
                if (expectPublic && fd.isPrivate) continue;
                return true;
			}
		}
		for (MethodDecl md : methodDeclList) {
			if (name.equals(md.id.spelling)) {
				if (expectStatic && !md.isStatic) continue;
                if (expectPublic && md.isPrivate) continue;
                return true;
			}
		}
		return false;
	}
}
