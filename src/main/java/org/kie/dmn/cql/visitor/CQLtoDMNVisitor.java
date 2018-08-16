package org.kie.dmn.cql.visitor;

import org.antlr.v4.runtime.ParserRuleContext;
import org.cqframework.cql.gen.cqlBaseVisitor;
import org.cqframework.cql.gen.cqlParser;
import org.kie.dmn.model.v1_1.*;

import javax.xml.namespace.QName;
import java.util.List;

public class CQLtoDMNVisitor extends cqlBaseVisitor {

    public static final String VALUESET_ID = "valuesetId";
    public static final String VERSION_SPECIFIER = "versionSpecifier";
    public static final String CODESYSTEMS = "codesystems";
    public static final String T_VALUESET = "tValueset";

    @Override
    public Object visitLibrary(cqlParser.LibraryContext ctx) {
        Definitions def = (Definitions) visit(ctx.libraryDefinition());

        List<cqlParser.ValuesetDefinitionContext> valuesetDefinitionContexts = ctx.valuesetDefinition();
        if (!valuesetDefinitionContexts.isEmpty()) {
            ItemDefinition valuesetType = generateValueSetType();
            def.getItemDefinition().add(valuesetType);
            for (cqlParser.ValuesetDefinitionContext vsd : valuesetDefinitionContexts) {
                def.getDrgElement().add((Decision) visit(vsd));
            }
        }
        return def;
    }

    private ItemDefinition generateValueSetType() {
        // for now, we generate the valueset type local to the model, but ideally
        // we need a model with the common datatypes that we can import and reuse
        ItemDefinition valueset = new ItemDefinition();
        valueset.setName(T_VALUESET);

        ItemDefinition valuesetId = new ItemDefinition();
        valuesetId.setName(VALUESET_ID);
        valuesetId.setTypeRef( new QName("feel:string" ));
        valueset.getItemComponent().add(valuesetId);

        ItemDefinition version = new ItemDefinition();
        version.setName(VERSION_SPECIFIER);
        version.setTypeRef( new QName("feel:string" ));
        valueset.getItemComponent().add(version);

        ItemDefinition code = new ItemDefinition();
        code.setName(CODESYSTEMS);
        code.setTypeRef( new QName("feel:string" ));
        code.setIsCollection(true);
        valueset.getItemComponent().add(code);

        return valueset;
    }

    @Override
    public Object visitLibraryDefinition(cqlParser.LibraryDefinitionContext ctx) {
        Definitions definitions = new Definitions();
        String libname = unquote( ctx.identifier() );
        String libversion = unquote( ctx.versionSpecifier() );
        String namespace = "http://www.kiegroup.org/" + libname + ( libversion != null ? "/"+libversion : "" );
        definitions.setNamespace(  namespace );
        definitions.setName("Model");
        definitions.setId(generateId(namespace));

        return definitions;
    }

    @Override
    public Object visitValuesetDefinition(cqlParser.ValuesetDefinitionContext ctx) {
        Decision decision = new Decision();
        decision.setName( unquote( ctx.identifier() ) );
        decision.setId( generateId( decision.getName() ) );

        InformationItem var = new InformationItem();
        var.setName( decision.getName() );
        var.setTypeRef( new QName( T_VALUESET ) );

        decision.setVariable( var );

        Context context = new Context();
        context.getContextEntry().add( createContextEntry( VALUESET_ID, "feel", "string", quote( unquote( ctx.valuesetId() ) ) ) );
        context.getContextEntry().add( createContextEntry( VERSION_SPECIFIER, "feel", "string", quote( unquote( ctx.versionSpecifier() ) ) ) );
        // TODO: create the list of system identifiers
//        org.kie.dmn.model.v1_1.List list = new org.kie.dmn.model.v1_1.List();
//        list.addChildren( createLiteralExpression( ctx.codesystems().codesystemIdentifier() ) );
        //context.addChildren( createContextEntry( CODESYSTEMS, "feel", "string", quote( unquote( ctx.valuesetId() ) ) ) );

        decision.setExpression( context );

        return decision;
    }

    private ContextEntry createContextEntry(String name, String typeNamespace, String typeName, String expression ) {
        ContextEntry ce = new ContextEntry();
        InformationItem vn = new InformationItem();
        vn.setName( name );
        //vn.setTypeRef( new QName( typeNamespace, typeName ) );
        LiteralExpression expr = new LiteralExpression();
        expr.setText( expression );
        ce.setVariable(vn);
        ce.setExpression(expr);
        return ce;
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

    private String quote(String text) {
        return text != null ? "\""+text+"\"" : "null";
    }

    private String generateId(String seed) {
        return "_"+ Math.abs(seed.hashCode());
    }


}
