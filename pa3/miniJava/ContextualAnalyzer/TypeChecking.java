package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.SourcePosition;

public class TypeChecking implements Visitor<TypeDenoter, TypeDenoter> {

	ClassDecl currClass;
	private AST decoratedAST;
	private ErrorReporter errorReporter;
	public boolean userDefinedString;
	boolean containsMain;

	public TypeChecking(AST ast, ErrorReporter errorReporter, boolean userDefinedString) {
		this.decoratedAST = ast;
		this.errorReporter = errorReporter;
		this.userDefinedString = userDefinedString;
	}

	public void check() {
		decoratedAST.visit(this, null);
		System.out.println("Type checking complete, success.");
	}

	// Package //

	@Override
	public TypeDenoter visitPackage(Package p, TypeDenoter arg) {
		ClassDeclList cdl = p.classDeclList;
		for (ClassDecl cd : cdl) {
			cd.visit(this, arg);
		}
		return null;
	}

	// Declaration //

	@Override
	public TypeDenoter visitClassDecl(ClassDecl cd, TypeDenoter arg) {
		currClass = cd;
		cd.id.visit(this, arg);
		for (FieldDecl fd : cd.fieldDeclList) {
			fd.visit(this, arg);
		}
		for (MethodDecl md : cd.methodDeclList) {
			md.visit(this, arg);
		}
		return null;
	}

	@Override
	public TypeDenoter visitFieldDecl(FieldDecl fd, TypeDenoter arg) {
		fd.type = fd.type.visit(this, arg);
		if (fd.type.typeKind == TypeKind.VOID) {
			errorReporter
					.reportError("*** line " + fd.posn.start + ": Type error - Field " + fd.id.spelling + " is void");
		}
		return fd.type;
	}

	@Override
	public TypeDenoter visitMethodDecl(MethodDecl md, TypeDenoter arg) {
		// check main method
		md.type = md.type.visit(this, arg);
		if (md.id.spelling.equals("main")) {
			if (containsMain) {
				errorReporter.reportError("*** line " + md.posn.start + ": Type error - only one main method allowed");

			}
			containsMain = true;
			TypeDenoter typeDenoter = md.parameterDeclList.get(0).visit(this, arg);
			// check for main parameter String[]
			if (!(typeDenoter instanceof ArrayType)) {
				errorReporter.reportError(
						"*** line " + md.posn.start + ": Type error - main method must have String[] as parameter");

			} else if (((ArrayType) typeDenoter).eltType.typeKind != TypeKind.UNSUPPORTED
					&& !((ClassType) ((ArrayType) typeDenoter).eltType).className.spelling.equals("String")) {
				errorReporter.reportError(
						"*** line " + md.posn.start + ": Type error - main method must have String[] as parameter");

			}
		}

		md.returnType = null;
		for (ParameterDecl pd : md.parameterDeclList) {
			pd.visit(this, arg);
		}
		for (Statement stmt : md.statementList) {
			stmt.methodDecl = md;
			stmt.visit(this, arg);
		}

		// check void
		TypeDenoter td = md.type;
		if (td.typeKind == TypeKind.VOID) {
			if (md.returnType != null && md.returnType.typeKind != TypeKind.VOID) {
				errorReporter.reportError("*** line " + md.posn.start
						+ ": Type error - cannot return value in void-type method: " + md.id.spelling);
			}
			if (md.statementList.size() == 0
					|| !(md.statementList.get(md.statementList.size() - 1) instanceof ReturnStmt)) {
				md.statementList.add(new ReturnStmt(null, new SourcePosition(md.posn.finish, md.posn.finish)));
			}
		} else {
			if (!(md.statementList.get(md.statementList.size() - 1) instanceof ReturnStmt)) {
				errorReporter.reportError("*** line " + md.posn.finish
						+ ":  Type error - no return statement at end of method: " + md.id.spelling);
			}
			if (md.returnType == null) {
				errorReporter.reportError("*** line " + md.posn.start
						+ ":  Type error - null return for non-void method: " + md.id.spelling);
			} else {
				TypeDenoter returnType = md.returnType.visit(this, arg);
				checkUnSupported(returnType);
				checkError(returnType);
				checkUnSupported(td);
				checkError(td);
				if (!returnType.equals(td)) {
					errorReporter.reportError("*** line " + md.posn.start
							+ ":  Type error - returning type doesn't match method type: " + md.id.spelling);
				}
			}
		}
		return md.type;
	}

	@Override
	public TypeDenoter visitParameterDecl(ParameterDecl pd, TypeDenoter arg) {
		pd.type = pd.type.visit(this, arg);
		if (pd.type.typeKind == TypeKind.VOID) {
			errorReporter.reportError("*** line " + pd.posn.start + ":  Type error - parameter argument "
					+ pd.id.spelling + " cannot be type VOID");
		}
		return pd.type;
	}

	@Override
	public TypeDenoter visitVarDecl(VarDecl vd, TypeDenoter arg) {
		vd.type = vd.type.visit(this, arg);
		if (vd.type.typeKind == TypeKind.VOID) {
			errorReporter.reportError("*** line " + vd.posn.start + ":  Type error - declared variable "
					+ vd.id.spelling + " cannot be type VOID");
		}
		return vd.type;
	}

	// TypeDenoters //

	@Override
	public TypeDenoter visitBaseType(BaseType type, TypeDenoter arg) {
		return type;
	}

	@Override
	public TypeDenoter visitClassType(ClassType type, TypeDenoter arg) {
		if (type.className.spelling.equals("String")) {
			if (userDefinedString)
				return type;
			else
				return new BaseType(TypeKind.UNSUPPORTED, type.posn);
		}
		return type;
	}

	@Override
	public TypeDenoter visitArrayType(ArrayType type, TypeDenoter arg) {
		TypeDenoter typeDenoter = type.eltType.visit(this, arg);
		return new ArrayType(typeDenoter, type.posn);
	}

	// Statements //

	@Override
	public TypeDenoter visitBlockStmt(BlockStmt bs, TypeDenoter arg) {
		for (Statement stmt : bs.sl) {
			stmt.methodDecl = bs.methodDecl;
			stmt.visit(this, arg);
		}
		return null;
	}

	@Override
	public TypeDenoter visitVardeclStmt(VarDeclStmt stmt, TypeDenoter arg) {
		TypeDenoter declaredType = stmt.varDecl.visit(this, arg);
		if (stmt.initExp != null) {
			TypeDenoter assignedType = stmt.initExp.visit(this, arg);
			checkUnSupported(assignedType);
			checkError(assignedType);
			if (!declaredType.equals(assignedType)) {
				errorReporter.reportError("*** line " + stmt.posn.start
						+ ":  Type error - variable value type is not the same as variable type");

			}
		}
		return null;
	}

	@Override
	public TypeDenoter visitAssignStmt(AssignStmt stmt, TypeDenoter arg) {
		TypeDenoter refType = stmt.ref.visit(this, arg);
		TypeDenoter valType = stmt.val.visit(this, arg);
		checkUnSupported(refType);
		checkError(refType);
		checkUnSupported(valType);
		checkError(valType);
		if (!refType.equals(valType)) {
			errorReporter.reportError(
					"*** line " + stmt.posn.start + ":  Type error - left and right sides of assign statement are not the same types");

		}
		return null;
	}

	@Override
	public TypeDenoter visitCallStmt(CallStmt stmt, TypeDenoter arg) {
		if (stmt.methodRef.getDecl() instanceof MethodDecl) {
			MethodDecl methodDecl = (MethodDecl) stmt.methodRef.getDecl();
			if (stmt.argList.size() != methodDecl.parameterDeclList.size()) {
				errorReporter.reportError("*** line " + methodDecl.posn.start + ":  Type error - method "
						+ methodDecl.id.spelling + ": wrong # of arguments");

			} else {
				for (int i = 0; i < stmt.argList.size(); i++) {
					TypeDenoter actualType = stmt.argList.get(i).visit(this, arg);
					TypeDenoter declaredType = methodDecl.parameterDeclList.get(i).type;
					if (!declaredType.equals(actualType)) {
						errorReporter.reportError("*** line " + stmt.posn.start + ":  Type error - method "
								+ methodDecl.id.spelling + ": #" + (i + 1) + " argument is wrong type");

					}
				}
			}
		} else {
			errorReporter.reportError("*** line " + stmt.posn.start + ":  Type error - can't call non-method");
		}

		return null;
	}

	@Override
	public TypeDenoter visitReturnStmt(ReturnStmt stmt, TypeDenoter arg) {
		TypeDenoter type;
		if (stmt.returnExpr == null) {
			type = new BaseType(TypeKind.VOID, stmt.posn);
		} else {
			type = stmt.returnExpr.visit(this, arg);
		}

		if (stmt.methodDecl.returnType != null) {
			errorReporter.reportError("*** line " + stmt.posn.start
					+ ":  Type error - returns different value type in method " + stmt.methodDecl.id.spelling);

		} else {
			stmt.methodDecl.returnType = type;
		}
		return null;
	}

	@Override
	public TypeDenoter visitIfStmt(IfStmt stmt, TypeDenoter arg) {
		// check if condition is type boolean
		TypeDenoter conditionType = stmt.cond.visit(this, arg);
		if (conditionType == null || conditionType.typeKind != TypeKind.BOOLEAN) {
			errorReporter
					.reportError("*** line " + stmt.posn.start + ":  Type error - if condition is not type boolean");
		}
		if (stmt.thenStmt instanceof VarDeclStmt) {
			errorReporter.reportError("*** line " + stmt.posn.start + ":  Type error - cannot declare in then branch of if statement");
		}
		stmt.thenStmt.visit(this, arg);
		if (stmt.elseStmt != null) {
			if (stmt.elseStmt instanceof VarDeclStmt) {
				errorReporter.reportError("*** line " + stmt.posn.start + ":  Type error - cannot declare in branch");

			}
			// stmt.elseStmt.methodDecl = stmt.methodDecl;
			stmt.elseStmt.visit(this, arg);
		}
		return null;
	}

	@Override
	public TypeDenoter visitWhileStmt(WhileStmt stmt, TypeDenoter arg) {
		// check for boolean condition type
		TypeDenoter conditionType = stmt.cond.visit(this, arg);
		if (conditionType == null || conditionType.typeKind != TypeKind.BOOLEAN) {
			errorReporter
					.reportError("*** line " + stmt.posn.start + ":  Type error - while condition is not boolean type");
		}
		if (stmt.body instanceof VarDeclStmt) {
			errorReporter.reportError("*** line " + stmt.posn.start + ":  Type error - cannot declare in while branch");
		}
		stmt.body.methodDecl = stmt.methodDecl;
		stmt.body.visit(this, arg);
		return null;
	}

	// Expressions //

	@Override
	public TypeDenoter visitUnaryExpr(UnaryExpr expr, TypeDenoter arg) {
		String op = expr.operator.spelling;
		TypeDenoter typeDenoter = expr.expr.visit(this, arg);
		if (op.equals("-")) {
			if (typeDenoter.typeKind == TypeKind.INT) {
				return new BaseType(TypeKind.INT, new SourcePosition());
			} else {
				errorReporter.reportError(
						"*** line " + expr.posn.start + ":  Type error - unaryexpr: variable after - is not int type");
				return new BaseType(TypeKind.ERROR, new SourcePosition());
			}
		} else if (op.equals("!")) {
			if (typeDenoter.typeKind == TypeKind.BOOLEAN) {
				return new BaseType(TypeKind.BOOLEAN, new SourcePosition());
			} else {
				errorReporter.reportError("*** line " + expr.posn.start
						+ ":  Type error in UnaryExpr: variable after ! operator is not boolean type");
				return new BaseType(TypeKind.ERROR, new SourcePosition());
			}
		} else {
			errorReporter.reportError(
					"*** line " + expr.posn.start + ":  Type error - UnaryExpr: incorrect operator, neither - or !");
			return new BaseType(TypeKind.ERROR, new SourcePosition());
		}
	}

	@Override
	public TypeDenoter visitBinaryExpr(BinaryExpr expr, TypeDenoter arg) {
		String op = expr.operator.spelling;
		TypeDenoter leftType = expr.left.visit(this, null);
		TypeDenoter rightType = expr.right.visit(this, null);
		if (op.equals("||") || op.equals("&&")) {
			if (leftType.typeKind == TypeKind.BOOLEAN && rightType.typeKind == TypeKind.BOOLEAN) {
				return new BaseType(TypeKind.BOOLEAN, new SourcePosition());
			} else {
				errorReporter.reportError(
						"*** line " + expr.posn.start + ":  Type error - || or && can't be used with non-boolean types");
				return new BaseType(TypeKind.ERROR, new SourcePosition());
			}
		} else if (op.equals("==") || op.equals("!=")) {
			checkUnSupported(leftType);
			checkError(leftType);
			checkUnSupported(rightType);
			checkError(rightType);
			if (leftType.equals(rightType)) { // require the same type
				return new BaseType(TypeKind.BOOLEAN, new SourcePosition());
			} else {
				errorReporter.reportError(
						"*** line " + expr.posn.start + ":  Type error - == or != are for comparing the same type only");
				return new BaseType(TypeKind.ERROR, new SourcePosition());
			}
		} else if (op.equals("<=") || op.equals(">=") || op.equals(">") || op.equals("<")) {
			if (leftType.typeKind == TypeKind.INT && rightType.typeKind == TypeKind.INT) {
				return new BaseType(TypeKind.BOOLEAN, new SourcePosition());
			} else {
				errorReporter
						.reportError("*** line " + expr.posn.start + ":  Type error - >= <= > < are for int type only");
				return new BaseType(TypeKind.ERROR, new SourcePosition());
			}
		} else if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/")) {
			if (leftType.typeKind == TypeKind.INT && rightType.typeKind == TypeKind.INT) {
				return new BaseType(TypeKind.INT, new SourcePosition());
			} else {
				errorReporter
						.reportError("*** line " + expr.posn.start + ":  Type error - + - * / are for int type only");
				return new BaseType(TypeKind.ERROR, new SourcePosition());
			}
		} else {
			errorReporter.reportError("*** line " + expr.posn.start + ":  Type error - operator doesn't exist");
			return new BaseType(TypeKind.ERROR, new SourcePosition());
		}
	}

	@Override
	public TypeDenoter visitRefExpr(RefExpr expr, TypeDenoter arg) {
		if (expr.ref.getDecl() instanceof MethodDecl) {
			errorReporter.reportError(
					"*** line " + expr.posn.start + ":  Type error - cannot use method name as an expression");
		}
		return expr.ref.visit(this, arg);
	}

	@Override
	public TypeDenoter visitCallExpr(CallExpr expr, TypeDenoter arg) {
		TypeDenoter type = null;
		if (expr.functionRef.getDecl() instanceof MethodDecl) {
			MethodDecl methodDecl = (MethodDecl) expr.functionRef.getDecl();
			if (expr.argList.size() != methodDecl.parameterDeclList.size()) {
				errorReporter.reportError("*** line " + expr.posn.start + ":  Type error - method "
						+ methodDecl.id.spelling + " has the wrong number of arguments");
			} else {
				for (int i = 0; i < expr.argList.size(); i++) {
					TypeDenoter actualType = expr.argList.get(i).visit(this, arg);
					TypeDenoter declaredType = methodDecl.parameterDeclList.get(i).type;
					checkUnSupported(actualType);
					checkError(actualType);
					checkUnSupported(declaredType);
					checkError(declaredType);
					if (!declaredType.equals(actualType)) {
						errorReporter.reportError("*** line " + expr.posn.start + ":  Type error - method "
								+ methodDecl.id.spelling + ": #" + (i + 1) + " parameter argument is wrong type");
					}
				}
			}
			type = methodDecl.type;
		} else {
			errorReporter.reportError("*** line " + expr.posn.start + ":  Type error - cannot call non-method");
		}

		return type;
	}

	@Override
	public TypeDenoter visitLiteralExpr(LiteralExpr expr, TypeDenoter arg) {
		return expr.lit.visit(this, arg);
	}

	@Override
	public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, TypeDenoter arg) {
		expr.type = expr.classtype.visit(this, arg);
		return expr.type;
	}

	@Override
	public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, TypeDenoter arg) {
		expr.type = new ArrayType(expr.eltType, expr.posn);
		return expr.type;
	}

	// References //

	@Override
	public TypeDenoter visitThisRef(ThisRef ref, TypeDenoter arg) {
		return ref.getType();
	}

	@Override
	public TypeDenoter visitIdRef(IdRef ref, TypeDenoter arg) {
		if (ref.getType() == null) {
			errorReporter.reportError("*** line " + ref.posn.start + ":  Type error - IdRef type cannot be null");
		}
		return ref.getType();
	}

	@Override
	public TypeDenoter visitQRef(QualRef ref, TypeDenoter arg) {
		if (ref.getType() == null) {
			errorReporter.reportError("*** line " + ref.posn.start + ":  Type error - QualRef type cannot be null");
		}
		return ref.getType();
	}

	@Override
	public TypeDenoter visitIxRef(IxRef ref, TypeDenoter arg) {
		TypeDenoter indexType = ref.indexExpr.visit(this, arg);
		if (indexType.typeKind != TypeKind.INT) {
			errorReporter.reportError("*** line " + ref.posn.start + ":  Type error - IxExpr is not int type");
		}
		if (ref.getType() == null) {
			errorReporter.reportError("*** line " + ref.posn.start + ":  Type error - IxRef cannot be null");
		}
		return ref.getType();
	}

	// Terminals //

	@Override
	public TypeDenoter visitIdentifier(Identifier id, TypeDenoter arg) {
		return id.decl.type;
	}

	@Override
	public TypeDenoter visitOperator(Operator op, TypeDenoter arg) {
		return null;
	}

	@Override
	public TypeDenoter visitIntLiteral(IntLiteral intLiteral, TypeDenoter arg) {
		return new BaseType(TypeKind.INT, intLiteral.posn);
	}

	@Override
	public TypeDenoter visitNullLiteral(NullLiteral nullLiteral, TypeDenoter arg) {
		return new BaseType(TypeKind.NULL, nullLiteral.posn);
	}

	@Override
	public TypeDenoter visitBooleanLiteral(BooleanLiteral booleanLiteral, TypeDenoter arg) {
		return new BaseType(TypeKind.BOOLEAN, booleanLiteral.posn);
	}

	private void checkUnSupported(TypeDenoter typeDenoter) {
		if (typeDenoter.typeKind == TypeKind.UNSUPPORTED) {
			errorReporter
					.reportError("*** line " + typeDenoter.posn.start + ": Type error - variable type UNSUPPORTED");
		}
	}

	private void checkError(TypeDenoter typeDenoter) {
		if (typeDenoter.typeKind == TypeKind.ERROR) {
			errorReporter.reportError("*** line " + typeDenoter.posn.start + ": Type error - variable type ERROR");
		}
	}
}