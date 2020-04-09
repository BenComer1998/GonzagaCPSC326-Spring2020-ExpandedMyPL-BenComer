
/**
 * Author: Ben Comer
 * Assign: 5
 * File: TypeChecker.java
 *
 * Visitor implementation of Semantic Analysis Checking for the MyPL
 * AST. Note the following conventions for representing type
 * information:
 * 
 * A variable name's type is a string (varname to string)
 *
 * A structured type name is a map of var mappings (typename to Map) where
 * each variable name is mapped to its type 
 *
 * A function type name is a list of parameter types (name to
 * List) where the list consists of each formal param type ending with
 * the return type of the function.
 *
 * For more information on the general design see the lecture notes.
 */

import java.io.InputStream; // DEBUG
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;


public class TypeChecker implements Visitor {
  // the symbol table
  private SymbolTable symbolTable = new SymbolTable();
  // holds last inferred type
  private String currType = null;
  private Integer globalEnvId = null;
  private boolean gotGlobal = false;
  private boolean hasReturn = false;

  // sets up the initial environment for type checking
  public TypeChecker() {
    symbolTable.pushEnvironment();
    // add return type for global scope
    symbolTable.addName("return");
    symbolTable.setInfo("return", "int");
    // print function
    symbolTable.addName("print");
    symbolTable.setInfo("print", List.of("string", "nil"));
    // read function
    symbolTable.addName("read");
    symbolTable.setInfo("read", List.of("string"));
	
	// length function
	symbolTable.addName("length");
	symbolTable.setInfo("length", List.of("string", "int"));
	// get function
	symbolTable.addName("get");
	symbolTable.setInfo("get", List.of("int", "string", "char"));
	// concat function
	symbolTable.addName("concat");
	symbolTable.setInfo("concat", List.of("string", "string", "string"));
	// append function
	symbolTable.addName("append");
	symbolTable.setInfo("append", List.of("string", "char", "string"));
	
	// itos function
	symbolTable.addName("itos");
	symbolTable.setInfo("itos", List.of("int", "string"));
	// stoi function
	symbolTable.addName("stoi");
	symbolTable.setInfo("stoi", List.of("string", "int"));
	// dtos function
	symbolTable.addName("dtos");
	symbolTable.setInfo("dtos", List.of("double", "string"));
	// stod function
	symbolTable.addName("stod");
	symbolTable.setInfo("stod", List.of("string", "double"));
  }

  
  // visitor functions

  
  public void visit(StmtList node) throws MyPLException {
    symbolTable.pushEnvironment();
	if (!gotGlobal) {
		globalEnvId = symbolTable.getEnvironmentId();
		gotGlobal = true;
	}
	// Global env id for return checking
    for (Stmt s : node.stmts)
      s.accept(this);
    symbolTable.popEnvironment();
  }

  
  public void visit(AssignStmt node) throws MyPLException {
    // check and infer rhs type
    node.rhs.accept(this);
    String rhsType = currType;
    // check and obtain lhs type
    node.lhs.accept(this);
    String lhsType = currType;
    // error if rhs and lhs types don't match
    if (!rhsType.equals("nil") && !rhsType.equals(lhsType)) {
      String msg = "mismatched type in assignment";
      error(msg, node.lhs.path.get(0));
    }
  }
  
  public void visit(VarDeclStmt node) throws MyPLException {
	// checks that the id doesn't already exist
	if (symbolTable.nameExistsInCurrEnv(node.varId.lexeme()))
		error("cannot redeclare variable of same id '" + node.varId.lexeme() + "'", node.varId);
	// gets the type of the right side
	node.varExpr.accept(this);
	// check if a type is given
	if (node.varType == null) {
		if (currType.equals("nil"))
			error("type missing in nil assignment", node.varId);
	}
	else {
		// Checks to see if types match
		if (!currType.equals(node.varType.lexeme()) && !currType.equals("nil"))
			error("primitive type mismatch", node.varType);
		else if (currType.equals("nil"))
			currType = node.varType.lexeme();
	}
	
	// adds the variable to the symbolTable if it checks out
	symbolTable.addName(node.varId.lexeme());
	symbolTable.setInfo(node.varId.lexeme(), currType);
  }

  public void visit(Expr node) throws MyPLException {
	node.first.accept(this);
	// No need to change currType further... if it works
	String lhs = currType;
	if (node.operator != null) {
		node.rest.accept(this);
		String rhs = currType;
	
		// They had better match up
		if (!lhs.equals(rhs) &&
			!(rhs.equals("nil") && (node.operator.type() == TokenType.EQUAL || node.operator.type() == TokenType.NOT_EQUAL)))
			error("mismatched types in expression '" + lhs + "' and '" + rhs + "'", getFirstToken(node));
	
		// Cases with operators
		if ((lhs.equals("int") || lhs.equals("double")) &&
			!(node.operator.type() == TokenType.PLUS ||
			  node.operator.type() == TokenType.MINUS ||
			  node.operator.type() == TokenType.MULTIPLY ||
			  node.operator.type() == TokenType.DIVIDE ||
			  node.operator.type() == TokenType.MODULO ||
			  node.operator.type() == TokenType.EQUAL ||
			  node.operator.type() == TokenType.LESS_THAN ||
			  node.operator.type() == TokenType.GREATER_THAN ||
			  node.operator.type() == TokenType.LESS_THAN_EQUAL ||
			  node.operator.type() == TokenType.GREATER_THAN_EQUAL ||
			  node.operator.type() == TokenType.NOT_EQUAL))
			error("invalid operator used on int or double '" + node.operator.lexeme() + "'", node.operator);
			
		else if (lhs.equals("bool") &&
			!(node.operator.type() == TokenType.AND ||
			  node.operator.type() == TokenType.OR ||
			  node.operator.type() == TokenType.EQUAL ||
			  node.operator.type() == TokenType.NOT_EQUAL))
			error("invalid type in arithmetic expression", getFirstToken(node.first));
			
		else if ((lhs.equals("char") || lhs.equals("string")) &&
			!(node.operator.type() == TokenType.EQUAL ||
			  node.operator.type() == TokenType.NOT_EQUAL))
			error("invalid type in arithmetic expression", getFirstToken(node.first));
			
		else if (lhs.equals("nil"))
			error("cannot use operator with nil", node.operator);
		
		// Setting the boolean type for comparison
		if (node.operator.type() == TokenType.EQUAL ||
			node.operator.type() == TokenType.LESS_THAN ||
			node.operator.type() == TokenType.GREATER_THAN ||
			node.operator.type() == TokenType.LESS_THAN_EQUAL ||
			node.operator.type() == TokenType.GREATER_THAN_EQUAL ||
			node.operator.type() == TokenType.NOT_EQUAL ||
			node.operator.type() == TokenType.AND ||
			node.operator.type() == TokenType.OR)
				currType = "bool";
	}
	
	// if it's negated, it had better be a boolean expression
	if (node.negated && !currType.equals("bool"))
		error("cannot negate non-boolean expression", getFirstToken(node));
  }
  
  public void visit(SimpleTerm node) throws MyPLException {
    node.rvalue.accept(this);
  }

  
  public void visit(ComplexTerm node) throws MyPLException {
    node.expr.accept(this);
  }

  
  public void visit(SimpleRValue node) throws MyPLException {
    if (node.val.type() == TokenType.INT_VAL)
      currType = "int";
    else if (node.val.type() == TokenType.DOUBLE_VAL)
      currType = "double";
    else if (node.val.type() == TokenType.BOOL_VAL)
      currType = "bool";
    else if (node.val.type() == TokenType.CHAR_VAL)
      currType = "char";
    else if (node.val.type() == TokenType.STRING_VAL)
      currType = "string";
    else if (node.val.type() == TokenType.NIL)
      currType = "nil";
  }

  public void visit(LValue node) throws MyPLException {
	// finding the last index of path, making sure the path is correctly implemented
	// check the first id in the path
    String varName = node.path.get(0).lexeme();
    if (!symbolTable.nameExists(varName))
      error("undefined variable '" + varName + "'", node.path.get(0));
    // make sure it isn't function or type name
    if (symbolTable.getInfo(varName) instanceof List)
      error("unexpected function name in rvalue", node.path.get(0));
    if (symbolTable.getInfo(varName) instanceof Map)
      error("unexpected type name in rvalue", node.path.get(0));
    // grab the type
    currType = (String) symbolTable.getInfo(varName);
    if (node.path.size() > 1 && !(symbolTable.getInfo(currType) instanceof Map))
		error("invalid member access for non-structured type", node.path.get(0));
    	
    // path catching
	HashMap<String, String> typeInfo = new HashMap<>();
	for (int i = 1; i < node.path.size(); ++i) {
		// checks that the type has the key
		typeInfo = (HashMap<String, String>) symbolTable.getInfo(currType);
		if (!typeInfo.containsKey(node.path.get(i).lexeme()))
			error("unexpected path identifier '" + node.path.get(i).lexeme() + "' found", node.path.get(i));
		// gets the name and type of the subvar
		varName = node.path.get(i).lexeme();
		// gets currType
		currType = (String) typeInfo.get(varName);
	}
  }
  
  public void visit(NewRValue node) throws MyPLException {
	// Find the ID of the new object
	String varName = node.typeId.lexeme();
	// Check that it exists
	if (!symbolTable.nameExists(varName))
		error("undefined variable '" + varName + "'", node.typeId);
	// Check that it's a struct
	if (!(symbolTable.getInfo(varName) instanceof Map))
		error("undefined structure '" + varName + "'", node.typeId);
	
	currType = varName;
  }
  
  public void visit(IDRValue node) throws MyPLException {
	// finding the last index of path, making sure the path is correctly implemented
	// check the first id in the path
    String varName = node.path.get(0).lexeme();
    if (!symbolTable.nameExists(varName))
      error("undefined variable '" + varName + "'", node.path.get(0));
    // make sure it isn't function or type name
    if (symbolTable.getInfo(varName) instanceof List)
      error("unexpected function name in rvalue", node.path.get(0));
    if (symbolTable.getInfo(varName) instanceof Map)
      error("unexpected type name in rvalue", node.path.get(0));
    // grab the type
    currType = (String) symbolTable.getInfo(varName);
    if (node.path.size() > 1 && !(symbolTable.getInfo(currType) instanceof Map))
		error("invalid member access for non-structured type", node.path.get(0));
    	
    // path catching
	HashMap<String, String> typeInfo = new HashMap<>();
	for (int i = 1; i < node.path.size(); ++i) {
		// checks that the type has the key
		typeInfo = (HashMap<String, String>) symbolTable.getInfo(currType);
		if (!typeInfo.containsKey(node.path.get(i).lexeme()))
			error("unexpected path identifier '" + node.path.get(i).lexeme() + "' found", node.path.get(i));
		// gets the name and type of the subvar
		varName = node.path.get(i).lexeme();
		// gets currType
		currType = (String) typeInfo.get(varName);
	}
	
  }
  
  public void visit(CallRValue node) throws MyPLException {
	// Find the ID of the called function
	String funName = node.funName.lexeme();
	// Check that it exists and is a function
	if (!symbolTable.nameExists(funName))
		error("undefined function '" + funName + "'", node.funName);
	// Check that it's a function
	if (!(symbolTable.getInfo(funName) instanceof List))
		error("function '" + funName + "' not defined", node.funName);
	
	// Get the function info
	List<String> funInfo = (List<String>)symbolTable.getInfo(funName);
	// Checks for correct number of parameters
	if (funInfo.size() - 1 != node.argList.size())
		error("invalid number of parameters", getFirstToken(node.argList.get(0)));
	
	// Checks that types of parameters match
	for (int i = 0; i < funInfo.size() - 1; ++i) {
		node.argList.get(i).accept(this);
		if (!funInfo.get(i).equals(currType) && !currType.equals("nil")) {
			String msg = funName;
			if (funInfo.size() == 1)
				error(funName + " takes no arguments", node.funName);
			else if (funInfo.size() == 2) {
				error(funName + " takes " + funInfo.get(0), node.funName);
			}
			else {
				for (int j = 0; j < node.argList.size() - 1; ++j) {
					msg += " takes ";
					msg += funInfo.get(j);
				}
				msg += " and ";
				msg += funInfo.get(funInfo.size() - 2);
			}
			error(msg, node.funName);
		}
	}
	
	// Set currType to return value
	currType = funInfo.get(funInfo.size() - 1);
  }
  
  public void visit(NegatedRValue node) throws MyPLException {
	// Should be an int or double
	node.expr.accept(this);
	if (currType != "int" && currType != "double")
		error("negated non-int and non-double of type '" + currType + "'", getFirstToken(node));
  }
  
  public void visit(IfStmt node) throws MyPLException {
	// Checks the if statement expression
	node.ifPart.boolExpr.accept(this);
	if (!currType.equals("bool"))
		error("non-boolean type '" + currType + "' not allowed", getFirstToken(node.ifPart.boolExpr));
	node.ifPart.stmtList.accept(this);
	// Iterate through each else if
	for (BasicIf e : node.elsifs) {
		e.boolExpr.accept(this);
		if (!currType.equals("bool"))
			error("non-boolean type '" + currType + "' not allowed", getFirstToken(e.boolExpr));
		e.stmtList.accept(this);
	}
	// Do the else, if it exists
	if (node.hasElse)
		node.elseStmtList.accept(this);
  }
  
  public void visit(WhileStmt node) throws MyPLException {
	// Checks the boolExpr
	node.boolExpr.accept(this);
	if (!currType.equals("bool"))
		error("non-boolean type '" + currType + "' not allowed", getFirstToken(node.boolExpr));
	node.stmtList.accept(this);
  }
  
  public void visit(ForStmt node) throws MyPLException {
	node.startExpr.accept(this);
	if (!currType.equals("int"))
		error("start expr must be of type int", getFirstToken(node.startExpr));
	node.endExpr.accept(this);
	if (!currType.equals("int"))
		error("end expr must be of type int", getFirstToken(node.endExpr));
	
	symbolTable.pushEnvironment();
	
	// Makes the var
	symbolTable.addName(node.var.lexeme());
	symbolTable.setInfo(node.var.lexeme(), "int");
	// Passes the buck
	node.stmtList.accept(this);
	
	symbolTable.popEnvironment();
  }
  
  public void visit(FunDeclStmt node) throws MyPLException {
	// Find the ID of the declared function
	String funName = node.funName.lexeme();
	// Check that no function or variable of the same name already exists within the scope
	if (symbolTable.nameExists(funName))
		error("declaring an already declared function '" + funName + "'", node.funName);
	
	symbolTable.addName(funName);
	// Creates an empty list for info
	List<String> paramList = new ArrayList<>();
	// Adds all parameters to the List
	for (FunParam fp : node.params) {
		paramList.add(fp.paramType.lexeme());
	}
	// Sets the info (essential for recursion)
	symbolTable.setInfo(funName, paramList);
	// Adds the return type to the end of the List
	paramList.add(node.returnType.lexeme());
	// Shadow work
	Map<String, Object> shadow = new HashMap<>();
	List<String> shadowNames = new ArrayList<>();
	for (FunParam fp : node.params) {
		if (symbolTable.nameExists(fp.paramName.lexeme())) {
			shadow.put(fp.paramName.lexeme(), symbolTable.getInfo(fp.paramName.lexeme()));
			shadowNames.add(fp.paramName.lexeme());
			symbolTable.setInfo(fp.paramName.lexeme(), fp.paramType.lexeme());
		}
	}
	symbolTable.pushEnvironment();
	
	// Requires the correct return typeId
	symbolTable.addName("return");
	if (node.returnType.lexeme() != "nil")
		symbolTable.setInfo("return", node.returnType.lexeme());
	else
		symbolTable.setInfo("return", "nil");
	
	for (FunParam fp : node.params) {
		symbolTable.addName(fp.paramName.lexeme());
		symbolTable.setInfo(fp.paramName.lexeme(), fp.paramType.lexeme());
	}
	String returnType = node.returnType.lexeme();
	node.stmtList.accept(this);
	if (!hasReturn)
		currType = "nil";
	
	if (!returnType.equals(currType))
		error("return values not matching", node.returnType);
	hasReturn = false;
	
	symbolTable.popEnvironment();
	// Resetting the variables
	for (String name : shadowNames) {
		symbolTable.setInfo(name, shadow.get(name));
	}
  }
  
  public void visit(TypeDeclStmt node) throws MyPLException {
	// Makes sure the id doesn't already exist
	if (symbolTable.nameExists(node.typeId.lexeme()))
		error("declaring an already declared structure '" + node.typeId.lexeme() + "' in current environment", node.typeId);
	
	// Adds the struct to the symbolTable
	symbolTable.addName(node.typeId.lexeme());
	Map<String, String> subVars = new HashMap<>();
	symbolTable.pushEnvironment();
	
	for (VarDeclStmt v : node.fields) {
		// Type-checks
		v.accept(this);
		// Adds a pair to the map
		subVars.put(v.varId.lexeme(), currType);
	}
	
	symbolTable.popEnvironment();
	symbolTable.setInfo(node.typeId.lexeme(), subVars);
  }
  
  public void visit(ReturnStmt node) throws MyPLException {
	hasReturn = true;
	if (node.returnExpr != null) {
		node.returnExpr.accept(this);
		if (symbolTable.getEnvironmentId().equals(globalEnvId) &&
			!currType.equals("int") && !currType.equals("nil"))
				error("global return must be int or nil", getFirstToken(node.returnExpr));
	}
	else
		currType = "nil";
  }
  
  // helper functions

  private void error(String msg, Token token) throws MyPLException {
    int row = token.row();
    int col = token.column();
    throw new MyPLException("Type", msg, row, col);
  }

  // gets first token of an expression
  private Token getFirstToken(Expr node) {
    return getFirstToken(node.first);
  }

  // gets first token of an expression term
  private Token getFirstToken(ExprTerm node) {
    if (node instanceof SimpleTerm)
      return getFirstToken(((SimpleTerm)node).rvalue);
    else
      return getFirstToken(((ComplexTerm)node).expr);      
  }

  // gets first token of an rvalue
  private Token getFirstToken(RValue node) {
    if (node instanceof SimpleRValue)
      return ((SimpleRValue)node).val;
    else if (node instanceof CallRValue)
      return ((CallRValue)node).funName;
    else if (node instanceof IDRValue)
      return ((IDRValue)node).path.get(0);
    else if (node instanceof NegatedRValue) 
      return getFirstToken(((NegatedRValue)node).expr);
    else 
      return ((NewRValue)node).typeId;
  }

}    

