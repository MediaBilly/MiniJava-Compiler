package components.prototypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import components.helpers.Utils;

public class Class {
  private Class parentClass;
  private String name;
  private ArrayList<Variable> variables;
  private Map<String,Variable> variablesMap;
  private ArrayList<Method> methods;
  private Map<String,Method> methodsMap;
  private ArrayList<Method> vtable;
  private boolean isMain;
  protected int varOffset;

  public Class(String name,boolean isMainClass) {
    this.name = name;
    this.parentClass = null;
    this.variables = new ArrayList<Variable>();
    this.variablesMap = new HashMap<String,Variable>();
    this.methods = new ArrayList<Method>();
    this.methodsMap = new HashMap<String,Method>();
    this.vtable = new ArrayList<Method>();
    this.isMain = isMainClass;
    this.varOffset = 0;
  }

  public Class(String name,Class parentClass,boolean isMainClass) {
    this(name,isMainClass);
    this.parentClass = parentClass;
    this.varOffset = this.parentClass.isMain ? 0 : this.parentClass.varOffset;
    this.vtable = (ArrayList<Method>)this.parentClass.getVtable().clone();
  }

  public boolean isMainClass() {
    return this.isMain;
  }

  public String getName() {
    return this.name;
  }

  public Class getParentClass() {
    return this.parentClass;
  }

  public ArrayList<Variable> getVariablesList() {
    return this.variables;
  }

  public ArrayList<Method> getMethodsList() {
    return this.methods;
  }

  public ArrayList<Method> getVtable() {
    return this.vtable;
  }

  // Returns the size of an object of this type(considering vtable pointer which is +8 bytes)
  public int getSize() {
    return this.varOffset + 8;
  }

  public void insertVariable(String type,String name) throws Exception {
    // Check if a variable with the same name was already declared in this class
    if (!this.variablesMap.containsKey(name)) {
      Variable newVariable = new Variable(type, name,this.varOffset);
      this.variables.add(newVariable);
      this.variablesMap.put(name, newVariable);
      this.varOffset += Utils.getTypeSize(newVariable.getType());
    } else {
      throw new Exception("Duplicate field " + this.name + "." + name);
    }
  }

  public int getVariableOffset(String varName) {
    if (!this.variablesMap.containsKey(varName)) {
      return this.parentClass != null ? this.parentClass.getVariableOffset(varName) : null;
    } else {
      return this.variablesMap.get(varName).getOffset();
    }
  }

  protected boolean hasMethod(String name) {
    return this.methodsMap.containsKey(name);
  }

  // Search for a method with a specific name on parent classes and also this class
  public Method getMethodRecursively(String name) {
    Method method;
    // Search the parent class if exists
    if (this.parentClass != null && (method = this.parentClass.getMethodRecursively(name)) != null) {
      return method;
    }
    // Not found so search the current class
    return this.methodsMap.containsKey(name) ? this.methodsMap.get(name) : null;
  }

  public Method getMethod(String name) {
    return this.methodsMap.containsKey(name) ? this.methodsMap.get(name) : null;
  }

  private void addNewMethod(String returnType, String name,ArrayList<Variable> args,Method parentMethod) throws Exception {
    // Check if parent method(if exists) has the same amount of arguments
    if (parentMethod != null && parentMethod.argc() != args.size()) {
      throw new Exception("'" + this.name + "." + name + "' does not have the same amount of arguments with '" + parentMethod.getOwnClass().getName() + "." + parentMethod.getName() + "'");
    }
    // Initialize method
    Method newMethod = new Method(returnType, name,this);
    // Insert arguments
    for (int i = 0;i < args.size();i++) {
      newMethod.insertArgument(args.get(i));
      // Check for argument type match with parent method
      if (parentMethod != null && !parentMethod.getNthArgument(i).getType().equals(args.get(i).getType())) {
        throw new Exception("Argument '" + args.get(i).getType() + " " + args.get(i).getName() + "' of '" + this.name + "." + newMethod.getName() + "' does not match argument '" + parentMethod.getNthArgument(i).getType() + " " + parentMethod.getNthArgument(i).getName() + "' of '" + parentMethod.getOwnClass().getName() + "." + parentMethod.getName() + "'");
      }
    }
    this.methods.add(newMethod);
    this.methodsMap.put(name, newMethod);
    // Insert the method to the vtable
    if (parentMethod != null) {
      newMethod.setOffset(parentMethod.getOffset());
      this.vtable.set(parentMethod.getOffset()/8, newMethod);
    } else {
      newMethod.setOffset(8*this.vtable.size());
      this.vtable.add(newMethod);
    }
  }

  private static String argsToString(ArrayList<Variable> args) { 
    String ret = "";
    for (int i = 0;i < args.size();i++) {
      ret += args.get(i).getType() + (i < args.size() - 1 ? ", " : "");
    }
    return ret;
  }

  public void insertMethod(String returnType,String name,ArrayList<Variable> args) throws Exception {
    // Check if a method with the same name was already declared in this class
    Method method = getMethodRecursively(name);
    if (method == null) {
      this.addNewMethod(returnType, name,args,method);
    } else {
      // Check if method was also previously declared in the derived(this) class
      // If this class is derived also check if return type matches. Later, the argument types will also be checked if they match
      if (this.parentClass != null && !this.hasMethod(name)) {
        if (method.getReturnType().equals(returnType)) {
          this.addNewMethod(returnType, name,args,method);
        } else {
          throw new Exception("The return type of " + this.name + "." + name + "() is incompatible with " + method.getOwnClass().getName() + "." + method.getName() + "()");
        }
      } else {
        throw new Exception("Duplicate method " + method.getOwnClass().getName() + "." + method.getName() + "(" + argsToString(args) + ")");
      }
    }
  }

  public int getMethodOffset(String methodName) {
    if (!this.methodsMap.containsKey(methodName)) {
      return this.parentClass != null ? this.parentClass.getMethodOffset(methodName) : null;
    } else {
      return this.methodsMap.get(methodName).getOffset();
    }
  }

  public Variable getVariable(String name) {
    if (this.parentClass == null) {
      return this.variablesMap.containsKey(name) ? this.variablesMap.get(name) : null;
    } else {
      return this.variablesMap.containsKey(name) ? this.variablesMap.get(name) : this.parentClass.getVariable(name);
    }
  }

  private void printVariableOffsets() {
    this.variables.forEach(var -> {
        System.out.println(this.name + "." + var.getName() + " : " + var.getOffset());
    });
  }

  private void printMethodOffsets() {
    this.methods.forEach(method -> {
      if (this.parentClass == null || this.parentClass != null && !this.parentClass.hasMethod(method.getName())) {
        System.out.println(this.name + "." + method.getName() + " : " + method.getOffset());
      }
    });
  }

  // Check if given type matches this class's type or any of it's parents classes types
  public boolean checkType(String type) {
    if (type.equals(this.name)) {
      return true;
    } else {
      // Search parent class
      return this.parentClass != null ? this.parentClass.checkType(type) : false;
    }
  }

  public void printOffsetTable() {
    // Print variable offsets
    this.printVariableOffsets();
    // Print method offsets
    this.printMethodOffsets();
  }
}