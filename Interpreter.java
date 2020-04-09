/**
 * Author: Ben Comer
 * Assign: 7
 * File: Interpreter.java
 *
 * Visitor implementation of a basic "Pure AST" Interpreter for MyPL. 
 */

// DEBUG
import java.util.Set;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.InputStreamReader;


public class Interpreter implements Visitor {
  private SymbolTable symbolTable = new SymbolTable();
  private Object currVal = null;
  private Map<Integer, Map<String, Object>> heap = new HashMap<>();
  
  public Integer run(StmtList stmtList) throws MyPLException {
    try {
		stmtList.accept(this);
		return 0;
	} catch (MyPLException e) {
		if (!e.isReturnException())
			throw e;
		Object returnVal = e.getReturnValue();
		if (returnVal == null)
			return 0;
		return (Integer)returnVal;
	}
  }

  
  // visitor functions

  
  public void visit(StmtList node) throws MyPLException {
    symbolTable.pushEnvironment();
    for (Stmt s : node.stmts) {
      s.accept(this);
    }
    symbolTable.popEnvironment();    
  }

  
  public void visit(VarDeclStmt node) throws MyPLException {  
    // TODO: HW6 XXX
	
	// Already typechecked, gonna add to symbolTable
	symbolTable.addName(node.varId.lexeme());
	node.varExpr.accept(this);
	symbolTable.setInfo(node.varId.lexeme(), currVal);
  }

  
  public void visit(AssignStmt node) throws MyPLException {
    // evaluate rhs
    node.rhs.accept(this);
    // let LValue do the assignment
    node.lhs.accept(this);
  }

  
  public void visit(ReturnStmt node) throws MyPLException {
    // TODO: HW7 XXX
	
	node.returnExpr.accept(this);
	Object returnVal = currVal;
	throw new MyPLException(returnVal);
  }

  
  public void visit(IfStmt node) throws MyPLException {
    // TODO: HW6 XXX
	
	Expr boolExpr = null;
	node.ifPart.boolExpr.accept(this);
	if ((Boolean)currVal)
		node.ifPart.stmtList.accept(this);
	else {
		int i = 0; boolean found = false;
		while (!found && i < node.elsifs.size()) {
			node.elsifs.get(i).boolExpr.accept(this);
			if ((Boolean)currVal) {
				found = true;
				node.elsifs.get(i).stmtList.accept(this);
			}
			++i;
		}
		
		if (!found && node.hasElse) {
			node.elseStmtList.accept(this);
		}
	}
  }

  
  public void visit(WhileStmt node) throws MyPLException {
    // TODO: HW6 XXX
	
	node.boolExpr.accept(this);
	while ((Boolean)currVal) {
		node.stmtList.accept(this);
		node.boolExpr.accept(this);
	}
  }

  
  public void visit(ForStmt node) throws MyPLException {
    // TODO: HW6 XXX
	
	node.endExpr.accept(this);
	Integer end_val = (Integer)currVal;
	symbolTable.pushEnvironment();
	
		symbolTable.addName(node.var.lexeme());
		node.startExpr.accept(this);
		if ((Integer)currVal > (Integer)end_val) {
			symbolTable.popEnvironment();
			return;
		}
		symbolTable.setInfo(node.var.lexeme(), (Integer)currVal);
		do {
			node.stmtList.accept(this);
			symbolTable.setInfo(node.var.lexeme(), (Integer)symbolTable.getInfo(node.var.lexeme()) + 1);
		} while ((Integer)symbolTable.getInfo(node.var.lexeme()) <= end_val);
	
	symbolTable.popEnvironment();
  }


  public void visit(TypeDeclStmt node) throws MyPLException {
    // TODO: HW7 XXX
	
	symbolTable.addName(node.typeId.lexeme());
	int envId = symbolTable.getEnvironmentId();
	List<Object> typeInfo = List.of(envId, node);
	symbolTable.setInfo(node.typeId.lexeme(), typeInfo);
  }


  public void visit(FunDeclStmt node) throws MyPLException {
    // TODO: HW7 XXX
	
	symbolTable.addName(node.funName.lexeme());
	int envId = symbolTable.getEnvironmentId();
	List<Object> typeInfo = List.of(envId, node);
	symbolTable.setInfo(node.funName.lexeme(), typeInfo);
  }

  
  // expressions
  
  public void visit(Expr node) throws MyPLException {
    // TODO: HW6 XXX
    //
    // The following is a basic sketch of what you need to
    // implement. You will need to fill in the remaining code to get
    // Expr working.

    node.first.accept(this);
    Object firstVal = currVal;
    
    if (node.operator != null) {
      node.rest.accept(this);
      Object restVal = currVal;
      String op = node.operator.lexeme();

      // Check for null values (all except == and !=)
      // if you find a null value report an error

      // basic math ops (+, -, *, /, %)
      if (op.equals("+")) {
		if (firstVal == null)
		  error("cannot operate with nil", getFirstToken(node.first));
	    if (restVal == null)
		  error("cannot operate with nil", getFirstToken(node.rest));
		
        if (firstVal instanceof Integer)
          currVal = (Integer)firstVal + (Integer)restVal;
        else 
          currVal = (Double)firstVal + (Double)restVal;
      }
      else if (op.equals("-")) {
		if (firstVal == null)
		  error("cannot operate with nil", getFirstToken(node.first));
	    if (restVal == null)
		  error("cannot operate with nil", getFirstToken(node.rest));
	  
        if (firstVal instanceof Integer)
          currVal = (Integer)firstVal - (Integer)restVal;
        else 
          currVal = (Double)firstVal - (Double)restVal;
      }
      else if (op.equals("*")) {
		if (firstVal == null)
		  error("cannot operate with nil", getFirstToken(node.first));
	    if (restVal == null)
		  error("cannot operate with nil", getFirstToken(node.rest));
	
        if (firstVal instanceof Integer)
          currVal = (Integer)firstVal * (Integer)restVal;
        else 
          currVal = (Double)firstVal * (Double)restVal;
      }
      else if (op.equals("/")) {
		if (firstVal == null)
		  error("cannot operate with nil", getFirstToken(node.first));
	    if (restVal == null)
		  error("cannot operate with nil", getFirstToken(node.rest));
	  
        if (firstVal instanceof Integer)
          currVal = (Integer)firstVal / (Integer)restVal;
        else 
          currVal = (Double)firstVal / (Double)restVal;
      }
      else if (op.equals("%")) {
		if (firstVal == null)
		  error("cannot operate with nil", getFirstToken(node.first));
	    if (restVal == null)
		  error("cannot operate with nil", getFirstToken(node.rest));
	  
        if (firstVal instanceof Integer)
          currVal = (Integer)firstVal % (Integer)restVal;
        else 
          currVal = (Double)firstVal % (Double)restVal;
      }

      // boolean operators (and, or)
      else if (op.equals("and")) {
		if (firstVal == null)
		  error("cannot operate with nil", getFirstToken(node.first));
	    if (restVal == null)
		  error("cannot operate with nil", getFirstToken(node.rest));
	  
        currVal = (Boolean)firstVal && (Boolean)restVal;
      }
      else if (op.equals("or")) {
		if (firstVal == null)
		  error("cannot operate with nil", getFirstToken(node.first));
	    if (restVal == null)
		  error("cannot operate with nil", getFirstToken(node.rest));
	  
        currVal = (Boolean)firstVal || (Boolean)restVal;
      }
        
      // relational comparators (=, !=, <, >, <=, >=)
      else if (op.equals("=")) {
        if (firstVal == null ^ restVal == null)
			currVal = false;
	    else if (firstVal == null && restVal == null)
			currVal = true;
		else
			currVal = firstVal == restVal;
      }
      else if (op.equals("!=")) {
        if (firstVal == null ^ restVal == null)
			currVal = true;
	    else if (firstVal == null && restVal == null)
			currVal = false;
		else
			currVal = firstVal != restVal;
      }
      else if (op.equals("<")) {
		if (firstVal == null)
		  error("cannot compare with nil", getFirstToken(node.first));
	    if (restVal == null)
		  error("cannot compare with nil", getFirstToken(node.rest));
	  
        if (firstVal instanceof Integer)
          currVal = (Integer)firstVal < (Integer)restVal;
        else if (firstVal instanceof Double)
          currVal = (Double)firstVal < (Double)restVal;
        else
          currVal = ((String)firstVal).compareTo((String)restVal) < 0;
      }
      else if (op.equals("<=")) {
		if (firstVal == null)
		  error("cannot compare with nil", getFirstToken(node.first));
	    if (restVal == null)
		  error("cannot compare with nil", getFirstToken(node.rest));
	  
        if (firstVal instanceof Integer)
          currVal = (Integer)firstVal <= (Integer)restVal;
        else if (firstVal instanceof Double)
          currVal = (Double)firstVal <= (Double)restVal;
        else
          currVal = ((String)firstVal).compareTo((String)restVal) <= 0;
      }
      else if (op.equals(">")) {
		if (firstVal == null)
		  error("cannot compare with nil", getFirstToken(node.first));
	    if (restVal == null)
		  error("cannot compare with nil", getFirstToken(node.rest));
	  
        if (firstVal instanceof Integer)
          currVal = (Integer)firstVal > (Integer)restVal;
        else if (firstVal instanceof Double)
          currVal = (Double)firstVal > (Double)restVal;
        else
          currVal = ((String)firstVal).compareTo((String)restVal) > 0;
      }
      else if (op.equals(">=")) {
		if (firstVal == null)
		  error("cannot compare with nil", getFirstToken(node.first));
	    if (restVal == null)
		  error("cannot compare with nil", getFirstToken(node.rest));
	  
        if (firstVal instanceof Integer)
          currVal = (Integer)firstVal >= (Integer)restVal;
        else if (firstVal instanceof Double)
          currVal = (Double)firstVal >= (Double)restVal;
        else
          currVal = ((String)firstVal).compareTo((String)restVal) >= 0;
      }
    }
    // deal with not operator
    if (node.negated) {
      currVal = !(Boolean)currVal;
    }
  }


  public void visit(LValue node) throws MyPLException {
    // TODO: HW7 XXX
	
	if (node.path.size() == 1)
		symbolTable.setInfo(node.path.get(0).lexeme(), currVal);
	else {
		// path catching
		String varName = node.path.get(0).lexeme();
		Map<String, Object> obj = (Map<String, Object>)heap.get(symbolTable.getInfo(varName));

		for (int i = 1; i < node.path.size(); ++i) {
			varName = node.path.get(i).lexeme();
			if (i != node.path.size() - 1)
				obj = heap.get(obj.get(varName));
			else
				obj.replace(varName, currVal);
		}
	}
  }

  
  public void visit(SimpleTerm node) throws MyPLException {
    node.rvalue.accept(this);
  }

  
  public void visit(ComplexTerm node) throws MyPLException {
    node.expr.accept(this);
  }

  
  public void visit(SimpleRValue node) throws MyPLException {
    if (node.val.type() == TokenType.INT_VAL)
      currVal = Integer.parseInt(node.val.lexeme());
    else if (node.val.type() == TokenType.DOUBLE_VAL)
      currVal = Double.parseDouble(node.val.lexeme());
    else if (node.val.type() == TokenType.BOOL_VAL)
      currVal = Boolean.parseBoolean(node.val.lexeme());
    else if (node.val.type() == TokenType.CHAR_VAL)
      currVal = node.val.lexeme(); // leave as single character string
    else if (node.val.type() == TokenType.STRING_VAL)
      currVal = node.val.lexeme();
    else if (node.val.type() == TokenType.NIL)
      currVal = null;
  }

  
  public void visit(NewRValue node) throws MyPLException {
    // TODO: HW7 XXX
	
	List<Object> typeInfo = (List<Object>)symbolTable.getInfo(node.typeId.lexeme());
	int currEnv = symbolTable.getEnvironmentId();
	symbolTable.setEnvironmentId((int)typeInfo.get(0));
	
	Map<String, Object> obj = new HashMap<>();
	int objectId = System.identityHashCode(obj);
	symbolTable.pushEnvironment();
	
	for (int i = 0; i < ((TypeDeclStmt)typeInfo.get(1)).fields.size(); ++i) {
		// Gets the varId, that's the key
		String memberId = ((VarDeclStmt)((TypeDeclStmt)typeInfo.get(1)).fields.get(i)).varId.lexeme();
		// Gets the varExpr and visits, currVal's the value
		((VarDeclStmt)((TypeDeclStmt)typeInfo.get(1)).fields.get(i)).varExpr.accept(this);
		// Puts it in the obj Map
		obj.put(memberId, currVal);
		// Repeat for all varDecls
	}
	
	symbolTable.popEnvironment();
	symbolTable.setEnvironmentId(currEnv);
	
	heap.put(objectId, obj);
	/*symbolTable.addName(node.typeId.lexeme());
	symbolTable.setInfo(node.typeId.lexeme(), objectId);*/
	currVal = objectId;
  }

  public void visit(CallRValue node) throws MyPLException {
    List<String> builtIns = List.of("print", "read", "length", "get",
                                    "concat", "append", "itos", "stoi",
                                    "dtos", "stod");
    String funName = node.funName.lexeme();
    if (builtIns.contains(funName)) {
      try {
		callBuiltInFun(node);
	  } catch (MyPLException e) {
		if (e.isReturnException())
			currVal = e.getReturnValue();
		else {
			System.out.println(e);
			System.exit(1);
		}
	  }
	  return;
	}
    
	// If it's a user-defined function...
	// TODO: HW7 XX
	
	List<Object> funInfo = (List<Object>)symbolTable.getInfo(node.funName.lexeme());
	int currEnv = symbolTable.getEnvironmentId();
	
	// Storing parameters
	List<Object> args = new ArrayList<Object>();
	for (int i = 0; i < node.argList.size(); ++i) {
		node.argList.get(i).accept(this);
		args.add(currVal);
	}
	
	symbolTable.setEnvironmentId((int)funInfo.get(0));
	symbolTable.pushEnvironment();
	
	// Initializing the new parameters
	String paramId = "";
	for (int i = 0; i < args.size(); ++i) {
		paramId = ((FunParam)((FunDeclStmt)funInfo.get(1)).params.get(i)).paramName.lexeme();
		symbolTable.addName(paramId);
		symbolTable.setInfo(paramId, args.get(i));
	}
	try {
		((FunDeclStmt)funInfo.get(1)).stmtList.accept(this);
		currVal = null; // if no return is found
	} catch (MyPLException e) {
		if (e.isReturnException())
			currVal = e.getReturnValue();
		else {
			System.out.println(e);
			System.exit(1);
		}
	}
	
	symbolTable.popEnvironment();
	symbolTable.setEnvironmentId(currEnv);
  }

  
  public void visit(IDRValue node) throws MyPLException {
	// TODO: HW7 XXX
	
    String varName = node.path.get(0).lexeme();
	
	// path catching
	if (node.path.size() > 1) {
		Map<String, Object> obj = (Map<String, Object>)heap.get(symbolTable.getInfo(varName));
		for (int i = 1; i < node.path.size(); ++i) {
			varName = node.path.get(i).lexeme();
			
			if (i != node.path.size() - 1)
				obj = heap.get(obj.get(varName));
			else
				currVal = (Object)obj.get(varName);
		}
	}
	// or single-path
	else {
		currVal = symbolTable.getInfo(varName);
	}
  }


  public void visit(NegatedRValue node) throws MyPLException {
    node.expr.accept(this);
    if (currVal instanceof Integer)
      currVal = -(Integer)currVal;
    else
      currVal = -(Double)currVal;
  }

  
  // helper functions
  
  private void callBuiltInFun(CallRValue node) throws MyPLException {
    // TODO: HW6 XXX
    // Most of function is completed, fill in rest below
    
    String funName = node.funName.lexeme();
    // get the function arguments
    List<Object> argVals = new ArrayList<>();
    for (Expr arg : node.argList) {
      arg.accept(this);
      // make sure no null values
      if (currVal == null)
        error("nil value", getFirstToken(arg));
      argVals.add(currVal);
    }
    if (funName.equals("print")) {
      // Fix '\' 'n' issue
      String msg = (String)argVals.get(0);
      msg = msg.replace("\\n", "\n");
      msg = msg.replace("\\t", "\t");
      System.out.print(msg);
      currVal = null;
    }
    else if (funName.equals("read")) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      try {
        currVal = reader.readLine();
      }
      catch(Exception e) {
        currVal = null;
      }
    }
    else if (funName.equals("get")) {
      int index = (Integer)argVals.get(0);
      String str = (String)argVals.get(1);
      if (index >= str.length())
		error("index selected out of range", node.funName);
	  currVal = (Character)str.charAt(index);
    }
    else if (funName.equals("concat")) {
      currVal = (String)argVals.get(0) + (String)argVals.get(1);
    }
    else if (funName.equals("append")) {
	  if (argVals.get(1) instanceof Character)
		currVal = (String)argVals.get(0) + (Character)argVals.get(1);
	  else
		currVal = (String)argVals.get(0) + (String)argVals.get(1);
    }
	else if (funName.equals("length")) {
	  currVal = (Integer)((String)argVals.get(0)).length();
	}
    else if (funName.equals("itos")) {
      currVal = argVals.get(0).toString();
    }
    else if (funName.equals("stoi")) {
      currVal = Integer.parseInt((String)argVals.get(0));
    }
    else if (funName.equals("dtos")) {
      currVal = argVals.get(0).toString();
    }
    else if (funName.equals("stod")) {
      currVal = Double.parseDouble((String)argVals.get(0));
    }
  }

  
  private void error(String msg, Token token) throws MyPLException {
    int row = token.row();
    int col = token.column();
    throw new MyPLException("\nRuntime", msg, row, col);
  }

  
  private Token getFirstToken(Expr node) {
    return getFirstToken(node.first);
  }

  
  private Token getFirstToken(ExprTerm node) {
    if (node instanceof SimpleTerm)
      return getFirstToken(((SimpleTerm)node).rvalue);
    else
      return getFirstToken(((ComplexTerm)node).expr);      
  }

  
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

// X: Began, untested
// XX: Completed, untested
// XXX: Completed, tested
