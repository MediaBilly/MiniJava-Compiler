import syntaxtree.*;
import visitor.*;
import java.io.*;

import components.helpers.SymbolTable;
import components.visitors.SymbolTableVisitor;
import components.visitors.TypeCheckVisitor;
import components.visitors.LLVMVisitor;

public class Main {
    public static void main (String [] args) throws Exception {
        FileInputStream fis = null;
        try{
            for (String file : args) {
                // Parsing
                System.out.println("----- " + file + " -----");
                fis = new FileInputStream(file);
                MiniJavaParser parser = new MiniJavaParser(fis);
                Goal root = parser.Goal();
                System.out.println("Program parsed successfully.");
                // Semantic checking
                SymbolTable symbolTable = new SymbolTable();
                SymbolTableVisitor stVisitor = new SymbolTableVisitor(symbolTable);
                root.accept(stVisitor,null);
                TypeCheckVisitor tcVisitor = new TypeCheckVisitor(symbolTable);
                root.accept(tcVisitor,null);
                System.out.println("Program semantically checked successfully.");
                symbolTable.printOffsetTables();
                // IR Generation
                LLVMVisitor llvmVisitor = new LLVMVisitor(symbolTable,file.replace(".java", ".ll"));
                root.accept(llvmVisitor,null);
                System.out.println("LLVM IR Generated");
                if(fis != null) fis.close();
            }
        }
        catch(ParseException ex){
            System.out.println(ex.getMessage());
        }
        catch(FileNotFoundException ex){
            System.err.println(ex.getMessage());
        }
        finally{
            try{
                if(fis != null) fis.close();
            }
            catch(IOException ex){
                System.err.println(ex.getMessage());
            }
        }
    }
}
