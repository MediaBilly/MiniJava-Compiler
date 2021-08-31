# MiniJava-Compiler
A compiler for a subset of java called minijava. It was implemented as a project for the compilers university course.

## Requirements:
- Java JDK and runtime environment.
- clang

## Compilation:
### To compile the program, type: 
`make compile`
### To clean object files and executable: 
`make clean`

## Usage: 
### To compile minijava programs, type: 
`java Main [file1] [file2] ... [fileN].`
For each .java file given for compilation, a file with the same name and extension will be generated in the same location .ll 
(For example, for the And.java file the And.ll file will be generated) which contains the intermediate code for the minijava program in LLVM IR. To generate executable programs from LLVM IR file type: 
`clang -o <executable_filename> <llvm_ir_filename>`

#### For more information, check the project description here: 
http://cgi.di.uoa.gr/~compilers/19_20/project.html
