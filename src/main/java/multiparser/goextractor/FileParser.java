package multiparser.goextractor;

import multiparser.goextractor.antlr4.GolangLexer;
import multiparser.goextractor.antlr4.GolangParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import multiparser.goextractor.visitor.firstpass.EntityVisitor;

import java.io.IOException;

public class FileParser {

    private String fileFullPath;
    public FileParser(String str) {
        this.fileFullPath = str;
    }

    public void parserOneFile() throws IOException {
        CharStream input = CharStreams.fromFileName(fileFullPath);
        GolangLexer lexer = new GolangLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        GolangParser parser = new GolangParser(tokens);
        ParseTree tree = parser.sourceFile(); // root rule: sourceFile

        EntityVisitor entityVisitor = new EntityVisitor(fileFullPath);
        entityVisitor.visit(tree);
    }


}