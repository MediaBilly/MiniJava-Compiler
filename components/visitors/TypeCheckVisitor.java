package components.visitors;

import components.helpers.SymbolTable;
import components.helpers.Utils;
import syntaxtree.*;
import visitor.GJDepthFirst;

public class TypeCheckVisitor extends GJDepthFirst<String,String> {

  private SymbolTable symbolTable;
  private String tmpArguments; // Used for MessageSend

  public TypeCheckVisitor(SymbolTable symbolTable) {
    this.symbolTable = symbolTable;
  }

  /** f0 -> "class" 
   *  f1 -> Identifier() 
   *  f2 -> "{"  f3 -> "public" 
   *  f4 -> "static" 
   *  f5 -> "void" 
   *  f6 -> "main" 
   *  f7 -> "(" 
   *  f8 -> "String" 
   *  f9 -> "[" 
   *  f10 -> "]" 
   *  f11 -> Identifier() 
   *  f12 -> ")" 
   *  f13 -> "{" 
   *  f14 -> ( VarDeclaration() )* 
   *  f15 -> ( Statement() )* 
   *  f16 -> "}" 
   *  f17 -> "}" 
   * */
  public String visit(MainClass n, String argu) throws Exception{
    // Read class name
    String mainClassName = n.f1.accept(this,null);
    // Read all variable declarations to check declared classes
    n.f14.accept(this,mainClassName);
    // Typecheck the statements
    n.f15.accept(this,mainClassName);
    return null;
  }

  /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
  */
  public String visit(ClassDeclaration n, String argu) throws Exception{
    // Read class name
    String className = n.f1.accept(this,null);
    // Read all variable declarations to check declared classes
    n.f3.accept(this,className);
    // Typecheck to all methods
    n.f4.accept(this,className);
    return null;
  }

  /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "extends"
    * f3 -> Identifier()
    * f4 -> "{"
    * f5 -> ( VarDeclaration() )*
    * f6 -> ( MethodDeclaration() )*
    * f7 -> "}"
  */
  public String visit(ClassExtendsDeclaration n, String argu) throws Exception {
    // Read class name
    String className = n.f1.accept(this,null);
    // Read all variable declarations to check declared classes
    n.f5.accept(this,className);
    // Typecheck to all methods
    n.f6.accept(this,className);
    return null;
  }

  /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
  */
  public String visit(VarDeclaration n, String argu) throws Exception {
    String name = n.f1.accept(this,null);
    String scope = argu + ":" + name;
    this.symbolTable.checkVarDeclaration(scope);
    return null;
  }
  
  /**
    * f0 -> "public"
    * f1 -> Type()
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( FormalParameterList() )?
    * f5 -> ")"
    * f6 -> "{"
    * f7 -> ( VarDeclaration() )*
    * f8 -> ( Statement() )*
    * f9 -> "return"
    * f10 -> Expression()
    * f11 -> ";"
    * f12 -> "}"
  */
  public String visit(MethodDeclaration n, String argu) throws Exception {
    String returnType = n.f1.accept(this,null);
    String name = n.f2.accept(this,null);
    // Read all parameter declarations 
    n.f4.accept(this,argu + ":" + name);
    // Read all variable declarations
    n.f7.accept(this,argu + ":" + name);
    // Typecheck all the statements
    n.f8.accept(this,argu + ":" + name);
    // Typecheck return type
    String exprType = this.symbolTable.getExpressionType(n.f10.accept(this,argu + ":" + name), argu + ":" + name);
    if (!this.symbolTable.checkTypeMatch(exprType, returnType)) {
      throw new Exception("Return type mismatch: cannot convert from " + exprType +" to " + returnType + " in method " + argu + "." + name);
    }
    return returnType + " " + name;
  }

  /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
  public String visit(AssignmentStatement n, String argu) throws Exception {
    String identifierType = this.symbolTable.getExpressionType(n.f0.accept(this,argu), argu);
    String expr = n.f2.accept(this,argu);
    String exprType = this.symbolTable.getExpressionType(expr, argu);
    if (!this.symbolTable.checkTypeMatch(exprType, identifierType)) {
      throw new Exception("Assignment type mismatch: cannot convert from " + exprType + " to " + identifierType);
    }
    return null;
  }

  /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
  public String visit(ArrayAssignmentStatement n, String argu) throws Exception {
    // Check array type (like array lookup)
    String arrayType = this.symbolTable.getExpressionType(n.f0.accept(this,argu), argu);
    if (arrayType.equals("int[]") || arrayType.equals("boolean[]")) {
      // Check index type
      String indexType = this.symbolTable.getExpressionType(n.f2.accept(this,argu), argu);
      if (!indexType.equals("int")) {
        throw new Exception("ArrayAssignment index error: " + indexType + " cannot be converted to int");
      }
    } else {
      throw new Exception("The type of the array variable must be an array type but it resolved to " + arrayType);
    }
    // Continue like in assignment statement
    String identifierType = arrayType.equals("int[]")  ? "int" : "boolean";
    String expr = n.f5.accept(this,argu);
    String exprType = this.symbolTable.getExpressionType(expr, argu);
    if (!this.symbolTable.checkTypeMatch(exprType, identifierType)) {
      throw new Exception("Assignment type mismatch: cannot convert from " + exprType + " to " + identifierType);
    }
    return null;
  }

  /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
  public String visit(IfStatement n, String argu) throws Exception {
    // Check expression type
    String exprType = this.symbolTable.getExpressionType(n.f2.accept(this,argu), argu);
    if (!exprType.equals("boolean")) {
      throw new Exception("If statements can only be booleans. " + exprType + " given.");
    }
    // Check it's statements
    n.f4.accept(this,argu);
    n.f6.accept(this,argu);
    return null;
  }

  /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
  public String visit(WhileStatement n, String argu) throws Exception {
    // Check expression type
    String exprType = this.symbolTable.getExpressionType(n.f2.accept(this,argu), argu);
    if (!exprType.equals("boolean")) {
      throw new Exception("While statements can only be booleans. " + exprType + " given.");
    }
    // Check it's statements
    n.f4.accept(this,argu);
    return null;
  }

  /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
  public String visit(PrintStatement n, String argu) throws Exception {
    String exprType = this.symbolTable.getExpressionType(n.f2.accept(this,argu), argu);
    if (!exprType.equals("int")) {
      throw new Exception("System.out.println() statement only accepts integers. " + exprType + " given.");
    }
    return "int";
  }

  /**
    * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
  public String visit(AndExpression n, String argu) throws Exception {
    String clause1Type = this.symbolTable.getExpressionType(n.f0.accept(this, argu), argu);
    String clause2Type = this.symbolTable.getExpressionType(n.f2.accept(this, argu), argu);
    if (!clause1Type.equals("boolean") || !clause2Type.equals("boolean")) {
      throw new Exception("The operator && is undefined for the argument type(s) " + clause1Type + ", " + clause2Type);
    }
    return "boolean";
  }

  /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
  public String visit(CompareExpression n, String argu) throws Exception {
    String expr1Type = this.symbolTable.getExpressionType(n.f0.accept(this, argu), argu);
    String expr2Type = this.symbolTable.getExpressionType(n.f2.accept(this, argu), argu);
    if (!expr1Type.equals("int") || !expr2Type.equals("int")) {
      throw new Exception("The operator < is undefined for the argument type(s) " + expr1Type + ", " + expr2Type);
    }
    return "boolean";
  }

  /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
  public String visit(PlusExpression n, String argu) throws Exception {
    String expr1Type = this.symbolTable.getExpressionType(n.f0.accept(this, argu), argu);
    String expr2Type = this.symbolTable.getExpressionType(n.f2.accept(this, argu), argu);
    if (!expr1Type.equals("int") || !expr2Type.equals("int")) {
      throw new Exception("The operator + is undefined for the argument type(s) " + expr1Type + ", " + expr2Type);
    }
    return "int";
  }

  /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
  public String visit(MinusExpression n, String argu) throws Exception {
    String expr1Type = this.symbolTable.getExpressionType(n.f0.accept(this, argu), argu);
    String expr2Type = this.symbolTable.getExpressionType(n.f2.accept(this, argu), argu);
    if (!expr1Type.equals("int") || !expr2Type.equals("int")) {
      throw new Exception("The operator - is undefined for the argument type(s) " + expr1Type + ", " + expr2Type);
    }
    return "int";
  }

  /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
  public String visit(TimesExpression n, String argu) throws Exception {
    String expr1Type = this.symbolTable.getExpressionType(n.f0.accept(this, argu), argu);
    String expr2Type = this.symbolTable.getExpressionType(n.f2.accept(this, argu), argu);
    if (!expr1Type.equals("int") || !expr2Type.equals("int")) {
      throw new Exception("The operator * is undefined for the argument type(s) " + expr1Type + ", " + expr2Type);
    }
    return "int";
  }

  /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
  public String visit(ArrayLookup n, String argu) throws Exception {
    // Check array type
    String arrayType = this.symbolTable.getExpressionType(n.f0.accept(this,argu), argu);
    if (arrayType.equals("int[]") || arrayType.equals("boolean[]")) {
      // Check index type
      String indexType = this.symbolTable.getExpressionType(n.f2.accept(this,argu), argu);
      if (!indexType.equals("int")) {
        throw new Exception("ArrayLookup index error: " + indexType + " cannot be converted to int");
      }
    } else {
      throw new Exception("ArrayLookup error: Bad left operand for operator [] of type " + arrayType);
    }
    return arrayType.equals("int[]") ? "int" : "boolean";
  }

  /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
  public String visit(ArrayLength n, String argu) throws Exception {
    // Check array type
    String varName = n.f0.accept(this,argu);
    String arrayType = this.symbolTable.getExpressionType(varName, argu);
    if (!arrayType.equals("int[]") && !arrayType.equals("boolean[]")) {
      throw new Exception("The primitive type " + arrayType + " of k does not have a field length ");
    }
    return "int";
  }

  /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
  public String visit(MessageSend n, String argu) throws Exception {
    String expr = n.f0.accept(this,argu);
    String id = n.f2.accept(this,argu);
    String args = n.f4.present() ? n.f4.accept(this,argu) : null;
    return this.symbolTable.checkMessageSend(expr, id, args, argu);
  }

  /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
  public String visit(ExpressionList n, String argu) throws Exception {
    this.tmpArguments = n.f0.accept(this,argu);
    n.f1.accept(this,argu);
    return this.tmpArguments;
  }

  /**
    * f0 -> ","
    * f1 -> Expression()
    */
  public String visit(ExpressionTerm n, String argu) throws Exception {
    this.tmpArguments += n.f0.accept(this,argu) + n.f1.accept(this,argu);
    return this.tmpArguments;
  }

  /**
    * f0 -> "new"
    * f1 -> "boolean"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
  public String visit(BooleanArrayAllocationExpression n, String argu) throws Exception {
    String expr = n.f3.accept(this,argu);
    String exprType = this.symbolTable.getExpressionType(expr, argu);
    if (!exprType.equals("int")) {
      throw new Exception("IntegerArrayAllocationExpressionSizeError: '" + expr + "' is not an integer but " + exprType);
    }
    return "boolean[]";
  }

  /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
  public String visit(IntegerArrayAllocationExpression n, String argu) throws Exception {
    String expr = n.f3.accept(this,argu);
    String exprType = this.symbolTable.getExpressionType(expr, argu);
    if (!exprType.equals("int")) {
      throw new Exception("IntegerArrayAllocationExpressionSizeError: '" + expr + "' is not an integer but " + exprType);
    }
    return "int[]";
  }

  /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
  public String visit(AllocationExpression n, String argu) throws Exception {
    String identifier = n.f1.accept(this,argu);
    this.symbolTable.checkAllocation(identifier);
    return identifier;
  }

  /**
    * f0 -> "!"
    * f1 -> Clause()
    */
  public String visit(NotExpression n, String argu) throws Exception {
    String clauseType = this.symbolTable.getExpressionType(n.f1.accept(this,argu), argu);
    if (!clauseType.equals("boolean")) {
      throw new Exception("The operator ! is undefined for the argument type(s) " + clauseType);
    }
    return "boolean";
  }

  /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
  public String visit(BracketExpression n, String argu) throws Exception {
    return n.f1.accept(this, argu);
  }

  /**
    * f0 -> "boolean"
    * f1 -> "["
    * f2 -> "]"
  */
  public String visit(BooleanArrayType n, String argu) throws Exception {
    return "boolean[]";
  }

  /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
  public String visit(IntegerArrayType n, String argu) throws Exception {
    return "int[]";
  }

  public String visit(NodeToken n, String argu) throws Exception { 
    return n.toString(); 
  }
}