/**
 * Author: Ben Comer
 * Homework: #3
 * File: Parser.java
 * 
 * Recursive descent parser implementation for MyPL. The parser
 * requires a lexer. Once a parser is created, the parse() method
 * ensures the given program is syntactically correct. 
 */

import java.util.*;


public class Parser {
    
  private Lexer lexer; 
  private Token currToken = null;
  private boolean debug_flag = false;  // set to false to remove debug comments
  
  /** 
   * Create a new parser over the given lexer.
   */
  public Parser(Lexer lexer) {
    this.lexer = lexer;
  }

  /**
   * Ensures program is syntactically correct. On error, throws a
   * MyPLException.
   */
  public StmtList parse() throws MyPLException
  {
	StmtList stmtListNode = new StmtList();
    advance();
    stmts(stmtListNode);
    eat(TokenType.EOS, "expecting end of file");
	return stmtListNode;
  }


  /* Helper Functions */

  // sets current token to next token in stream
  private void advance() throws MyPLException {
    currToken = lexer.nextToken();
  }

  // checks that current token matches given type and advances,
  // otherwise creates an error with the given error message
  private void eat(TokenType t, String errmsg) throws MyPLException {
    if (currToken.type() == t)
      advance();
    else
      error(errmsg);
  }

  // generates an error message from the given message and throws a
  // corresponding MyPLException
  private void error(String errmsg) throws MyPLException {
    String s = errmsg + " found '" + currToken.lexeme() + "'";
    int row = currToken.row();
    int col = currToken.column();
    throw new MyPLException("Parser", errmsg, row, col);
  }

  // function to print a debug string if the debug_flag is set for
  // helping to diagnose/test the parser
  private void debug(String msg) {
    if (debug_flag)
      System.out.println(msg);
  }

  
  /* Recursive Descent Functions */
  /* You're on hw4! */
  // Note: {e} stands for empty, or epsilon
  
  // <stmts> ::= <stmt> <stmts> | epsilon
  private void stmts(StmtList stmtListNode) throws MyPLException {
    debug("<stmts>");
	if (currToken.type() == TokenType.TYPE)
		stmtListNode.stmts.add(tdecl());
	else if (currToken.type() == TokenType.FUN)
		stmtListNode.stmts.add(fdecl());
	else if (currToken.type() == TokenType.VAR)
		stmtListNode.stmts.add(vdecl());
	else if (currToken.type() == TokenType.SET)
		stmtListNode.stmts.add(assign());
	else if (currToken.type() == TokenType.IF)
		stmtListNode.stmts.add(cond());
	else if (currToken.type() == TokenType.WHILE)
		stmtListNode.stmts.add(whilef());
	else if (currToken.type() == TokenType.FOR)
		stmtListNode.stmts.add(forf());
	else if (currToken.type() == TokenType.RETURN)
		stmtListNode.stmts.add(exit());
	else if (isExpr())
		stmtListNode.stmts.add(expr());
	else return; // Empty
	
	stmts(stmtListNode);
  }
  
  // <stmt> ::= <tdecl> | <fdecl> | <bstmt>
  private ASTNode stmt() throws MyPLException {
	debug("<stmt>");
	Stmt node = null;
	if (currToken.type() == TokenType.TYPE)
		node = tdecl();
	else if (currToken.type() == TokenType.FUN)
		node = fdecl();
	else
		node = bstmt();
	return node;
  }


  // <bstmts> ::= <bstmt> <bstmts> | epsilon
  private StmtList bstmts() throws MyPLException {
    debug("<bstmts>");
	StmtList stmtList = new StmtList();
	if (currToken.type() == TokenType.VAR ||
		currToken.type() == TokenType.SET ||
		currToken.type() == TokenType.IF ||
		currToken.type() == TokenType.WHILE ||
		currToken.type() == TokenType.FOR ||
		currToken.type() == TokenType.RETURN ||
		isExpr())
			return bstmts(stmtList);
	else return stmtList;
  }
  
  // Recursive version of above
  private StmtList bstmts(StmtList stmtList) throws MyPLException {
	debug("<bstmts_more>");
	stmtList.stmts.add(bstmt());
	if (currToken.type() == TokenType.VAR ||
		currToken.type() == TokenType.SET ||
		currToken.type() == TokenType.IF ||
		currToken.type() == TokenType.WHILE ||
		currToken.type() == TokenType.FOR ||
		currToken.type() == TokenType.RETURN ||
		currToken.type() == TokenType.ARRAY ||
		isExpr())
			return bstmts(stmtList);
	else return stmtList;
  }
  
  // <bstmt> ::= <vdecl> | <assign> | <cond> | <while>
  //             <for> | <expr> | <exit>
  private Stmt bstmt() throws MyPLException {
	debug("<bstmt>");
	Stmt node = null;
	if (currToken.type() == TokenType.VAR)
		node = vdecl();
	else if (currToken.type() == TokenType.SET)
		node = assign();
	else if (currToken.type() == TokenType.IF)
		node = cond();
	else if (currToken.type() == TokenType.WHILE)
		node = whilef();
	else if (currToken.type() == TokenType.FOR)
		node = forf();
	else if (currToken.type() == TokenType.RETURN)
		node = exit();
	else if (currToken.type() == TokenType.ARRAY)
		node = adecl();
	else if (isExpr())
		node = expr();
	else
		error("invalid statement");
	
	return node;
  }
  
  // <tdecl> ::= TYPE ID <vdecls> END
  private Stmt tdecl() throws MyPLException {
	debug("<tdecl>");
	TypeDeclStmt node = new TypeDeclStmt();
	advance();
	node.typeId = currToken;
	eat(TokenType.ID, "expecting identifier");
	vdecls(node.fields);
	eat(TokenType.END, "expecting 'end'");
	return node;
  }
  
  // <fdecl> ::= FUN (<dtype> | NIL) ID LPAREN <parems>
  // 			 RPAREN <bstmts> END
  private Stmt fdecl() throws MyPLException {
	debug("<fdecl>");
	FunDeclStmt node = new FunDeclStmt();
	advance();
	node.returnType = currToken;
	dtype();
	node.funName = currToken;
	eat(TokenType.ID, "expecting identifier");
	eat(TokenType.LPAREN, "expecting '('");
	params(node.params);
	eat(TokenType.RPAREN, "expecting ')'");
	node.stmtList = bstmts();
	eat(TokenType.END, "expecting 'end'");
	return node;
  }
  
  // <adecl> ::= ARRAY <dtype> ID (ASSIGN <adeclt> | {e})
  private Stmt adecl() throws MyPLException {
	debug("<adecl>");
	ArrayDeclStmt node = new ArrayDeclStmt();
	advance();
	node.arrayType = currToken;
	dtype(); // advances
	node.arrayId = currToken;
	eat(TokenType.ID, "expecting array identifier");
	if (currToken.type() == TokenType.ASSIGN) {
		advance();
		node.elements = adeclt();
	}
	return node;
  }
  
  // HELPER
  // <adecl> :: <dtype> (COMMA <dtype>)*
  private ArrayList<Token> adeclt() throws MyPLException {
	debug("<adeclt>");
	ArrayList<Token> elements = new ArrayList<>();
	elements.add(currToken);
	dtype();
	
	while (currToken.type() == TokenType.COMMA) {
		advance();
		elements.add(currToken);
		dtype();
	}
	
	return elements;
  }
  
  // <dtype> ::= INT_TYPE | DOUBLE_TYPE | BOOL_TYPE |
  //			 CHAR_TYPE | STRING_TYPE | ID
  // Honestly a helper at this point
  private void dtype() throws MyPLException {
	debug("<dtype>");
	if (currToken.type() == TokenType.INT_TYPE ||
		currToken.type() == TokenType.DOUBLE_TYPE ||
		currToken.type() == TokenType.BOOL_TYPE ||
		currToken.type() == TokenType.CHAR_TYPE ||
		currToken.type() == TokenType.STRING_TYPE ||
		currToken.type() == TokenType.ID ||
		currToken.type() == TokenType.NIL)
			advance();
	else
		error("expected type or identifier");
  }

  // <cond> ::= IF <expr> THEN <bstmts><condt> END
  private IfStmt cond() throws MyPLException {
	debug("<cond>");
	IfStmt node = new IfStmt();
	advance();
	node.ifPart.boolExpr = expr();
	eat(TokenType.THEN, "expecting 'then'");
	node.ifPart.stmtList = bstmts();
	condt(node);
	eat(TokenType.END, "expecting 'end'");
	return node;
  }
  
  // <condt> ::= ELIF <expr> THEN <bstmts><condt> |
  //			 ELSE <bstmts> | {e}
  private void condt(IfStmt node) throws MyPLException {
	debug("<condt>");
	if (currToken.type() == TokenType.ELIF) {
		BasicIf elseif = new BasicIf();
		advance();
		elseif.boolExpr = expr();
		eat(TokenType.THEN, "expected 'then'");
		elseif.stmtList = bstmts();
		node.elsifs.add(elseif);
		condt(node);
	}
	else if (currToken.type() == TokenType.ELSE) {
		node.hasElse = true;
		advance();
		node.elseStmtList = null; // Clears previous address
		node.elseStmtList = bstmts();
	}
  }
  
  // Simple things first...
  // <assign> ::= SET <lvalue> ASSIGN <expr>
  private Stmt assign() throws MyPLException {
	debug("<assign>");
	AssignStmt node = new AssignStmt();
	eat(TokenType.SET, "expecting 'set'");
	node.lhs = lvalue();
	eat(TokenType.ASSIGN, "expecting ':='");
	node.rhs = expr();
	return node;
  }
  
  // <expr> ::= (<rvalue> | NOT <expr> | LPAREN <expr> RPAREN)
  //			(<operator><expr> | {e})
  private Expr expr() throws MyPLException {
	debug("<expr>");
	Expr node = new Expr();
	if (currToken.type() == TokenType.NOT) {
		ComplexTerm term = new ComplexTerm();
		advance();
		term.expr = expr();
		node.negated = true;
		node.first = term;
	}
	else if (currToken.type() == TokenType.LPAREN) {
		ComplexTerm term = new ComplexTerm();
		advance();
		term.expr = expr();
		node.first = term;
		eat(TokenType.RPAREN, "expected ')'");
	}
	else {
		SimpleTerm term = new SimpleTerm();
		term.rvalue = rvalue();
		node.first = term;
	}	
	
	if (currToken.type() == TokenType.PLUS ||
		currToken.type() == TokenType.MINUS ||
		currToken.type() == TokenType.DIVIDE ||
		currToken.type() == TokenType.MULTIPLY ||
		currToken.type() == TokenType.MODULO ||
		currToken.type() == TokenType.AND ||
		currToken.type() == TokenType.OR ||
		currToken.type() == TokenType.EQUAL ||
		currToken.type() == TokenType.LESS_THAN ||
		currToken.type() == TokenType.GREATER_THAN ||
		currToken.type() == TokenType.LESS_THAN_EQUAL ||
		currToken.type() == TokenType.GREATER_THAN_EQUAL ||
		currToken.type() == TokenType.NOT_EQUAL) {
			node.operator = currToken;
			advance();
			node.rest = expr();
	}
	
	return node;
  }
  
  
  // <rvalue> ::= <pval> | NIL | NEW ID | <idrval> | NEG <expr>
  private RValue rvalue() throws MyPLException {
	debug("<rvalue>");	
	if (currToken.type() == TokenType.INT_VAL ||
		currToken.type() == TokenType.DOUBLE_VAL ||
		currToken.type() == TokenType.BOOL_VAL ||
		currToken.type() == TokenType.CHAR_VAL ||
		currToken.type() == TokenType.STRING_VAL ||
		currToken.type() == TokenType.NIL) {
			SimpleRValue node = new SimpleRValue();
			node.val = currToken;
			advance(); // pval();
			return node;
	}
	else if (currToken.type() == TokenType.NEW) {
		NewRValue node = new NewRValue();
		advance();
		node.typeId = currToken;
		eat(TokenType.ID, "expected identifier");
		return node;
	}
	else if (currToken.type() == TokenType.NEG) {
		NegatedRValue node = new NegatedRValue();
		advance();
		node.expr = expr();
		return node;
	}
	else if (currToken.type() == TokenType.ID) {
		Token id = currToken;
		eat(TokenType.ID, "expected right side");
		if (currToken.type() == TokenType.LPAREN) {
			CallRValue node = new CallRValue();
			node.funName = id;
			eat(TokenType.LPAREN, "expected '('");
			node.argList = exprlist();
			eat(TokenType.RPAREN, "expected ')'");
			return node;
		}
		else {
			IDRValue node = new IDRValue();
			node.path.add(id);
			if (currToken.type() == TokenType.DOT)
				dotid(node.path);
			return node;
		}
	}
	else {
		error("expected right value");
	}
	
	return new SimpleRValue(); // dummy
  }
  
  // <exit> ::= RETURN (<expr> | {e})
  private Stmt exit() throws MyPLException {
	debug("<exit>");
	ReturnStmt node = new ReturnStmt();
	node.returnToken = currToken;
	eat(TokenType.RETURN, "expecting 'return'");
	if (isExpr()) {
		node.returnExpr = expr();
	}
	return node;
  }
  
  // <whilef> ::= WHILE <expr> DO <bstmts> END
  private Stmt whilef() throws MyPLException {
	debug("<whilef>");
	WhileStmt node = new WhileStmt();
	advance();
	node.boolExpr = expr();
	eat(TokenType.DO, "expecting 'do'");
	node.stmtList = bstmts();
	eat(TokenType.END, "expecting 'end'");
	return node;
  }
  
  // <forf> ::= FOR ID ASSIGN <expr> TO <expr> DO <bstmts> END
  private Stmt forf() throws MyPLException {
	debug("<forf>");
	ForStmt node = new ForStmt();
	advance();
	node.var = currToken;
	eat(TokenType.ID, "expecting identifier");
	eat(TokenType.ASSIGN, "expecting ':='");
	node.startExpr = expr();
	eat(TokenType.TO, "expecting 'to'");
	node.endExpr = expr();
	eat(TokenType.DO, "expecting 'do'");
	node.stmtList = bstmts();
	eat(TokenType.END, "expecting 'end'");
	return node;
  }
  
  // <operator> ::= PLUS | MINUS | DIVIDE | MULTIPLY | MODULO |
  //				AND | OR | EQUAL | LESS_THAN | GREATER_THAN |
  //				LESS_THAN_EQUAL | GREATER_THAN_EQUAL | NOT_EQUAL
  // Honestly a helper at this point
  private void operator() throws MyPLException {
	debug("<operator>");
	if (currToken.type() == TokenType.PLUS ||
		currToken.type() == TokenType.MINUS ||
		currToken.type() == TokenType.DIVIDE ||
		currToken.type() == TokenType.MULTIPLY ||
		currToken.type() == TokenType.MODULO ||
		currToken.type() == TokenType.AND ||
		currToken.type() == TokenType.OR ||
		currToken.type() == TokenType.EQUAL ||
		currToken.type() == TokenType.LESS_THAN ||
		currToken.type() == TokenType.GREATER_THAN ||
		currToken.type() == TokenType.LESS_THAN_EQUAL ||
		currToken.type() == TokenType.GREATER_THAN_EQUAL ||
		currToken.type() == TokenType.NOT_EQUAL)
			advance();
	else
		error("expected operator");
  }
  
  // <pval> ::= INT_VAL | DOUBLE_VAL | BOOL_VAL | CHAR_VAL | STRING_VAL
  // consolidated
  
  // <idrval> ::= ID (DOT ID)* | ID LPAREN <exprlist> RPAREN
  // consolidated
  
  // <lvalue> ::= ID (DOT ID)*
  private LValue lvalue() throws MyPLException {
	debug("<lvalue>");
	LValue node = new LValue();
	node.path.add(currToken);
	eat(TokenType.ID, "expected identifier");
	if (currToken.type() == TokenType.DOT)
		dotid(node.path);
	
	return node;
  }
  
  // <vdecls> ::= <vdecl><vdecls> | {e}
  private void vdecls(ArrayList<VarDeclStmt> fields) throws MyPLException {
	debug("<vdecls>");
	if (currToken.type() == TokenType.VAR) {
		fields.add(vdecl());
		vdecls(fields);
	}
	// else empty
  }
  
  // <vdecl> ::= VAR (<dtype> | {e}) ID ASSIGN <expr>
  private VarDeclStmt vdecl() throws MyPLException {
	debug("<vdecl>");
	VarDeclStmt node = new VarDeclStmt();
	advance();
	if (currToken.type() == TokenType.INT_TYPE ||
		currToken.type() == TokenType.DOUBLE_TYPE ||
		currToken.type() == TokenType.BOOL_TYPE ||
		currToken.type() == TokenType.CHAR_TYPE ||
		currToken.type() == TokenType.STRING_TYPE) {
			node.varType = currToken;
			advance();
	}
	
	node.varId = currToken;
	eat(TokenType.ID, "expected identifier");
	if (currToken.type() == TokenType.ID) {
		node.varType = node.varId;
		node.varId = currToken;
		advance();
	}
	eat(TokenType.ASSIGN, "expected ':='");
	node.varExpr = expr();
	return node;
  }
  
  // <params> ::= <dtype> ID (COMMA <dtype> ID)* | {e}
  private void params(ArrayList<FunParam> params) throws MyPLException {
	debug("<params>");
	FunParam node = new FunParam();
	if (currToken.type() == TokenType.INT_TYPE ||
		currToken.type() == TokenType.DOUBLE_TYPE ||
		currToken.type() == TokenType.BOOL_TYPE ||
		currToken.type() == TokenType.CHAR_TYPE ||
		currToken.type() == TokenType.STRING_TYPE ||
		currToken.type() == TokenType.ID) {
			node.paramType = currToken;
			advance();
	}
	else
		return;
	
	node.paramName = currToken;
	eat(TokenType.ID, "expected identifier");
	params.add(node);
	if (currToken.type() == TokenType.COMMA)
		commatypeid(params);
  }
  
  // <exprlist> ::= <expr>(COMMA <expr>)* | {e}
  private ArrayList<Expr> exprlist() throws MyPLException {
	debug("<exprlist>");
	ArrayList<Expr> exprList = new ArrayList<Expr>();
	if (isExpr()) {
		exprList.add(expr());
		exlr(exprList);
	}
	return exprList;
  }
  
  // HELPER: <dotid> ::= DOT ID (<dotid> | {e})
  private void dotid(ArrayList<Token> path) throws MyPLException {
	debug("<dotid>");
	advance();
	path.add(currToken);
	eat(TokenType.ID, "expected identifier");
	if (currToken.type() == TokenType.DOT)
		dotid(path);
  }
  
  // HELPER: <commatypeid> ::= COMMA <dtype> ID (<commatypeid> | {e})
  private void commatypeid(ArrayList<FunParam> params) throws MyPLException {
	debug("<commatypeid>");
	FunParam node = new FunParam();
	advance();
	if (currToken.type() == TokenType.INT_TYPE ||
		currToken.type() == TokenType.DOUBLE_TYPE ||
		currToken.type() == TokenType.BOOL_TYPE ||
		currToken.type() == TokenType.CHAR_TYPE ||
		currToken.type() == TokenType.STRING_TYPE ||
		currToken.type() == TokenType.ID) {
			node.paramType = currToken;
			advance();
	}
	else
		error("expected type or identifier");
	
	node.paramName = currToken;
	eat(TokenType.ID, "expecting identifier");
	
	params.add(node);
	if (currToken.type() == TokenType.COMMA)
		commatypeid(params);
  }
  
  // HELPER: <exlr> ::= COMMA <expr> | {e}
  private void exlr(ArrayList<Expr> exprList) throws MyPLException {
	debug("exlr");
	if (currToken.type() == TokenType.COMMA) {
		advance();
		exprList.add(expr());
		exlr(exprList);
	}
	// else empty
  }
  
  // HELPER: Is a valid <expr>? Rather, is the beginning char valid?
  private boolean isExpr() throws MyPLException {
	debug("isExpr");
	if (currToken.type() == TokenType.INT_VAL ||
		currToken.type() == TokenType.DOUBLE_VAL ||
		currToken.type() == TokenType.BOOL_VAL ||
		currToken.type() == TokenType.CHAR_VAL ||
		currToken.type() == TokenType.STRING_VAL ||
		currToken.type() == TokenType.NIL ||
		currToken.type() == TokenType.NEW ||
		currToken.type() == TokenType.ID ||
		currToken.type() == TokenType.NEG ||
		currToken.type() == TokenType.NOT ||
		currToken.type() == TokenType.LPAREN)
			return true;
	else return false;
  }
  
  /*
  Done?		Debugged?
	XX stmts
	XX bstmts
	XX stmt
	XX bstmt
	XX tdecl
	XX vdecls
	XX fdecl
	XX params
	XX dtype
	XX exit
	XX vdecl
	XX assign
	XX lvalue
	XX cond
	XX condt
	XX whilef
	XX forf
	XX expr
	XX operator
	XX rvalue
	XX pval
	XX idrval
	XX exprlist
  */
}
