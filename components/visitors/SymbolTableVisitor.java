package components.visitors;

import components.helpers.SymbolTable;
import syntaxtree.*;
import visitor.GJDepthFirst;

public class SymbolTableVisitor extends GJDepthFirst<String,String> {

  private SymbolTable symbolTable;

  public SymbolTableVisitor(SymbolTable st) {
    this.symbolTable = st;
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
    this.symbolTable.insertClass(mainClassName,true);
    // Read args variable as String[] and insert it in main class
    String argsVarName = n.f11.accept(this,null);
    this.symbolTable.varDeclaration("String[]", argsVarName, mainClassName);
    // Read all variable declarations
    n.f14.accept(this,mainClassName);
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
    this.symbolTable.insertClass(className,false);
    // Read all variable declarations
    n.f3.accept(this,className);
    // Read all method declarations
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
    // Read parent class name
    String parentClassName = n.f3.accept(this,null);
    this.symbolTable.insertClass(className,parentClassName,false);
    // Read all variable declarations
    n.f5.accept(this,className);
    // Read all method declarations
    n.f6.accept(this,className);
    return null;
  }

  /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
  */
  public String visit(VarDeclaration n, String argu) throws Exception {
    String type = n.f0.accept(this,null);
    String name = n.f1.accept(this,null);
    this.symbolTable.varDeclaration(type, name, argu);
    return type + " " + name;
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
    // Insert method to last class
    this.symbolTable.methodDeclaration(returnType, name,argu);
    // Read all variable declarations
    n.f7.accept(this,argu + ":" + name);
    return returnType + " " + name;
  }

  /**
    * f0 -> Type()
    * f1 -> Identifier()
  */
  public String visit(FormalParameter n, String argu) throws Exception {
    String type = n.f0.accept(this,null);
    String name = n.f1.accept(this,null);
    this.symbolTable.formalParameterDeclaration(type, name);
    return type + " " + name;
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