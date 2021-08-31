all: compile

compile:
	java -jar jtb132di.jar -te minijava.jj
	java -jar javacc5.jar minijava-jtb.jj
	javac Main.java

clean:
	rm -f *.class *~
	rm -r -f syntaxtree visitor 
	rm components/*/*.class
	rm MiniJavaParser.java MiniJavaParserConstants.java MiniJavaParserTokenManager.java ParseException.java Token.java TokenMgrError.java JavaCharStream.java minijava-jtb.jj
