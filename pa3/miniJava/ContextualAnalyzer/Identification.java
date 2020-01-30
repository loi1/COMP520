package miniJava.ContextualAnalyzer;

import miniJava.*;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import java.util.HashMap;
import java.util.HashSet;

public class Identification implements Visitor<Integer, Object> {

	public ErrorReporter errorReporter;
	public IdentificationTable idt;
	private AST sourceAST;
	public boolean userDefinedString;
	public boolean containsMain;
	private boolean inStaticMethod;
	private ClassDecl currentClass;
	private String currentDeclaredVariable;
	
	public HashMap<ClassDecl, Integer> classList;
    private HashSet<String> localVariables;

	//private Package stdAST;
	//public StdEnvGenerator stdEnv;

	public Identification(AST sourceAST, ErrorReporter reporter) {
		this.errorReporter = reporter;
		this.idt = new IdentificationTable();
		this.sourceAST = sourceAST;
		//stdEnv = new StdEnvGenerator();
		//stdEnv.genEnv(idt);
	}

	public AST check() {
		containsMain = false;
		sourceAST.visit(this, 0);
		
		if (!containsMain) {
			errorReporter.reportError("*** line N/A: Identification Error: main method does not exist");
			System.exit(4);
		} else System.out.println("Check completed, success.");
		return sourceAST;
	}
	
	// Package //

	@Override
	public Reference visitPackage(Package p, Integer phase) {
		ClassDeclList cdl = p.classDeclList;
		idt.openScope(); // open 1st level
		
		for (ClassDecl cd: cdl){ 
			if (cd.id.spelling.startsWith("_")) {
                errorReporter.reportError("*** line " + cd.posn.start + ": Identification Error - illegal class name, cannot start with _");
                System.exit(4);
                return null;
            } else if (cd.id.spelling.equals("String")) {
                this.userDefinedString = true; 
            }
            if (idt.enterDecl(cd) == null) {
                errorReporter.reportError("*** line " + cd.posn.start + ": Identification Error - duplicate declaration of " + cd.id.spelling);
                System.exit(4);
            }
		}	

		for (ClassDecl cd: cdl){ // each member of each class gets OFFICIALLY assigned a class
			for(FieldDecl fd : cd.fieldDeclList){
				fd.classDecl = cd; // assign each field a class
			}
			for(MethodDecl md : cd.methodDeclList){
				md.classDecl = cd; // assign each method a class
			}
		}

		classList = new HashMap<ClassDecl, Integer>();
		for (ClassDecl cd: cdl){ 
			idt.openScope(); // open new level for each class
			for (FieldDecl fd: cd.fieldDeclList) {
				fd.visit(this, 0);
                if (idt.enterDecl(fd) == null) {
                    errorReporter.reportError("*** line " + fd.posn.start + ": Identification Error - duplicate declaration of " + fd.id.spelling);
                    System.exit(4);
                }
			}
			for (MethodDecl md: cd.methodDeclList) {
				if (idt.enterDecl(md) == null) {
                    errorReporter.reportError("*** line " + md.posn.start + ": Identification Error - duplicate declaration of " + md.id.spelling);
                    System.exit(4);
				}
                if (md.isMain()) {
                	if (containsMain) {
                		errorReporter.reportError("*** line " + md.posn.start + ": Identification Error - duplicate declaration of main method");
                		System.exit(4);
                	} else {
                		containsMain = true;
                	}
                }
			}
			classList.put(cd, idt.getHighestLevel()); // put class in highest level
		}
		
		for (ClassDecl cd : cdl) {
            currentClass = cd;
            idt.swapIdt(classList.get(cd), idt.getHighestLevel());
            cd.visit(this, 0);
        }
		
		idt.closeScope(); // close 1st level
		return null;
	}
	
	// Declarations //

	@Override
	public Reference visitClassDecl(ClassDecl cd, Integer phase) {
		//currentClass = cd;
		for (MethodDecl m : cd.methodDeclList) {
			//idt.openScope();
			m.visit(this, 0);
			//idt.closeScope(); 
		}
		return null;
	}

	@Override
	public Reference visitFieldDecl(FieldDecl fd, Integer phase) { // level 2
		fd.type.visit(this, 0);
		return null;
	}

	@Override
	public Reference visitMethodDecl(MethodDecl md, Integer phase) {		
		md.type.visit(this, 0);
		inStaticMethod = md.isStatic;
		localVariables = new HashSet<>();
		
		idt.openScope();
		for (ParameterDecl pd: md.parameterDeclList) {
			pd.visit(this, 0);
		}
		idt.openScope();
		for (Statement s: md.statementList) {
			s.visit(this, 0);
		}
		
		idt.closeScope(); 
		return null;
	}

	@Override
	public Reference visitParameterDecl(ParameterDecl pd, Integer phase) {
		//idt.enterDecl(pd);
		pd.type.visit(this, 0);
		pd.setIdBinding();
		if(localVariables.contains(pd.id.spelling)) {
			errorReporter.reportError("*** line " + pd.posn.start + ": Identification Error - duplicate declaration of local variable " + pd.id.spelling);
			System.exit(4);
		} else {
			localVariables.add(pd.id.spelling);
		}
		if (idt.enterDecl(pd) == null) {
			errorReporter.reportError("*** line " + pd.posn.start
					+ ": Identification Error - duplicate declaration of " + pd.id.spelling);
			System.exit(4);
		}
		return null;
	}

	@Override
	public Reference visitVarDecl(VarDecl decl, Integer phase) {
		decl.type.visit(this, 0);
		decl.setIdBinding();
		if (localVariables.contains(decl.id.spelling)) {
        	errorReporter.reportError("*** line " + decl.posn.start + ": Identification Error - duplicate declaration of local variable " + decl.id.spelling);
        	System.exit(4);
		} else {
        	localVariables.add(decl.id.spelling);
        }
		
		if (idt.enterDecl(decl) == null) {
			errorReporter.reportError("*** line " + decl.posn.start
					+ ": Identification Error - duplicate declaration of " + decl.id.spelling);
			System.exit(4);
		}
		return null;
	}
	
	// TypeDenoters //

	@Override
	public Reference visitBaseType(BaseType type, Integer phase) {
		return null;
	}

	@Override
	public Reference visitClassType(ClassType type, Integer phase) {
		ClassDecl cd = (ClassDecl) idt.retrieveClass(type.className.spelling);
		if (cd == null) {
			errorReporter.reportError("*** line " + type.posn.start + ": Identification Error - class name expected ");
			System.exit(4);
		} else {
			type.className.decl = cd;
		}
		
		return null;
	}

	@Override
	public Reference visitArrayType(ArrayType type, Integer phase) {
		type.eltType.visit(this, 0);
		return null;
	}
	
	// Statements //

	@Override
	public Reference visitBlockStmt(BlockStmt stmt, Integer phase) {
		idt.openScope();
		for (Statement s: stmt.sl) {
			s.visit(this, 0);
		}
		for(String var: idt.getCurrentIdt().keySet()) {
			if(localVariables.contains(var)) {
				localVariables.remove(var);
			}
		}
		idt.closeScope();
		return null;
	}

	@Override
	public Reference visitVardeclStmt(VarDeclStmt stmt, Integer phase) {
		currentDeclaredVariable = stmt.varDecl.id.spelling;
		stmt.initExp.visit(this, 0);
		currentDeclaredVariable = "";
		stmt.varDecl.visit(this, 0);	
		return null;
	}

	@Override
	public Reference visitAssignStmt(AssignStmt stmt, Integer phase) {
		if (stmt.ref instanceof ThisRef) {
			errorReporter.reportError("*** line " + stmt.posn.start
					+ ": Identification Error - \'this\' cannot be on left side of assign statement");
			System.exit(4);
		} else {
			stmt.ref.visit(this, 0);
		}
		stmt.val.visit(this, 0);
		return null;
	}

	@Override
	public Reference visitCallStmt(CallStmt stmt, Integer phase) {
		stmt.methodRef.visit(this, 0);

		MethodDecl methodDecl = (MethodDecl) stmt.methodRef.getDecl();

		if (methodDecl.parameterDeclList.size() != stmt.argList.size()) {
			errorReporter.reportError("*** line " + stmt.posn.start + 
					": Identification Error - # of arguments does not equal # of parameters of method "+methodDecl.id.spelling);
			System.exit(4);
			return null;
		}

		for (Expression e : stmt.argList) {
			e.visit(this, 0);
		}
		return null;
	}

	@Override
	public Reference visitReturnStmt(ReturnStmt stmt, Integer phase) {
		if (stmt.returnExpr != null)
			stmt.returnExpr.visit(this, 0);
		return null;
	}

	@Override
	public Reference visitIfStmt(IfStmt stmt, Integer phase) {
		stmt.cond.visit(this, 0);

		if (stmt.thenStmt instanceof VarDeclStmt) {
			errorReporter.reportError("*** line " + stmt.thenStmt.posn.start
					+ ": Identification Error - cannot declare variable in then statement");
			System.exit(4);
		} else {
			stmt.thenStmt.visit(this, 0);
		}

		if (stmt.elseStmt != null) {
			if (stmt.elseStmt instanceof VarDeclStmt) {
				errorReporter.reportError("*** line " + stmt.elseStmt.posn.start
						+ ": Identification Error - cannot declare variable in else statement");
				System.exit(4);
			} else {
				stmt.elseStmt.visit(this, 0);
			}
		}
		return null;
	}

	@Override
	public Reference visitWhileStmt(WhileStmt stmt, Integer phase) {
		stmt.cond.visit(this, 0); // checks for boolean in condition
		if (stmt.body != null) { // checks existing body
			if (stmt.body instanceof VarDeclStmt) {
				errorReporter.reportError("*** line " + stmt.body.posn.start
						+ ": Identification Error - cannot declare variable in body of while loop");
				System.exit(4);
			} else stmt.body.visit(this, 0);
		}
		return null;
	}
	
	// Expressions //

	@Override
	public Reference visitUnaryExpr(UnaryExpr expr, Integer phase) {
		expr.expr.visit(this, 0);
		return null;
	}

	@Override
	public Reference visitBinaryExpr(BinaryExpr expr, Integer phase) {
		expr.left.visit(this, 0);
		expr.right.visit(this, 0);
		return null;
	}

	@Override
	public Reference visitRefExpr(RefExpr expr, Integer phase) {
		expr.ref.visit(this, 0);
		if (expr.ref instanceof IdRef
				&& (expr.ref.getDecl() instanceof ClassDecl || expr.ref.getDecl() instanceof MethodDecl)) {
			errorReporter.reportError("*** line " + expr.posn.start
					+ ": Identification Error - cannot reference class or method name in a RefExpr");
			System.exit(4);
		}
		return null;
	}

	@Override
	public Reference visitCallExpr(CallExpr expr, Integer phase) {
		expr.functionRef.visit(this, 2);

		MethodDecl methodDecl = (MethodDecl) expr.functionRef.getDecl();
		if (methodDecl.parameterDeclList.size() != expr.argList.size()) {
			errorReporter.reportError("*** line " + expr.posn.start + ": Identification Error - # of arguments does not "
					+ "equal # of parameters in method "+ methodDecl.id.spelling);
			System.exit(4);
			return null;
		}
		
		for (Expression e : expr.argList) {
			e.visit(this, 0);
		}
		return null;
	}

	@Override
	public Reference visitLiteralExpr(LiteralExpr expr, Integer phase) {
		expr.lit.visit(this, 0);
		return null;
	}

	@Override
	public Reference visitNewObjectExpr(NewObjectExpr expr, Integer phase) {
		expr.classtype.className.visit(this, 0);
		return null;
	}
	
	@Override
	public Reference visitNewArrayExpr(NewArrayExpr expr, Integer phase) {
		expr.eltType.visit(this, 0);
		expr.sizeExpr.visit(this, 0);
		return null;
	}
	
	// References //

	@Override
	public Reference visitThisRef(ThisRef ref, Integer phase) {
		if (inStaticMethod) {
			errorReporter.reportError("*** line"+ref.posn.start+": Identification Error - \'this\' cannot be used as static context");
			System.exit(4);
		}
		ref.declaration = currentClass;
        ref.isStatic = false;
		return ref;
	}
	
	@Override
	public Reference visitIdRef(IdRef ref, Integer phase) {
		ref.isStatic = ref.id.isStatic;
		ref.id.visit(this, phase);
		if (ref.id.spelling.equals(currentDeclaredVariable)) {
			errorReporter.reportError("*** line " + ref.id.posn.start + ": Identification Error - " + currentDeclaredVariable
					+ " not initialized yet");
			System.exit(4);
		} else if (ref.getDecl() instanceof MemberDecl) {
			MemberDecl memberDecl = (MemberDecl) ref.getDecl();
			ClassDecl cd = memberDecl.classDecl;
			if (cd != currentClass) {
				errorReporter.reportError("*** line " + ref.posn.start + ": Identification Error - cannot find reference: "
								+ ref.id.spelling + " in class: " + cd.id.spelling);
				System.exit(4);
			}
		}
		return null;
	}

	@Override
	public Reference visitQRef(QualRef ref, Integer phase) {
		ref.ref.visit(this, 0);
        Declaration qDecl = ref.ref.getDecl();
        boolean oldStatic = inStaticMethod;
        inStaticMethod = ref.ref.isStatic;
        if (qDecl == null) return null;
        if (qDecl instanceof MethodDecl) {
        	errorReporter.reportError("*** line " + ref.posn.start + ": Identification Error - cannot call method in the middle of a QualRef");
        }
        if (qDecl instanceof ClassDecl) {
            ClassDecl classDecl = (ClassDecl) qDecl;
            ref.id.visit(this, 3);
            ref.isStatic = ref.id.isStatic;
            Declaration declaration = ref.getDecl();
            boolean sameClass = classDecl == currentClass;

            if (declaration instanceof MemberDecl) {
                if (!sameClass) {
                    // Check visible & static
                    String checkName = declaration.id.spelling;
                    if (!classDecl.existsMember(checkName, inStaticMethod, true)) {
                        errorReporter.reportError("*** line " + ref.posn.start + ": Identification Error - cannot find static and public member: " + declaration.id.spelling);
                        System.exit(4);
                    }
                } else {
                    if (!classDecl.existsMember(declaration.id.spelling, inStaticMethod, false)) {
                        errorReporter.reportError("*** line " + ref.posn.start + ": Identification Error - cannot find member: " + declaration.id.spelling);
                        System.exit(4);
                    }
                }
            } else {
                errorReporter.reportError("*** line " + ref.posn.start + ": Identification Error - expect a member declaration: " + declaration.id.spelling);
                System.exit(4);
            }

        } else {
            ref.id.visit(this, 3);
            ref.isStatic = ref.id.isStatic;
            Declaration declaration = ref.getDecl();
            if (qDecl.type.typeKind == TypeKind.CLASS) {
                ClassDecl classDecl = (ClassDecl) (qDecl.type.getDecl());
                if (qDecl instanceof ClassDecl) classDecl = (ClassDecl)qDecl;
                boolean sameClass = classDecl == currentClass;

                if (declaration instanceof MemberDecl) {
                    if (!sameClass) {
                        // Check visible
                        String checkName = declaration.id.spelling;
                        if (!classDecl.existsMember(checkName, false, true)) {
                            errorReporter.reportError("*** line " + ref.posn.start + ": Identification Error - cannot find public member: " + declaration.id.spelling);
                            System.exit(4);
                        }
                    } else {
                        if (!classDecl.existsMember(declaration.id.spelling, false, false)) {
                            errorReporter.reportError("*** line " + ref.posn.start + ": Identification Error - cannot member: " + declaration.id.spelling);
                            System.exit(4);
                        }
                    }
                } else {
                    errorReporter.reportError("*** line " + ref.posn.start + ": Identification Error - expect a member declaration: " + declaration.id.spelling);
                    System.exit(4);
                }
            } else if (qDecl.type.typeKind == TypeKind.ARRAY) {
                if (ref.ref instanceof IdRef || ref.ref instanceof QualRef) {
                    ClassDecl classDecl = (ClassDecl) (qDecl.type.getDecl());
                    boolean sameClass = classDecl == currentClass;

                    if (declaration instanceof MemberDecl) {
                        if (!sameClass) {
                            // Check visible
                            String checkName = declaration.id.spelling;
                            if (!classDecl.existsMember(checkName, false, true)) {
                                errorReporter.reportError("*** line " + ref.posn.start + ": Identification Error - cannot find public member: " + declaration.id.spelling);
                                System.exit(4);
                            }
                        } else {
                            if (!classDecl.existsMember(declaration.id.spelling, false, false)) {
                                errorReporter.reportError("*** line " + ref.posn.start + ": Identification Error - cannot find member: " + declaration.id.spelling);
                                System.exit(4);
                            }
                        }
                    } else {
                        errorReporter.reportError("*** line " + ref.posn.start + ": Identification Error - expect a member declaration: " + declaration.id.spelling);
                        System.exit(4);
                    }
                } else {
                    if (!ref.id.spelling.equals("length")) {
                    	errorReporter.reportError("*** line " + ref.posn.start + ": Identification Error - referencing a member of an ARRAY");
                    	System.exit(4);
                    }
                }
            } else {
                errorReporter.reportError("*** line " + ref.posn.start + ": Identification Error - referncing member of primitive types");
                System.exit(4);
            }
        }
        inStaticMethod = oldStatic;

        return null;
	}
	
	@Override
	public Reference visitIxRef(IxRef ref, Integer phase) {
		ref.id.visit(this, phase);
		ref.isStatic = ref.id.isStatic;
		if (ref.getDecl().type.typeKind != TypeKind.ARRAY) {
			errorReporter.reportError("*** line " + ref.posn.start + ": Identification Error - " + ref.id.spelling
					+ " is not array type");
			System.exit(4);
		} else {
			ref.indexExpr.visit(this, 0);
		}
		return null;
	}
	
	// Terminals //

	@Override
	public Reference visitIdentifier(Identifier id, Integer phase) {
		id.isStatic = false;
		
		if (id.spelling.equals("length")) {
			id.decl = new FieldDecl(false, false, new BaseType(TypeKind.INT, id.posn), id, id.posn);
			return null;
		}

		if (phase == 2) {
            Declaration declaration = idt.retrieveMethod(id.spelling);
            if (declaration == null) {
                errorReporter.reportError("*** line " + id.posn.start + ": Identification Error - method name expected");
                System.exit(4);
            } else {
                id.decl = declaration;
            }
            return null;
        } else if (phase == 3) {
            Declaration declaration = idt.retrieveMember(id.spelling);
            if (declaration == null) {
                errorReporter.reportError("*** line " + id.posn.start + ": Identification Error - method name expected");
                System.exit(4);
            } else {
                id.decl = declaration;
            }
            return null;
        }
        
		Declaration decl = idt.retrieve(id.spelling);
		if (decl == null) {
			errorReporter.reportError("*** line "+id.posn.start+": Identification Error - cannot find variable: " + id.spelling);
			System.exit(4);
		} else {
			if (decl instanceof LocalDecl) {
				decl.type.visit(this, 0);
				id.decl = decl;
			} else if (decl instanceof MemberDecl) {
				MemberDecl memDecl = (MemberDecl) decl;
				if (inStaticMethod && !memDecl.isStatic) {
					errorReporter.reportError("*** line "+id.posn.start+": Identification Error - referencing non-static member declaration, " + id.spelling + ", in a static method");
					System.exit(4);
				} else {
					boolean sameClass = (currentClass == memDecl.classDecl);
	                if (!sameClass && !inStaticMethod && memDecl.isStatic) {
	                    errorReporter.reportError("*** line " + id.posn.start + ": Identification Error - referencing static variables in non-static methods");
	                    System.exit(4);
	                }
				}

                decl.type.visit(this, 0);
				id.decl = decl;
			} else if (decl instanceof ClassDecl) {// Class name, no type}
				id.decl = decl;
                id.isStatic = true;
			} else {
				errorReporter.reportError("*** line "+id.posn.start+": Identification Error - id should not be here");
				System.exit(4);
			}
		}
		return null;
	}

	// Literals and Operator: do nothing //
	@Override
	public Reference visitOperator(Operator op, Integer phase) {
		return null;
	}

	@Override
	public Reference visitIntLiteral(IntLiteral num, Integer phase) {
		return null;
	}

	@Override
	public Reference visitNullLiteral(NullLiteral nullLiteral, Integer phase) {
		return null;
	}

	@Override
	public Reference visitBooleanLiteral(BooleanLiteral bool, Integer phase) {
		return null;
	}
}
