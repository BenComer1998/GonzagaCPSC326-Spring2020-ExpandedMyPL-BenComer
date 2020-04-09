/**
 * Author: Benjamin Comer
 * Assign: 2
 *
 * The lexer implementation tokenizes a given input stream. The lexer
 * implements a pull-based model via the nextToken function such that
 * each call to nextToken advances the lexer to the next token (which
 * is returned by nextToken). The file has been completed read when
 * nextToken returns the EOS token. Lexical errors in the source file
 * result in the nextToken function throwing a MyPL Exception.
 */

import java.util.*;
import java.io.*;


public class Lexer {

  private BufferedReader buffer; // handle to input stream
  private int line;
  private int column;
  
  /** 
   */
  public Lexer(InputStream instream) {
    buffer = new BufferedReader(new InputStreamReader(instream));
    this.line = 1;
    this.column = 1;
  }

  
  /**
   * Returns next character in the stream. Returns -1 if end of file.
   */
  private int read() throws MyPLException {
    try {
      int ch = buffer.read();
      return ch;
    } catch(IOException e) {
      error("read error read", line, column);
    }
    return -1;
  }

  
  /** 
   * Returns next character without removing it from the stream.
   */
  private int peek() throws MyPLException {
    int ch = -1;
    try {
      buffer.mark(1);
      ch = read();
      buffer.reset();
    } catch(IOException e) {
		e.printStackTrace();
      error("read error peek", line, column);
    }
    return ch;
  }


  /**
   * Print an error message and exit the program.
   */
  private void error(String msg, int line, int column) throws MyPLException {
    throw new MyPLException("Lexer", msg, line, column);
  }


  /**
   * Grabs the next token from the MyPL program.
   */
  public Token nextToken() throws MyPLException {
	char symbol = ' ';
	String lexeme = "";
	Token new_token = new Token(TokenType.NIL, "BAD", line, column);
	
	if (peek() == -1) {
		return new Token(TokenType.EOS, "", line, column);
	}
	
	// Whitespace getter-ridder-of
		do {
			if (peek() == -1) {
				return new Token(TokenType.EOS, "", line, column);
			}

			symbol = (char) peek();
			if (symbol == ' ') {
				read();
				++column;
			}
			// WORKS ON ADA, BUT NOT GIT BASH.
			// Coming to office hours to figure out a solution.
			else if (symbol == '\r' || symbol == '\n') {
				if (symbol == '\r')
					read();
				
				read();
				++line;
				column = 1;
			}
			else if (symbol == '\t') {
				read();
				line += 4;
			}
			else if (symbol == '#') {
				read();
				while ((char) peek() != '\r' && (char) peek() != '\n') {
					read();
				}
				if ((char) peek() == '\r')
					read();
				
				read();
				++line;
				column = 1;
			}
			else break;
		} while (true);
	
	int new_column = column;
	
	// Reads a string
		// If the first char is a letter...
		if (Character.isLetter((char) peek())) {
			do {
				lexeme += (char) read(); ++new_column;
			} while (Character.isLetter((char) peek()));
			
			// If contains a digit or underscore, it is an ID token
			if (Character.isDigit((char) peek()) ||
				(char) peek() == '_') {
				
				while (Character.isLetter((char) peek()) ||
					   Character.isDigit((char) peek()) ||
					   (char) peek() == '_') {
					
					lexeme += (char) read(); ++new_column;
				}
				
				new_token = new Token(TokenType.ID, lexeme, line, column);
			}
			// Otherwise, it could be a whole host of tokens.
			else {
				if (lexeme.equals("true") || lexeme.equals("false"))
					new_token = new Token(TokenType.BOOL_VAL, lexeme, line, column);
				else if (lexeme.equals("int"))
					new_token = new Token(TokenType.INT_TYPE, lexeme, line, column);
				else if (lexeme.equals("double"))
					new_token = new Token(TokenType.DOUBLE_TYPE, lexeme, line, column);
				else if (lexeme.equals("char"))
					new_token = new Token(TokenType.CHAR_TYPE, lexeme, line, column);
				else if (lexeme.equals("string"))
					new_token = new Token(TokenType.STRING_TYPE, lexeme, line, column);
				else if (lexeme.equals("bool"))
					new_token = new Token(TokenType.BOOL_TYPE, lexeme, line, column);
				else if (lexeme.equals("nil"))
					new_token = new Token(TokenType.NIL, lexeme, line, column);
				else if (lexeme.equals("and"))
					new_token = new Token(TokenType.AND, lexeme, line, column);
				else if (lexeme.equals("or"))
					new_token = new Token(TokenType.OR, lexeme, line, column);
				else if (lexeme.equals("not"))
					new_token = new Token(TokenType.NOT, lexeme, line, column);
				else if (lexeme.equals("neg"))
					new_token = new Token(TokenType.NEG, lexeme, line, column);
				else if (lexeme.equals("while"))
					new_token = new Token(TokenType.WHILE, lexeme, line, column);
				else if (lexeme.equals("for"))
					new_token = new Token(TokenType.FOR, lexeme, line, column);
				else if (lexeme.equals("to"))
					new_token = new Token(TokenType.TO, lexeme, line, column);
				else if (lexeme.equals("do"))
					new_token = new Token(TokenType.DO, lexeme, line, column);
				else if (lexeme.equals("if"))
					new_token = new Token(TokenType.IF, lexeme, line, column);
				else if (lexeme.equals("then"))
					new_token = new Token(TokenType.THEN, lexeme, line, column);
				else if (lexeme.equals("else"))
					new_token = new Token(TokenType.ELSE, lexeme, line, column);
				else if (lexeme.equals("elif"))
					new_token = new Token(TokenType.ELIF, lexeme, line, column);
				else if (lexeme.equals("end"))
					new_token = new Token(TokenType.END, lexeme, line, column);
				else if (lexeme.equals("array"))
					new_token = new Token(TokenType.ARRAY, lexeme, line, column);
				else if (lexeme.equals("fun"))
					new_token = new Token(TokenType.FUN, lexeme, line, column);
				else if (lexeme.equals("var"))
					new_token = new Token(TokenType.VAR, lexeme, line, column);
				else if (lexeme.equals("set"))
					new_token = new Token(TokenType.SET, lexeme, line, column);
				else if (lexeme.equals("return"))
					new_token = new Token(TokenType.RETURN, lexeme, line, column);
				else if (lexeme.equals("new"))
					new_token = new Token(TokenType.NEW, lexeme, line, column);
				else if (lexeme.equals("type"))
					new_token = new Token(TokenType.TYPE, lexeme, line, column);
				else {
					// If it is not a keyword, it must be an id
					new_token = new Token(TokenType.ID, lexeme, line, column);
				}
			}
		}
		
		// If the first char is a digit...
		else if (Character.isDigit((char) peek())) {
			boolean decimal = false;
			lexeme += (char) read(); ++new_column;
			while (peek() != -1 && Character.isDigit((char) peek()) || (char) peek() == '.') {
				if ((char) peek() == '.' && decimal) {
					while (peek() != -1 && Character.isDigit((char) peek()) || (char) peek() == '.') {
						lexeme += (char) read(); ++new_column;
					}
					error("extra dot in '" + lexeme + "'", line, column);
				}
				else if ((char) peek() == '.') {
					decimal = true;
				}
				lexeme += (char) read(); ++new_column;
			}
			if (Character.isLetter((char) peek()) || (char) peek() == '_') {
				error("unexpected symbol '" + (char) peek() + "'", line, new_column);
			}
			if (decimal && lexeme.endsWith(".")) {
				error("missing digit in float '" + lexeme + "'", line, column);
			}
			if (lexeme.charAt(0) == '0' && lexeme.length() != 1 && lexeme.charAt(1) != '.') {
				error("leading zero in '" + lexeme + "'", line, column);
			}
			
			if (decimal)
				new_token = new Token(TokenType.DOUBLE_VAL, lexeme, line, column);
			else
				new_token = new Token(TokenType.INT_VAL, lexeme, line, column);
		}
		
		// If the first char is an apostrophe
		else if ((char) peek() == '\'') {
			read(); ++new_column;
			lexeme += (char) read();
			if ((char) read() == '\'') {
				new_token = new Token(TokenType.CHAR_VAL, lexeme, line, column);
				column += 3; ++new_column;
				return new_token;
			}
			else {
				error("missing apostrophe", line, column + 2);
			}
		}
		
		// If the first char is a quotation mark
		else if ((char) peek() == '\"') {
			read(); ++new_column;
			while (!((char) peek() == '\"') && !((char) peek() == '\n')) {
				lexeme += (char) read(); ++new_column;
			}
			if ((char) peek() == '\n') {
				error("found newline within string", line, new_column);
			}
			read(); ++new_column;
			new_token = new Token(TokenType.STRING_VAL, lexeme, line, column);
		}
		
		// If the first char is an operator...
		else if ((char) peek() == '+') {
			lexeme += (char) read(); ++new_column;
			new_token = new Token(TokenType.PLUS, lexeme, line, column);
		}
		else if ((char) peek() == '-') {
			lexeme += (char) read(); ++new_column;
			new_token = new Token(TokenType.MINUS, lexeme, line, column);
		}
		else if ((char) peek() == '*') {
			lexeme += (char) read(); ++new_column;
			new_token = new Token(TokenType.MULTIPLY, lexeme, line, column);
		}
		else if ((char) peek() == '/') {
			lexeme += (char) read(); ++new_column;
			new_token = new Token(TokenType.DIVIDE, lexeme, line, column);
		}
		else if ((char) peek() == '%') {
			lexeme += (char) read(); ++new_column;
			new_token = new Token(TokenType.MODULO, lexeme, line, column);
		}
		else if ((char) peek() == ',') {
			lexeme += (char) read(); ++new_column;
			new_token = new Token(TokenType.COMMA, lexeme, line, column);
		}
		else if ((char) peek() == '.') {
			lexeme += (char) read(); ++new_column;
			new_token = new Token(TokenType.DOT, lexeme, line, column);
		}
		else if ((char) peek() == '=') {
			lexeme += (char) read(); ++new_column;
			new_token = new Token(TokenType.EQUAL, lexeme, line, column);
		}
		else if ((char) peek() == '>') {
			lexeme += (char) read(); ++new_column;
			if ((char) peek() == '=') {
				lexeme += (char) read(); ++new_column;
				new_token = new Token(TokenType.GREATER_THAN_EQUAL,
									  lexeme, line, column);
			}
			else new_token = new Token(TokenType.GREATER_THAN,
									   lexeme, line, column);
		}
		else if ((char) peek() == '<') {
			lexeme += (char) read(); ++new_column;
			if ((char) peek() == '=') {
				lexeme += (char) read(); ++new_column;
				new_token = new Token(TokenType.LESS_THAN_EQUAL,
									  lexeme, line, column);
			}
			else new_token = new Token(TokenType.LESS_THAN,
									   lexeme, line, column);
		}
		else if ((char) peek() == '!') {
			lexeme += (char) read(); ++new_column;
			if ((char) peek() == '=') {
				lexeme += (char) read(); ++new_column;
				new_token = new Token(TokenType.NOT_EQUAL,
									  lexeme, line, column);
			}
			else {
				error("unexpected symbol '!'", line, column);
			}
		}
		else if ((char) peek() == ':') {
			lexeme += (char) read(); ++new_column;
			if ((char) peek() == '=') {
				lexeme += (char) read(); ++new_column;
				new_token = new Token(TokenType.ASSIGN,
									  lexeme, line, column);
			}
		}
		else if ((char) peek() == '(') {
			lexeme += (char) read(); ++new_column;
			new_token = new Token(TokenType.LPAREN, lexeme, line, column);		  
		}
		else if ((char) peek() == ')') {
			lexeme += (char) read(); ++new_column;
			new_token = new Token(TokenType.RPAREN, lexeme, line, column);
		}
		else if ((char) peek() == '{') {
			lexeme += (char) read(); ++new_column;
			new_token = new Token(TokenType.LBRACKET, lexeme, line, column);
		}
		else if ((char) peek() == '}') {
			lexeme += (char) read(); ++new_column;
			new_token = new Token(TokenType.RBRACKET, lexeme, line, column);
		}
		else {
			error("unexpected symbol '" + lexeme + "'", line, column);
		}
			
	column = new_column;
	return new_token;
  }
}
