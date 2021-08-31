package components.prototypes;

import components.helpers.Utils;

public class Variable {
  private String type;
  private String name;
  private int size;
  private int offset;

  public Variable(String type,String name) {
    this.type = type;
    this.name = name;
    this.size = Utils.getTypeSize(type);
    this.offset = 0;
  }

  public Variable(String type,String name,int offset) {
    this(type, name);
    this.offset = offset;
  }

  public String getType() {
    return this.type;
  }

  public String getName() {
    return this.name;
  }

  public int getSize() {
    return this.size;
  }

  public int getOffset() {
    return this.offset;
  }
}