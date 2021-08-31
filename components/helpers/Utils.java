package components.helpers;

import java.util.regex.Pattern;

public class Utils {
  private static Pattern numPattern = Pattern.compile("\\d+");

  public static final int getTypeSize(String type) {
    switch (type) {
      case "boolean":
        return 1;
      case "int":
        return 4;
      case "boolean[]":
      case "int[]":
      case "String[]":
      default:
        return 8;
    }
  }

  public static final boolean isPrimitiveType(String type) {
    switch (type) {
      case "boolean":
      case "int":
      case "boolean[]":
      case "int[]":
      case "String[]":
      return true;
      default:
      return false;
    }
  }

  public static final boolean isNumericType(String expr) throws Exception{
    if (expr == null) {
      return false;
    }
    if (numPattern.matcher(expr).matches()) {
      try {
        Integer.parseInt(expr);
      } catch (NumberFormatException e) {
        throw new Exception("The integer literal: " + expr + " is invalid.");
      }
      return true;
    } else {
      return false;
    }
  }

  public static final String llvmType(String minijavaType) {
    switch (minijavaType) {
      case "boolean":
        return "i1";
      case "int":
        return "i32";
      case "int[]":
        return "i32*";
      case "boolean[]":
      case "string[]":
      default:
        return "i8*";
    }
  } 
}