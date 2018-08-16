package org.kie.dmn.cql.visitor;

import org.antlr.v4.runtime.ParserRuleContext;
import org.cqframework.cql.gen.cqlBaseVisitor;
import org.cqframework.cql.gen.cqlParser;
import org.kie.dmn.model.v1_1.Definitions;

public class CQLtoDMNVisitor extends cqlBaseVisitor {

    private Definitions definitions;

    public Definitions getDefinitions() {
        return definitions;
    }

    @Override
    public Object visitLibrary(cqlParser.LibraryContext ctx) {
        definitions = new Definitions();
        super.visitLibrary(ctx);
        return definitions;
    }

    @Override
    public Object visitLibraryDefinition(cqlParser.LibraryDefinitionContext ctx) {
        String libname = unquote( ctx.identifier() );
        String libversion = unquote( ctx.versionSpecifier() );
        String namespace = libname + ( libversion != null ? ":"+libversion : "" );
        definitions.setNamespace(  namespace );
        definitions.setName("Model");
        definitions.setId( "_"+ namespace.hashCode() );

        return definitions;
    }

    private String nullsafeText(ParserRuleContext ctx) {
        return nullsafeText(ctx, null);
    }

    private String nullsafeText(ParserRuleContext ctx, String defaultValue) {
        return ctx != null ? ctx.getText() : defaultValue;
    }

    private String unquote(ParserRuleContext ctx) {
        if (ctx == null) return null;
        String text = ctx.getText();

        if ( text.startsWith("\"") || text.startsWith("'")) {
            // chop off leading and trailing ' or "
            text = text.substring(1, text.length() - 1);
        }

        return text;
    }


}
