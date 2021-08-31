package components.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import components.prototypes.Class;
import components.prototypes.Method;
import components.prototypes.Variable;

public class SymbolTable {
  private Map<String,Class> classes;
  private ArrayList<Class> classesList; // To print classes in declared order
  private ArrayList<Variable> lastParameList;

  public SymbolTable() {
    this.classes = new HashMap<String,Class>();
    this.classesList = new ArrayList<Class>();
    this.lastParameList = new ArrayList<Variable>();
  }

  public Class getClass(String className) {
    return this.classes.get(className);
  }

  public ArrayList<Class> getClassList() {
    return this.classesList;
  }

  // Inserts single class
  public void insertClass(String className,boolean isMainClass) throws Exception{
    // Check if new class was previously defined
    if (!this.classes.containsKey(className)) {
      Class newClass = new Class(className,isMainClass);
      this.classes.put(className, newClass);
      this.classesList.add(newClass);
    } else {
      throw new Exception("The type " + className + " is already defined");
    }
  }

  // Inserts inherited class
  public void insertClass(String className,String parentClassName,boolean isMainClass) throws Exception {
    // Check if parent class was previously declared
    if (this.classes.containsKey(parentClassName)) {
      Class parentClass = this.classes.get(parentClassName);
      // Check if new class was previously defined
      if (!this.classes.containsKey(className)) {
        Class newClass = new Class(className,parentClass,isMainClass);
        this.classes.put(className, newClass);
        this.classesList.add(newClass);
      } else {
        throw new Exception("The type " + className + " is already defined");
      }
    } else {
      // Implicit declaration of parent class
      throw new Exception(parentClassName + " cannot be resolved to a type");
    }
  }

  // Inserts variable in last declared class
  public void varDeclaration(String type,String name,String scope) throws Exception {
    String[] scopeArgs = scope.split(":");
    // Get scope type
    if (scopeArgs.length == 2) {
      // Method
      String className = scopeArgs[0];
      String methodName = scopeArgs[1];
      if (this.classes.containsKey(className)) {
        Class classObj = this.classes.get(className);
        Method method = classObj.getMethod(methodName);
        if (method != null) {
          method.insertVariable(new Variable(type, name));
        } 
      } 
    } else if (scopeArgs.length == 1) {
      // Class
      String className = scopeArgs[0];
      if (this.classes.containsKey(className)) {
        this.classes.get(className).insertVariable(type, name);
      } 
    } 
  }

  // Insert method in last declared class
  public void methodDeclaration(String returnType,String name,String className) throws Exception{
    // Get class obj from name
    if (this.classes.containsKey(className)) {
      Class classObj = this.classes.get(className);
      classObj.insertMethod(returnType, name, this.lastParameList);
      this.lastParameList.clear();
    } 
  }

  public void formalParameterDeclaration(String type,String name) {
    this.lastParameList.add(new Variable(type, name));
  }

  public void checkVarDeclaration(String scope) throws Exception {
    String[] scopeArgs = scope.split(":");
    Variable var;
    if (scopeArgs.length == 3) {
      // Variable's scope is inside method(local variable)
      String className = scopeArgs[0];
      String methodName = scopeArgs[1];
      String variableName = scopeArgs[2];
      var = this.classes.get(className).getMethod(methodName).getVariable(variableName);
    } else if (scopeArgs.length == 2) {
      // Variable's scope is inside class(field)
      String className = scopeArgs[0];
      String fieldName = scopeArgs[1];
      var = this.classes.get(className).getVariable(fieldName);
    } else {
      return;
    }
    // Class type not defined
    if (!Utils.isPrimitiveType(var.getType()) && !this.classes.containsKey(var.getType())) {
      throw new Exception(scope + " cannot be resolved to a type");
    }
  }

  // Return values:int,boolean,int[],boolean[],<class name>,null
  public String getExpressionType(String expr,String scope) throws Exception {
    // Primitive or class type
    if (Utils.isPrimitiveType(expr) || this.classes.containsKey(expr)) {
      return expr;
    }
    // true or false is boolean
    if (expr.equals("true") || expr.equals("false")) {
      return "boolean";
    }
    // Numbers are int
    if (Utils.isNumericType(expr)) {
      return "int";
    }
    // this literal is scope's class type
    if (expr.equals("this")) {
      String scopeClass = scope.split(":")[0];
      return scopeClass;
    }
    // If expression is identifier(variable) search the symbol table to find it's type
    String scopeArgs[] = scope.split(":");
    Variable var = null;
    String scopeClass = scopeArgs[0];
    if (scopeArgs.length == 2) {
      // Expression's scope is inside method
      String scopeMethod = scopeArgs[1]; 
      // Search method first
      if (this.classes.containsKey(scopeClass)) {
        var = this.classes.get(scopeClass).getMethod(scopeMethod).getVariable(expr);
      }
    }
    // Otherwise search class and it's parent classes (Class::getVariable(String) does that)
    if (var == null) {
      var = this.classes.containsKey(scopeClass) ? this.classes.get(scopeClass).getVariable(expr) : null;
    }
    if (var == null) {
      throw new Exception(expr + " field in scope " + scope + " cannot be resolved to a variable");
    }
    return var.getType();
  }

  // type1: male (sender) type2:female (receiver)
  public boolean checkTypeMatch(String type1,String type2) {
    if (!Utils.isPrimitiveType(type1)) {
      Class type1Class = this.classes.get(type1);
      return type1Class.checkType(type2);
    } else {
      return type1.equals(type2);
    }
  }

  public String checkMessageSend(String obj,String methodName,String args,String scope) throws Exception{
    String objType = this.getExpressionType(obj, scope);
    // Check if obj is class type
    Method method;
    if (!Utils.isPrimitiveType(objType)) {
      Class objClass = this.classes.get(objType);
      // Check if obj class has the wanted method
      method = objClass.getMethod(methodName);
      if (method == null) {
        method = objClass.getMethodRecursively(methodName);
      }
      if (method != null) {
        // Check if # of arguments in both methods is the same
        String[] argsArr = args != null ? args.split(",") : new String[0];
        if (argsArr.length == method.argc()) {
          // Typecheck method's arguments
          for (int i = 0;i < method.argc();i++) {
            if (!this.checkTypeMatch(this.getExpressionType(argsArr[i], scope), method.getNthArgument(i).getType())) {
              throw new Exception("Call arguments in method call " + obj + "." + methodName + " in scope " + scope + " do not match it's declared arguments");
            }
          }
        } else {
          throw new Exception("Call arguments in method call " + obj + "." + methodName + " in scope " + scope + " do not match it's declared arguments");
        }
      } else {
        throw new Exception(obj + " does not have any method with name " + methodName);
      }
    } else {
      throw new Exception(objType + " type does not have any properties or methods");
    }
    return method.getReturnType();
  }

  public void checkAllocation(String identifier) throws Exception {
    if (!this.classes.containsKey(identifier)) {
      throw new Exception(identifier + " cannot be resolved to a type");
    }
  }

  public void printOffsetTables() {
    this.classesList.forEach(cl -> {
      if (!cl.isMainClass()) {
        cl.printOffsetTable();
      }
    });
  }
}