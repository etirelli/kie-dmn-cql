package org.kie.dmn.cql;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.cqframework.cql.gen.cqlLexer;
import org.cqframework.cql.gen.cqlParser;
import org.kie.dmn.cql.visitor.CQLtoDMNVisitor;
import org.kie.dmn.model.v1_1.Definitions;

import java.io.IOException;
import java.io.InputStream;

public class CQLtoDMNTranslator {

    public static Definitions translate(InputStream is) throws IOException {
        ANTLRInputStream input = new ANTLRInputStream(is);
        cqlLexer lexer = new cqlLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        cqlParser parser = new cqlParser(tokens);
        parser.setBuildParseTree(true);
        ParseTree tree = parser.library();

        CQLtoDMNVisitor visitor = new CQLtoDMNVisitor();
        return (Definitions) visitor.visit(tree);
    }

}
