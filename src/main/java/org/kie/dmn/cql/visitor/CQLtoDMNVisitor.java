package org.kie.dmn.cql.visitor;

import org.antlr.v4.runtime.ParserRuleContext;
import org.cqframework.cql.gen.cqlBaseVisitor;
import org.cqframework.cql.gen.cqlParser;
import org.kie.dmn.api.core.ast.InputDataNode;
import org.kie.dmn.core.ast.InputDataNodeImpl;
import org.kie.dmn.model.v1_1.*;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CQLtoDMNVisitor extends cqlBaseVisitor {

    public static final String VALUESET_ID = "valuesetId";
    public static final String VERSION_SPECIFIER = "versionSpecifier";
    public static final String CODESYSTEMS = "codesystems";
    public static final String T_VALUESET = "tValueset";

    private Map<String, DMNElement> dmnElements = new HashMap<>();
    private Decision currentDecision;

    @Override
    public Object visitLibrary(cqlParser.LibraryContext ctx) {
        Definitions def = (Definitions) visit(ctx.libraryDefinition());

        List<cqlParser.ValuesetDefinitionContext> valuesetDefinitionContexts = ctx.valuesetDefinition();
        if (!valuesetDefinitionContexts.isEmpty()) {
            ItemDefinition valuesetType = generateValueSetType();
            def.getItemDefinition().add(valuesetType);
            for (cqlParser.ValuesetDefinitionContext vsd : valuesetDefinitionContexts) {
                Decision decision = (Decision) visit(vsd);
                dmnElements.put( decision.getName(), decision );
                def.getDrgElement().add(decision);
            }
        }

        List<cqlParser.ParameterDefinitionContext> parameters = ctx.parameterDefinition();
        if( ! parameters.isEmpty() ) {
            for(cqlParser.ParameterDefinitionContext stc : parameters ) {
                InputData input = (InputData) visit(stc);
                if( input != null ) {
                    dmnElements.put( input.getName(), input );
                    def.getDrgElement().add( input );
                }
            }
        }

        List<cqlParser.StatementContext> statements = ctx.statement();
        if( ! statements.isEmpty() ) {
            for(cqlParser.StatementContext stc : statements ) {
                Decision decision = (Decision) visit(stc);
                if( decision != null ) {
                    dmnElements.put( decision.getName(), decision );
                    def.getDrgElement().add( decision );
                }
            }
        }

        return def;
    }

    @Override
    public Object visitLibraryDefinition(cqlParser.LibraryDefinitionContext ctx) {
        Definitions definitions = new Definitions();
        String libname = unquote( ctx.identifier() );
        String libversion = unquote( ctx.versionSpecifier() );
        String namespace = "http://www.kiegroup.org/" + libname + ( libversion != null ? "/"+libversion : "" );
        definitions.setNamespace(  namespace );
        definitions.setName("Model");
        definitions.setId(generateId());

        definitions.getNsContext().put( "feel", "http://www.omg.org/spec/FEEL/20140401");
        definitions.getNsContext().put( "kie", namespace );

        return definitions;
    }

    @Override
    public Object visitValuesetDefinition(cqlParser.ValuesetDefinitionContext ctx) {
        Decision decision = new Decision();
        decision.setName( unquote( ctx.identifier() ) );
        decision.setId( generateId() );

        InformationItem var = new InformationItem();
        var.setName( decision.getName() );
        var.setTypeRef( new QName( "kie:"+T_VALUESET ) );

        decision.setVariable( var );

        Context context = new Context();
        context.getContextEntry().add( createContextEntry( VALUESET_ID, "feel:string", quote( unquote( ctx.valuesetId() ) ) ) );
//        context.getContextEntry().add( createContextEntry( VERSION_SPECIFIER, "feel:string", quote( unquote( ctx.versionSpecifier() ) ) ) );
//        context.getContextEntry().add( createContextEntry( CODESYSTEMS, null, "null" ) );
        // TODO: create the list of system identifiers
//        org.kie.dmn.model.v1_1.List list = new org.kie.dmn.model.v1_1.List();
//        list.addChildren( createLiteralExpression( ctx.codesystems().codesystemIdentifier() ) );

        decision.setExpression( context );

        return decision;
    }

    @Override
    public Object visitExpressionDefinition(cqlParser.ExpressionDefinitionContext ctx) {
        Decision decision = new Decision();
        decision.setName( unquote( ctx.identifier() ) );
        decision.setId( generateId() );

        InformationItem var = new InformationItem();
        var.setName( decision.getName() );
        var.setId( generateId() );
        decision.setVariable( var );

        LiteralExpression expression = new LiteralExpression();
        String exprString = "// " + ctx.expression().getText();
        expression.setText(exprString);
        expression.setId( generateId() );
        decision.setExpression( expression );

        this.currentDecision = decision;
        visit( ctx.expression() );
        this.currentDecision = null;

        return decision;
    }

    @Override
    public Object visitMemberInvocation(cqlParser.MemberInvocationContext ctx) {
        String invocation = unquote( ctx );
        addInformationRequirement(invocation);
        return super.visitMemberInvocation(ctx);
    }

    @Override
    public Object visitQualifiedIdentifier(cqlParser.QualifiedIdentifierContext ctx) {
        String qualifiedName = unquote( ctx );
        addInformationRequirement( qualifiedName );
        return super.visitQualifiedIdentifier(ctx);
    }

    @Override
    public Object visitParameterDefinition(cqlParser.ParameterDefinitionContext ctx) {
        InputData input = new InputData();
        input.setName( unquote( ctx.identifier() ) );
        input.setId( generateId() );

        InformationItem var = new InformationItem();
        var.setName( input.getName() );
        //var.setTypeRef( new QName( "kie:"+T_VALUESET ) );

        input.setVariable( var );

        return input;
    }

    private void addInformationRequirement(String name) {
        if( currentDecision != null && dmnElements.containsKey(name) ) {
            DMNElement dmnElement = dmnElements.get(name);
            DMNElementReference ref = new DMNElementReference();
            ref.setHref( "#"+dmnElement.getId() );

            InformationRequirement req = new InformationRequirement();
            req.setRequiredDecision( ref );

            if( !contains(currentDecision.getInformationRequirement(), req ) ) {
                currentDecision.getInformationRequirement().add( req );
            }
        }
    }

    private boolean contains(List<InformationRequirement> informationRequirement, InformationRequirement ir) {
        for( InformationRequirement req : informationRequirement ) {
            if( req.getRequiredDecision().getHref().equals( ir.getRequiredDecision().getHref() ) ) {
                return true;
            }
        }
        return false;
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

    private ContextEntry createContextEntry(String name, String typeName, String expression ) {
        ContextEntry ce = new ContextEntry();
        InformationItem vn = new InformationItem();
        vn.setName( name );
        if( typeName != null ) {
            vn.setTypeRef( new QName( typeName ) );
        }
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

    private String generateId() {
        return "_"+ UUID.randomUUID().toString();
    }


}
