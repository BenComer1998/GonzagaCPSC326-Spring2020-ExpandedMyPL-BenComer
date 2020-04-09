
import java.util.ArrayList;

public class ArrayDeclStmt implements Stmt {
	
  public Token arrayId = null;
  public Token arrayType = null;
  public ArrayList<Token> elements = new ArrayList<>();
  
  public void accept(Visitor visitor) throws MyPLException {
    visitor.visit(this);
  }
  
}
