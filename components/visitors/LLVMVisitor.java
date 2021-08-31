package components.visitors;

import components.prototypes.*;
import components.prototypes.Class;
import components.helpers.SymbolTable;
import components.helpers.Utils;
import syntaxtree.*;
import visitor.GJDepthFirst;
import java.io.*;

public class LLVMVisitor extends GJDepthFirst<String,String> {

  private SymbolTable symbolTable;
  private FileWriter fWriter;
  private int registerCounter;
  private int ifLabelCounter;
  private int elseLabelCounter;
  private int endIfLabelCounter;
  private int whileLabelCounter;
  private int loopLabelCounter;
  private int endWhileLabelCounter;
  private int otherLabelCounter;
  private String tmpArguments;
  private String lastClassType;
  private String identiferAcceptType; // "load" or "store". it is automatically set to load when visitor finishes accept so you need to set it to store when you need to

  public LLVMVisitor(SymbolTable symbolTable,String outFileName) {
    // Initialize data members
    try {
      this.fWriter = new FileWriter(outFileName);
    } catch (IOException ex) {
      System.out.println(ex.getMessage());
    }
    // Default action for identifier accept
    this.identiferAcceptType = "load";
    this.symbolTable = symbolTable;
    this.registerCounter = this.ifLabelCounter = this.elseLabelCounter = this.endIfLabelCounter = this.whileLabelCounter = this.loopLabelCounter = this.endWhileLabelCounter = this.otherLabelCounter = 0;
    // Generate and emit LLVM vtables
    for (int clIndex = 0;clIndex < this.symbolTable.getClassList().size();clIndex++) {
      Class cl = this.symbolTable.getClassList().get(clIndex);
      String vtableLLVM = "@." + cl.getName() + "_vtable = global [" + Integer.toString(cl.getVtable().size()) + " x i8*] [";
      for (int methodIndex = 0;methodIndex < cl.getVtable().size();methodIndex++) {
        Method method = cl.getVtable().get(methodIndex);
        if (methodIndex > 0) {
          vtableLLVM += ",";
        }
        vtableLLVM += "i8* bitcast (" + Utils.llvmType(method.getReturnType()) + " (i8*";
        if (method.argc() > 0) {
          vtableLLVM += ",";
        }
        for (int i = 0;i < method.argc();i++) {
          if (i > 0) {
            vtableLLVM += ",";
          }
          vtableLLVM += Utils.llvmType(method.getNthArgument(i).getType());
        }
        vtableLLVM += ")* @" + method.getOwnClass().getName() + "." + method.getName() + " to i8*)";
      }
      vtableLLVM += "]";
      emit(vtableLLVM);
    }
    emit("\n");
    // Write helper methods to output .ll
    emit("declare i8* @calloc(i32, i32)");
    emit("declare i32 @printf(i8*, ...)");
    emit("declare void @exit(i32)\n");
    emit("@_cint = constant [4 x i8] c\"%d\\0a\\00\"");
    emit("@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"");
    emit("@_cNSZ = constant [15 x i8] c\"Negative size\\0a\\00\"");
    emit("define void @print_int(i32 %i) {");
    emit("\t%_str = bitcast [4 x i8]* @_cint to i8*");
    emit("\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)");
    emit("\tret void");
    emit("}\n");
    emit("define void @throw_oob() {");
    emit("\t%_str = bitcast [15 x i8]* @_cOOB to i8*");
    emit("\tcall i32 (i8*, ...) @printf(i8* %_str)");
    emit("\tcall void @exit(i32 1)");
    emit("\tret void");
    emit("}\n");
    emit("define void @throw_nsz() {");
    emit("\t%_str = bitcast [15 x i8]* @_cNSZ to i8*");
    emit("\tcall i32 (i8*, ...) @printf(i8* %_str)");
    emit("\tcall void @exit(i32 1)");
    emit("\tret void");
    emit("}\n");
  }

  private void emit(String code) {
    try {
      this.fWriter.write(code + "\n");
    } catch (IOException ex) {
      System.out.println(ex.getMessage());
    }
  }

  private String new_temp() {
    String reg = "%_" + this.registerCounter;
    this.registerCounter++;
    return reg;
  }

  private String new_label(String type) {
    String label;
    switch (type) {
      case "if":
        label = "if_then_" + this.ifLabelCounter;
        this.ifLabelCounter++;
        break;
      case "else":
        label = "if_else_" + this.elseLabelCounter;
        this.elseLabelCounter++;
        break;
      case "endif":
        label = "if_end_" + this.endIfLabelCounter;
        this.endIfLabelCounter++;
        break;
      case "while":
        label = "while_" + this.whileLabelCounter;
        this.whileLabelCounter++;
        break;
      case "loop":
        label = "loop_" + this.loopLabelCounter;
        this.loopLabelCounter++;
        break;
      case "endwhile":
        label = "end_while_" + this.endWhileLabelCounter;
        this.endWhileLabelCounter++;
        break;
      default:
        label = "lb_" + this.otherLabelCounter;
        this.otherLabelCounter++;
        break;
    }
    return label;
  }

  /**
    * f0 -> MainClass()
    * f1 -> ( TypeDeclaration() )*
    * f2 -> <EOF>
    */
  public String visit(Goal n, String argu) throws Exception {
    n.f0.accept(this,argu);
    n.f1.accept(this,argu);
    this.fWriter.close();
    return null;
  }

  /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> "public"
    * f4 -> "static"
    * f5 -> "void"
    * f6 -> "main"
    * f7 -> "("
    * f8 -> "String"
    * f9 -> "["
    * f10 -> "]"
    * f11 -> Identifier()
    * f12 -> ")"
    * f13 -> "{"
    * f14 -> ( VarDeclaration() )*
    * f15 -> ( Statement() )*
    * f16 -> "}"
    * f17 -> "}"
    */
  public String visit(MainClass n, String argu) throws Exception {
    emit("define i32 @main() {");
    String className = n.f1.accept(this,"name_only");
    // Emit var declarations
    Class mainClass = this.symbolTable.getClass(className);
    for (int i = 0;i < mainClass.getVariablesList().size();i++) {
      Variable var = mainClass.getVariablesList().get(i);
      // Ignore String[] args cause this type is not currently supported by minijava
      if (var.getType() != "String[]") {
        emit("\t%" + var.getName() + " = alloca " + Utils.llvmType(var.getType()));
      }
    }
    // Accept statements
    n.f15.accept(this,className);
    emit("\tret i32 0\n}\n");
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
  public String visit(ClassDeclaration n, String argu) throws Exception {
    String className = n.f1.accept(this,"name_only");
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
    String className = n.f1.accept(this,"name_only");
    n.f6.accept(this,className);
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
    // Get method's class from arguments
    Class cl = this.symbolTable.getClass(argu);
    // Get method
    String methodName = n.f2.accept(this,"name_only");
    Method method = cl.getMethod(methodName);
    // Emit method declaration
    String methodArgs = "";
    for (int i = 0;i < method.argc();i++) {
      Variable arg = method.getNthArgument(i);
      methodArgs += ", " + Utils.llvmType(arg.getType()) + " %." + arg.getName();
    }
    emit("define " + Utils.llvmType(method.getReturnType()) + " @" + cl.getName() + "." + method.getName() + "(i8* %this" + methodArgs + ") {");
    // Emit statements for arguments local memory allocation
    for (int i = 0;i < method.argc();i++) {
      Variable arg = method.getNthArgument(i);
      emit("\t%" + arg.getName() + " = " + "alloca " + Utils.llvmType(arg.getType()));
      emit("\tstore " + Utils.llvmType(arg.getType()) + " %." + arg.getName() + ", " + Utils.llvmType(arg.getType()) + "* %" + arg.getName());
    }
    // Emit var declarations
    for (int i = 0;i < method.getVariablesList().size();i++) {
      Variable var = method.getVariablesList().get(i);
      emit("\t%" + var.getName() + " = alloca " + Utils.llvmType(var.getType()));
    }
    // Declare statements
    n.f8.accept(this,argu + ":" + methodName);
    // Return statement 
    String retExpr = n.f10.accept(this,argu + ":" + methodName);
    emit("\tret " + Utils.llvmType(method.getReturnType()) + " " + retExpr);
    emit("}\n");
    return null;
  }

  /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
  public String visit(AssignmentStatement n, String argu) throws Exception {
    String id = n.f0.accept(this,"name_only");
    String[] args = argu.split(":");
    // Load and get variable specified by the Identifier depending on the scope
    Variable idVar;
    // Scope is method
    if (args.length == 2) {
      // Try to get variable from method's scope
      idVar = this.symbolTable.getClass(args[0]).getMethod(args[1]).getVariable(id);
      if (idVar == null) {
        idVar = this.symbolTable.getClass(args[0]).getVariable(id);
      }
    } else {
      // Scope is class(field)
      idVar = this.symbolTable.getClass(args[0]).getVariable(id);
    }
    // Generate expression(accept returns either a register that contains the result of the generated expression or a literal(integer or 0 for false or 1 for true))
    String expr = n.f2.accept(this,argu);
    // Get address of left operand to store to
    this.identiferAcceptType = "store";
    String idAddr = n.f0.accept(this,argu);
    emit("\tstore " + Utils.llvmType(idVar.getType()) + " " + expr + ", " + Utils.llvmType(idVar.getType()) + "* " + idAddr);
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
    // Create the labels
    String oobLabel = new_label("if");
    String nonOobLabel = new_label("endif"); 
    String[] args = argu.split(":");
    String arrayName = n.f0.accept(this,"name_only");
    // Get array variable specified by the Identifier depending on the scope (needed to get it's type)
    Variable array;
    // Scope is method
    if (args.length == 2) {
      // Try to get variable from method's scope
      array = this.symbolTable.getClass(args[0]).getMethod(args[1]).getVariable(arrayName);
      if (array == null) {
        array = this.symbolTable.getClass(args[0]).getVariable(arrayName);
      }
    } else {
      // Scope is class(field)
      array = this.symbolTable.getClass(args[0]).getVariable(arrayName);
    }
    // Load the address of the array
    String address = n.f0.accept(this,argu);
    // Bitcast the array address to i32* if the array is of type boolean[] and load the size of the array
    String size;
    if (array.getType() == "boolean[]") {
      String bitcastedAddress = new_temp();
      emit("\t" + bitcastedAddress + " = bitcast i8* " + address + " to i32*");
      size = new_temp();
      emit("\t" + size + " = load i32, i32* " + bitcastedAddress);
    } else {
      size = new_temp();
      emit("\t" + size + " = load i32, i32* " + address);
    }
    // Get array index
    String index = n.f2.accept(this,argu);
    // Check that the index is greater than zero
    String comparison1 = new_temp();
    emit("\t" + comparison1 + " = icmp sge i32 " + index + ", 0");
    // Check that the index is less than the size of the array
    String comparison2 = new_temp();
    emit("\t" + comparison2 + " = icmp slt i32 " + index + ", " + size);
    // Check that both of theese conditions hold
    String comparison = new_temp();
    emit("\t" + comparison + " = and i1 " + comparison1 + ", " + comparison2);
    emit("\tbr i1 " + comparison + ", label %" + nonOobLabel + ", label %" + oobLabel);
    emit("\t" + oobLabel + ":");
    emit("\tcall void @throw_oob()");
    emit("\tbr label %" + nonOobLabel);
    emit("\t" + nonOobLabel + ":");
    // Add 1 or 4 to the index depending on the array's type to ignore the size
    String finalIndex = new_temp();
    if (array.getType() == "boolean[]") {
      emit("\t" + finalIndex + " = add i32 4, " + index);
    } else {
      emit("\t" + finalIndex + " = add i32 1, " + index);
    }
    // Get pointer to the i + 1 (or i + 4 if type is boolean[]) element of the array 
    String pointer = new_temp();
    if (array.getType() == "boolean[]") {
      emit("\t" + pointer + " = getelementptr i8, i8* " + address + ", i32 " + finalIndex);
    } else {
      emit("\t" + pointer + " = getelementptr i32, i32* " + address + ", i32 " + finalIndex);
    }
    // Get right operand
    String rOperand = n.f5.accept(this,argu);
    // Store right operand's result to the array
    if (array.getType() == "boolean[]") {
      // Zero extend right operand from i1 to i8
      String finalRoperand = new_temp();
      emit("\t" + finalRoperand + " = zext i1 " + rOperand + " to i8");
      emit("\tstore i8 " + finalRoperand + ", i8* " + pointer);
    } else {
      emit("\tstore i32 " + rOperand + ", i32* " + pointer);
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
    String condition = n.f2.accept(this,argu);
    String ifLabel = new_label("if");
    String elseLabel = new_label("else");
    String endLabel = new_label("endif");
    emit("\tbr i1 " + condition + ", label %" + ifLabel + ", label %" + elseLabel);
    emit("\t" + ifLabel + ":");
    n.f4.accept(this,argu);
    emit("\tbr label %" + endLabel);
    emit("\t" + elseLabel + ":");
    n.f6.accept(this,argu);
    emit("\tbr label %" + endLabel);
    emit("\t" + endLabel + ":");
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
    String loopLabel = new_label("while");
    String loopBodyLabel = new_label("loop");
    String endLabel = new_label("endwhile");
    emit("\tbr label %" + loopLabel);
    emit("\t" + loopLabel + ":");
    String condition = n.f2.accept(this,argu);
    emit("\tbr i1 " + condition + ", label %" + loopBodyLabel + ", label %" + endLabel);
    emit("\t" + loopBodyLabel + ":");
    n.f4.accept(this,argu);
    emit("\tbr label %" + loopLabel);
    emit("\t" + endLabel + ":");
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
    String expr = n.f2.accept(this,argu);
    emit("\tcall void (i32) @print_int(i32 " + expr + ")");
    return null;
  }

  /**
    * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
  public String visit(AndExpression n, String argu) throws Exception {
    // Generate expr1
    String expr1 = n.f0.accept(this, argu);
    String label1 = new_label("and");
    String label2 = new_label("and");
    String label3 = new_label("and");
    emit("\tbr i1 " + expr1 + ", label %" + label2 + ", label %" + label1);
    emit("\t" + label1 + ":");
    emit("\tbr label %" + label3);
    emit("\t" + label2 + ":");
    String expr2 = n.f2.accept(this,argu);
    emit("\tbr label %" + label3);
    emit("\t" + label3 + ":");
    String result = new_temp();
    emit("\t" + result + " = phi i1 [0, %" + label1 + "], [" + expr2 + ", %" + label2 + "]");
    return result;
  }

  /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
  public String visit(CompareExpression n, String argu) throws Exception {
    String expr1 = n.f0.accept(this,argu);
    String expr2 = n.f2.accept(this,argu);
    String result = new_temp();
    emit("\t" + result + " = icmp slt i32 " + expr1 + ", " + expr2);
    return result;
  }

  /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
  public String visit(PlusExpression n, String argu) throws Exception {
    String expr1 = n.f0.accept(this,argu);
    String expr2 = n.f2.accept(this,argu);
    String result = new_temp();
    emit("\t" + result + " = add i32 " + expr1 + ", " + expr2);
    return result;
  }

  /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
  public String visit(MinusExpression n, String argu) throws Exception {
    String expr1 = n.f0.accept(this,argu);
    String expr2 = n.f2.accept(this,argu);
    String result = new_temp();
    emit("\t" + result + " = sub i32 " + expr1 + ", " + expr2);
    return result;
  }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
  public String visit(TimesExpression n, String argu) throws Exception {
    String expr1 = n.f0.accept(this,argu);
    String expr2 = n.f2.accept(this,argu);
    String result = new_temp();
    emit("\t" + result + " = mul i32 " + expr1 + ", " + expr2);
    return result;
  }

  /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
  public String visit(ArrayLookup n, String argu) throws Exception {
    // Create the labels
    String oobLabel = new_label("if");
    String nonOobLabel = new_label("endif"); 
    // Load the address of the array
    String address = n.f0.accept(this,argu);
    // Bitcast the array address to i32* if the array is of type boolean[] and load the size of the array
    String size;
    String arrType = this.lastClassType;
    if (arrType == "boolean[]") {
      String bitcastedAddress = new_temp();
      emit("\t" + bitcastedAddress + " = bitcast i8* " + address + " to i32*");
      size = new_temp();
      emit("\t" + size + " = load i32, i32* " + bitcastedAddress);
    } else {
      size = new_temp();
      emit("\t" + size + " = load i32, i32* " + address);
    }
    // Get array index
    String index = n.f2.accept(this,argu);
    // Check that the index is greater than zero
    String comparison1 = new_temp();
    emit("\t" + comparison1 + " = icmp sge i32 " + index + ", 0");
    // Check that the index is less than the size of the array
    String comparison2 = new_temp();
    emit("\t" + comparison2 + " = icmp slt i32 " + index + ", " + size);
    // Check that both of theese conditions hold
    String comparison = new_temp();
    emit("\t" + comparison + " = and i1 " + comparison1 + ", " + comparison2);
    emit("\tbr i1 " + comparison + ", label %" + nonOobLabel + ", label %" + oobLabel);
    emit("\t" + oobLabel + ":");
    emit("\tcall void @throw_oob()");
    emit("\tbr label %" + nonOobLabel);
    emit("\t" + nonOobLabel + ":");
    // Add 1 or 4 to the index depending on the array's type to ignore the size
    String finalIndex = new_temp();
    if (arrType == "boolean[]") {
      emit("\t" + finalIndex + " = add i32 4, " + index);
    } else {
      emit("\t" + finalIndex + " = add i32 1, " + index);
    }
    // Get pointer to the i + 1 (or i + 4 if type is boolean[]) element of the array 
    String pointer = new_temp();
    String result = new_temp();
    // Load value from array
    if (arrType == "boolean[]") {
      emit("\t" + pointer + " = getelementptr i8, i8* " + address + ", i32 " + finalIndex);
      emit("\t" + result + " = load i8, i8* " + pointer);
      // Truncate i8 result to i1 for usage as a boolean variable to logical statements
      String finalResult = new_temp();
      emit("\t" + finalResult + " = trunc i8 " + result + " to i1");
      result = finalResult;
    } else {
      emit("\t" + pointer + " = getelementptr i32, i32* " + address + ", i32 " + finalIndex);
      emit("\t" + result + " = load i32, i32* " + pointer);
    }
    return result;
  }

  /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
  public String visit(ArrayLength n, String argu) throws Exception {
    // Load the address of the array
    String address = n.f0.accept(this,argu);
    // Bitcast the array address to i32* if the array is of type boolean[]
    if (this.lastClassType == "boolean[]") {
      String bitcastedAddress = new_temp();
      emit("\t" + bitcastedAddress + " = bitcast i8* " + address + " to i32*");
      address = bitcastedAddress;
    } 
    // Load the size of the array
    String size = new_temp();
    emit("\t" + size + " = load i32, i32* " + address);
    return size;
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
    // Load the object's pointer
    String objPtr = n.f0.accept(this,argu);
    Class classCalled = this.symbolTable.getClass(this.lastClassType);
    // Do the required bitcasts, so that we can access the vtable pointer
    String bitcastedPtr = new_temp();
    emit("\t" + bitcastedPtr + " = bitcast i8* " + objPtr + " to i8***");
    // Load vtable pointer
    String vtablePtr = new_temp();
    emit("\t" + vtablePtr + " = load i8**, i8*** " + bitcastedPtr);
    // Get the called method
    String methodName = n.f2.accept(this,"name_only");
    Method calledMethod = classCalled.getMethod(methodName);
    if (calledMethod == null) {
      calledMethod = classCalled.getMethodRecursively(methodName);
    }
    // Get pointer to the called method from the vtable
    String vtableEntryPtr = new_temp();
    emit("\t" + vtableEntryPtr + " = getelementptr i8*, i8** " + vtablePtr + ", i32 " + calledMethod.getOffset()/8);
    // Get the actual function pointer
    String funcPtr = new_temp();
    emit("\t" + funcPtr + " = load i8*, i8** " + vtableEntryPtr);
    // Generate argument types list
    String argumentTypesList = "";
    for (int i = 0;i < calledMethod.argc();i++) {
      argumentTypesList += "," + Utils.llvmType(calledMethod.getNthArgument(i).getType());
    }
    // Cast the function pointer from i8* to a function ptr type that matches its signature.
    String castedFuncPtr = new_temp();
    emit("\t" + castedFuncPtr + " = bitcast i8* " + funcPtr + " to " + Utils.llvmType(calledMethod.getReturnType()) + " (i8*" + argumentTypesList + ")*");
    // Generate called arguments
    String llvmCalledArguments = "";
    if (n.f4.present()) {
      String[] calledArguments = n.f4.accept(this,argu).split(",");
      for(int i = 0;i < calledMethod.argc();i++) {
        llvmCalledArguments += ", " + Utils.llvmType(calledMethod.getNthArgument(i).getType()) + " " + calledArguments[i];
      }
    }
    // Perform the call - note the first argument is the receiver object.
    String retValue = new_temp();
    emit("\t" + retValue + " = call " + Utils.llvmType(calledMethod.getReturnType()) + " " + castedFuncPtr + "(i8* " + objPtr + llvmCalledArguments + ")");
    this.lastClassType = calledMethod.getReturnType();
    return retValue;
  }

  /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
  public String visit(ExpressionList n, String argu) throws Exception {
    this.tmpArguments = n.f0.accept(this,argu);
    n.f1.accept(this, argu);
    return this.tmpArguments;
  }

  /**
    * f0 -> ","
    * f1 -> Expression()
    */
  public String visit(ExpressionTerm n, String argu) throws Exception {
    this.tmpArguments += n.f0.accept(this,argu) + n.f1.accept(this,argu);
    return null;
  }

  /**
    * f0 -> "true"
    */
  public String visit(TrueLiteral n, String argu) throws Exception {
    return "1";
  }

   /**
    * f0 -> "false"
    */
  public String visit(FalseLiteral n, String argu) throws Exception {
    return "0";
  }

  /**
    * f0 -> <IDENTIFIER>
    */
  public String visit(Identifier n, String argu) throws Exception {
    String id = n.f0.accept(this,argu);
    if (argu.equals("name_only")) {
      return id;
    }
    String[] args = argu.split(":");
    Variable idVar;
    boolean scopeIsClass = false;
    // Get variable specified by the Identifier depending on the scope
    if (args.length == 2) {
      // Scope is inside method
      idVar = this.symbolTable.getClass(args[0]).getMethod(args[1]).getVariable(id);
      if (idVar == null) {
        // Not found in method's scope so search the classe's scope
        idVar = this.symbolTable.getClass(args[0]).getVariable(id);
        scopeIsClass = true;
      }
    } else {
      // Scope is inside class (field)
      idVar = this.symbolTable.getClass(args[0]).getVariable(id);
      scopeIsClass = true;
    }
    this.lastClassType = idVar.getType();
    // If classe's scope is main class then work as it is method cause main class doesn't have any fields or other functions but only the main function
    // and so all the variables are in main function's scope
    if (scopeIsClass && this.symbolTable.getClass(args[0]).isMainClass()) {
      scopeIsClass = false;
    }
    // If variable's scope is inside class we need to get it's address 
    String varAddress;
    if (scopeIsClass) {
      String pointer = new_temp();
      // Get a pointer to the variable's field of this aka &this->varName
      emit("\t" + pointer + " = getelementptr i8, i8* %this, i32 " + (idVar.getOffset() + 8));
      // Perform the necessary bitcasts
      String bitcastedPtr = new_temp();
      emit("\t" + bitcastedPtr + " = bitcast i8* " + pointer + " to " + Utils.llvmType(idVar.getType()) + "*");
      varAddress = bitcastedPtr;
    } else {
      // If variable's scope is inside method it's address is in the form %varName because they are allocated with alloca function
      varAddress = "%" + idVar.getName();
    }
    // If we want to load this variable somewhere load and return it here in a register
    if (this.identiferAcceptType.equals("load")) {
      String r = this.new_temp();
      emit("\t" + r + " = load " + Utils.llvmType(idVar.getType()) + ", " + Utils.llvmType(idVar.getType()) + "* " + varAddress);
      return r;
    } else {
      // Reset default identifier accept type
      this.identiferAcceptType = "load";
      // If we want to store to it somewhere else just return it's address
      return varAddress;
    }
  }

  /**
    * f0 -> "this"
    */
  public String visit(ThisExpression n, String argu) throws Exception {
    this.lastClassType = argu.split(":")[0];
    return "%this";
  }

  /**
    * f0 -> "new"
    * f1 -> "boolean"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
  public String visit(BooleanArrayAllocationExpression n, String argu) throws Exception {
    // Create labels
    String negSizeLabel = new_label("if");
    String nonNegativeSizeLabel = new_label("endif");
    // Get array's declared size
    String arraySize = n.f3.accept(this,argu);
    // Here the size needs 4 extra bytes to be stored because this is boolean array(aka i8*)
    // Create a register to store the final size (array's size + 4 extra bytes to store array's size)
    String finalSize = new_temp();
    emit("\t" + finalSize + " = add i32 4, " + arraySize);
    // Check that the size of the array is >= 4 (because we added 4)
    String sizeCheckResult = new_temp();
    emit("\t" + sizeCheckResult + " = icmp sge i32 " + finalSize + ", 4");
    // Negative size detected
    emit("\tbr i1 " + sizeCheckResult + ", label %" + nonNegativeSizeLabel + ", label %" + negSizeLabel);
    emit("\t" + negSizeLabel + ":");
    emit("\tcall void @throw_nsz()");
    // Positive size
    emit("\tbr label %" + nonNegativeSizeLabel);
    emit("\t" + nonNegativeSizeLabel + ":");
    // Allocate menory for the array
    String arrayPointer = new_temp();
    emit("\t" + arrayPointer + " = call i8* @calloc(i32 " + finalSize + ", i32 1)");
    // Cast the returned array pointer to i32* (integer type) to store the size
    String castedPointer= new_temp();
    emit("\t" + castedPointer + " = bitcast i8* " + arrayPointer + " to i32*");
    // Store the size of the array to the first position
    emit("\tstore i32 " + arraySize + ", i32* " + castedPointer);
    this.lastClassType = "boolean[]";
    return arrayPointer;
  }

   /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
  public String visit(IntegerArrayAllocationExpression n, String argu) throws Exception {
    // Create labels
    String negSizeLabel = new_label("if");
    String nonNegativeSizeLabel = new_label("endif");
    // Get array's declared size
    String arraySize = n.f3.accept(this,argu);
    // Create a register to store the final size (array's size + 1 integer to store array's size)
    String finalSize = new_temp();
    emit("\t" + finalSize + " = add i32 1, " + arraySize);
    // Check that the size of the array is >= 1 (because we added 1)
    String sizeCheckResult = new_temp();
    emit("\t" + sizeCheckResult + " = icmp sge i32 " + finalSize + ", 1");
    // Negative size detected
    emit("\tbr i1 " + sizeCheckResult + ", label %" + nonNegativeSizeLabel + ", label %" + negSizeLabel);
    emit("\t" + negSizeLabel + ":");
    emit("\tcall void @throw_nsz()");
    // Positive size
    emit("\tbr label %" + nonNegativeSizeLabel);
    emit("\t" + nonNegativeSizeLabel + ":");
    // Allocate menory for the array
    String arrayPointer = new_temp();
    emit("\t" + arrayPointer + " = call i8* @calloc(i32 " + finalSize + ", i32 4)");
    // Cast the returned array pointer
    String castedPointer= new_temp();
    emit("\t" + castedPointer + " = bitcast i8* " + arrayPointer + " to i32*");
    // Store the size of the array to the first position
    emit("\tstore i32 " + arraySize + ", i32* " + castedPointer);
    this.lastClassType = "int[]";
    return castedPointer;
  }

  /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
  public String visit(AllocationExpression n, String argu) throws Exception {
    // Get identifier class type
    Class classType = this.symbolTable.getClass(n.f1.accept(this,"name_only"));
    // Allocate memory on heap for the object
    String objPtr = new_temp();
    emit("\t" + objPtr + " = call i8* @calloc(i32 1, i32 " + classType.getSize() + ")");
    // Set vtable pointer
    // Bitcast the pointer to i8*** to setup the vtable
    String bitcastedPtr = new_temp();
    emit("\t" + bitcastedPtr + " = bitcast i8* " + objPtr + " to i8***");
    // Get the address of the vtable (the first element of the Base_vtable)
    String vtableAddr = new_temp();
    emit("\t" + vtableAddr + " = getelementptr [" + classType.getVtable().size() + " x i8*], [" + classType.getVtable().size() + " x i8*]* @." + classType.getName() + "_vtable, i32 0, i32 0");
    // Set the vtable to the correct address (object's bitcasted addresse's first element).
    emit("\tstore i8** " + vtableAddr + ", i8*** " + bitcastedPtr);
    this.lastClassType = classType.getName();
    return objPtr;
  }
  
  /**
    * f0 -> "!"
    * f1 -> Clause()
    */
  public String visit(NotExpression n, String argu) throws Exception {
    String expr = n.f1.accept(this,argu);
    String result = new_temp();
    emit("\t" + result + " = xor i1 1, " + expr);
    return result;
  }

  /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
  public String visit(BracketExpression n, String argu) throws Exception {
    return n.f1.accept(this, argu);
  }

  public String visit(NodeToken n, String argu) throws Exception { 
    return n.toString(); 
  }
}