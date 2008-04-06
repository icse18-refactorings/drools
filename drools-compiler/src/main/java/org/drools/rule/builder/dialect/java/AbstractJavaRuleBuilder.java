package org.drools.rule.builder.dialect.java;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.lang.descr.BaseDescr;
import org.drools.rule.Declaration;
import org.drools.rule.builder.RuleBuildContext;
import org.drools.util.StringUtils;
import org.mvel.MVEL;
import org.mvel.integration.impl.MapVariableResolverFactory;
import org.mvel.templates.TemplateRegistry;
import org.mvel.templates.SimpleTemplateRegistry;
import org.mvel.templates.TemplateRuntime;
import org.mvel.templates.TemplateCompiler;
import org.mvel.optimizers.OptimizerFactory;
import com.sun.org.apache.xalan.internal.xsltc.compiler.Template;

public class AbstractJavaRuleBuilder {

    protected static final TemplateRegistry RULE_REGISTRY = new SimpleTemplateRegistry();
    protected static final TemplateRegistry INVOKER_REGISTRY = new SimpleTemplateRegistry();

    static {
        OptimizerFactory.setDefaultOptimizer("reflective");

        RULE_REGISTRY.addNamedTemplate("rules", TemplateCompiler.compileTemplate(AbstractJavaRuleBuilder.class.getResourceAsStream("javaRule.mvel"), null));
        INVOKER_REGISTRY.addNamedTemplate("invokers", TemplateCompiler.compileTemplate(AbstractJavaRuleBuilder.class.getResourceAsStream("javaInvokers.mvel"), null));

        /**
         * Process these templates
         */
        TemplateRuntime.execute(RULE_REGISTRY.getNamedTemplate("rules"), null, null, RULE_REGISTRY);
        TemplateRuntime.execute(INVOKER_REGISTRY.getNamedTemplate("invokers"), null, null, INVOKER_REGISTRY);

    }

    public TemplateRegistry getRuleTemplateRegistry() {
        return RULE_REGISTRY;
    }

    public TemplateRegistry getInvokerTemplateRegistry() {
        return INVOKER_REGISTRY;
    }

    public Map createVariableContext(final String className,
                                     final String text,
                                     final RuleBuildContext context,
                                     final Declaration[] declarations,
                                     final Declaration[] localDeclarations,
                                     final String[] globals) {
        final Map map = new HashMap();

        map.put("methodName",
                className);

        map.put("package",
                context.getPkg().getName());

        map.put("ruleClassName",
                StringUtils.ucFirst(context.getRuleDescr().getClassName()));

        map.put("invokerClassName",
                context.getRuleDescr().getClassName() + StringUtils.ucFirst(className) + "Invoker");

        if (text != null) {
            map.put("text",
                    text);

            map.put("hashCode",
                    new Integer(text.hashCode()));
        }

        final String[] declarationTypes = new String[declarations.length];
        for (int i = 0, size = declarations.length; i < size; i++) {
            declarationTypes[i] = ((JavaDialect) context.getDialect()).getTypeFixer().fix(declarations[i]);
        }

        map.put("declarations",
                declarations);

        map.put("declarationTypes",
                declarationTypes);

        if (localDeclarations != null) {
            final String[] localDeclarationTypes = new String[localDeclarations.length];
            for (int i = 0, size = localDeclarations.length; i < size; i++) {
                localDeclarationTypes[i] = ((JavaDialect) context.getDialect()).getTypeFixer().fix(localDeclarations[i]);
            }

            map.put("localDeclarations",
                    localDeclarations);

            map.put("localDeclarationTypes",
                    localDeclarationTypes);
        }

        final List globalTypes = new ArrayList(globals.length);
        for (int i = 0, length = globals.length; i < length; i++) {
            globalTypes.add(((Class) context.getPkg().getGlobals().get(globals[i])).getName().replace('$',
                    '.'));
        }

        map.put("globals",
                globals);

        map.put("globalTypes",
                globalTypes);

        return map;
    }

    public void generatTemplates(final String ruleTemplate,
                                 final String invokerTemplate,
                                 final RuleBuildContext context,
                                 final String className,
                                 final Map vars,
                                 final Object invokerLookup,
                                 final BaseDescr descrLookup) {
        TemplateRegistry registry = getRuleTemplateRegistry();

        context.getMethods().add(
             TemplateRuntime.execute(registry.getNamedTemplate(ruleTemplate), null, new MapVariableResolverFactory(vars), registry)
        );


        registry = getInvokerTemplateRegistry();
        final String invokerClassName = context.getPkg().getName() + "." + context.getRuleDescr().getClassName() + StringUtils.ucFirst(className) + "Invoker";


        Object debug;
        context.getInvokers().put(invokerClassName,
              debug = TemplateRuntime.execute(registry.getNamedTemplate(invokerTemplate), null, new MapVariableResolverFactory(vars), registry)
        );

        System.out.println("----");
        System.out.println(String.valueOf(debug));
        System.out.println("----");

        context.getInvokerLookups().put(invokerClassName,
                invokerLookup);
        context.getDescrLookups().put(invokerClassName,
                descrLookup);
    }
}
