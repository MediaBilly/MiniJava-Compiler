package components.prototypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Method {
  private String returnType;
  private String name;
  private Class ownClass;
  private ArrayList<Variable> argumnents;
  private Map<String,Variable> argumentsMap;
  private Map<String,Variable> variables;
  private ArrayList<Variable> variablesList;
  private int offset;

  public Method(String returnType,String name,Class ownClass) {
    this.returnType = returnType;
    this.name = name;
    this.ownClass = ownClass;
    this.argumnents = new ArrayList<Variable>();
    this.argumentsMap = new HashMap<String,Variable>();
    this.variables = new HashMap<String,Variable>();
    this.variablesList = new ArrayList<Variable>();
    this.offset = 0;
  }

  public String getReturnType() {
    return this.returnType;
  }

  public String getName() {
    return this.name;
  }

  public Class getOwnClass() {
    return this.ownClass;
  }

  public int getOffset() {
    return this.offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public void insertArgument(Variable arg) throws Exception{
    // Check if argument with the same name was already declared
    if (!this.argumentsMap.containsKey(arg.getName())) {
      this.argumnents.add(arg);
      this.argumentsMap.put(arg.getName(), arg);
    } else {
      throw new Exception("Duplicate local variable " + name + " in method " + this.ownClass.getName() + "." + this.name);
    }
  }

  public Variable getNthArgument(int n) {
    return this.argumnents.get(n);
  }

  public void insertVariable(Variable var) throws Exception{
    if (!this.argumentsMap.containsKey(var.getName()) && !this.variables.containsKey(var.getName())) {
      this.variables.put(var.getName(), var);
      this.variablesList.add(var);
    } else {
      throw new Exception("Duplicate local variable " + var.getName() + " in method " + this.ownClass.getName() + "." + this.name);
    }
  }

  public int argc() {
    return this.argumnents.size();
  }

  public ArrayList<Variable> getVariablesList() {
    return this.variablesList;
  }

  public Variable getVariable(String name) {
    if (this.variables.containsKey(name)) {
      return this.variables.get(name);
    } else if (this.argumentsMap.containsKey(name)) {
      return this.argumentsMap.get(name);
    } else {
      return null;
    }
  }
}