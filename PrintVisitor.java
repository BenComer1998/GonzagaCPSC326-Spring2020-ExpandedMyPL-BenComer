
/**
 * Author: Ben Comer
 * Assign: 4
 * File: PrintVisitor.java
 *
 * Print Visitor skeleton code for MyPL AST.
 */


import java.io.PrintStream;


public class PrintVisitor implements Visitor {
  private PrintStream out;      // the output stream for printing
  private int indent = 0;       // the current indent level (num spaces)

  // indent helper functions

  // to get a string with the current indentation level (in spaces)
  private String getIndent() {
    return " ".repeat(indent);
  }

  // to increment the indent level
  private void incIndent() {
    indent += 2;
  }

  // to decrement the indent level
  private void decIndent() {
    indent -= 2;
  }

  // visitor functions

  public PrintVisitor(PrintStream printStream) {
    this.out = printStream;
  }

  public void visit(StmtList node) throws MyPLException {
    // iterate through each statement list node and delegate
    for (Stmt s : node.stmts) {
      out.print(getIndent());
      s.accept(this);
      out.println();
    }
  }

  public void visit(VarDeclStmt node) throws MyPLException {
    out.print("var ");
    if (node.varType != null)
      out.print(node.varType.lexeme() + " ");
    out.print(node.varId.lexeme() + " := ");
    node.varExpr.accept(this);
  }

  // TODO: Complete the remaining print visitor functions. Note that
  // your code will not compile until you stub out all of the visitor
  // functions from Visitor
  
  public void visit(ArrayDeclStmt node) throws MyPLException {
	out.print("array ");
	out.print(node.arrayType.lexeme());
	out.print(" ");
	out.print(node.arrayId.lexeme());
	if (node.elements.size() > 0) {
		out.print(" := ");
		for (int i = 0; i < node.elements.size() - 1; ++i) {
			out.print(node.elements.get(i).lexeme());
			out.print(", ");
		}
		out.print(node.elements.get(node.elements.size() - 1));
	}
  }
  
  public void visit(AssignStmt node) throws MyPLException {
	out.print("set ");
	node.lhs.accept(this);
	out.print(" := ");
	node.rhs.accept(this);
  }
  
  public void visit(ReturnStmt node) throws MyPLException {
	out.print("return");
	if (node.returnExpr != null) {
		out.print(" ");
		node.returnExpr.accept(this);
	}
  }
  
  public void visit(IfStmt node) throws MyPLException {
	out.print("if ");
	node.ifPart.boolExpr.accept(this);
	out.print(" then\n");
	incIndent();
	visit(node.ifPart.stmtList);
	decIndent(); out.print(getIndent());
	out.print("end");
	
	for (int i = 0; i < node.elsifs.size(); ++i) {
		out.print("\n" + getIndent());
		out.print("elif ");
		node.elsifs.get(i).boolExpr.accept(this);
		out.print(" then\n");
		incIndent();
		node.elsifs.get(i).stmtList.accept(this);
		decIndent(); out.print(getIndent());
		out.print("end");
	}
	
	if (node.hasElse) {
		out.print("\n" + getIndent());
		out.print("else\n");
		incIndent();
		node.elseStmtList.accept(this);
		decIndent(); out.print(getIndent());
		out.print("end");
	}
  }
  
  public void visit(WhileStmt node) throws MyPLException {
	out.print("while ");
	node.boolExpr.accept(this);
	out.print(" do\n");
	incIndent();
	node.stmtList.accept(this);
	decIndent(); out.print(getIndent());
	out.print("end");
  }
  
  public void visit(ForStmt node) throws MyPLException {
	out.print("for ");
	out.print(node.var.lexeme());
	out.print(" := ");
	node.startExpr.accept(this);
	out.print(" to ");
	node.endExpr.accept(this);
	out.print(" do\n");
	incIndent();
	node.stmtList.accept(this);
	decIndent(); out.print(getIndent());
	out.print("end");
  }
  
  public void visit(TypeDeclStmt node) throws MyPLException {
	out.print("\ntype ");
	out.print(node.typeId.lexeme());
	out.print("\n");
	incIndent();
	for (int i = 0; i < node.fields.size(); ++i) {
		out.print(getIndent());
		node.fields.get(i).accept(this);
		out.print("\n");
	}
	decIndent(); out.print(getIndent());
	out.print("end\n");
  }
  
  public void visit(FunDeclStmt node) throws MyPLException {
	out.print("\nfun ");
	out.print(node.returnType.lexeme());
	out.print(" " + node.funName.lexeme() + "(");
	
	if (node.params.size() > 0) {
		out.print(node.params.get(0).paramType.lexeme() + " ");
		out.print(node.params.get(0).paramName.lexeme());
		for (int i = 1; i < node.params.size(); ++i) {
			out.print(", ");
			out.print(node.params.get(i).paramType.lexeme() + " ");
			out.print(node.params.get(i).paramName.lexeme());
		}
		out.print(")\n");
	}
	
	incIndent();
	node.stmtList.accept(this);
	decIndent(); out.print(getIndent());
	out.print("end\n");
  }
  
  public void visit(Expr node) throws MyPLException {
	if (node.negated)
		out.print("not ");
	
	if (node.operator != null)
		out.print("(");
	
	node.first.accept(this);
	if (node.operator != null) {
		out.print(" " + node.operator.lexeme() + " ");
		node.rest.accept(this);
		out.print(")");
	}
  }
  
  public void visit(LValue node) throws MyPLException {
	out.print(node.path.get(0).lexeme());
	for (int i = 1; i < node.path.size(); ++i) {
		out.print("." + node.path.get(i).lexeme());
	}
  }
  
  public void visit(SimpleTerm node) throws MyPLException {
	node.rvalue.accept(this);
  }
  
  public void visit(ComplexTerm node) throws MyPLException {
	node.expr.accept(this);
  }
  
  public void visit(SimpleRValue node) throws MyPLException {
	if (node.val.type() == TokenType.STRING_VAL)
		out.print("\"");
	else if (node.val.type() == TokenType.CHAR_VAL)
		out.print("'");
	
	out.print(node.val.lexeme());
	
	if (node.val.type() == TokenType.STRING_VAL)
		out.print("\"");
	else if (node.val.type() == TokenType.CHAR_VAL)
		out.print("'");
  }
  
  public void visit(NewRValue node) throws MyPLException {
	out.print("new " + node.typeId.lexeme());
  }
  
  public void visit(CallRValue node) throws MyPLException {
	out.print(node.funName.lexeme());
	out.print("(");
	if (node.argList.size() > 0) {
		node.argList.get(0).accept(this);
		
		for (int i = 1; i < node.argList.size(); ++i) {
			out.print(", ");
			node.argList.get(i).accept(this);
		}
	}
	out.print(")");
  }
  
  public void visit(IDRValue node) throws MyPLException {
	out.print(node.path.get(0).lexeme());
	for (int i = 1; i < node.path.size(); ++i) {
		out.print(".");
		out.print(node.path.get(i).lexeme());
	}
  }
  
  public void visit(NegatedRValue node) throws MyPLException {
	out.print("neg ");
	node.expr.accept(this);
  }

}    

