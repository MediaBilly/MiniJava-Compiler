# MiniJava-Compiler
A compiler for a subset of java called minijava. It was implemented as a project for the compilers university course.

## Requirements:
- Java JDK and runtime environment.
- clang

## Compilation:
### To compile the program, type `make compile`
### To clean object files and executable: `make clean`
## Usage: 
- Usage: java Main [file1] [file2] ... [fileN].
- For each .java file given for compilation, a file with the same name and extension will be generated in the same location .ll 
  (For example, for the And.java file the And.ll file will be generated) which contains the intermediate code for the minijava program in LLVM IR.
