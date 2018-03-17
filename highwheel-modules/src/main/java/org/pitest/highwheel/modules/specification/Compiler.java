package org.pitest.highwheel.modules.specification;

import org.pitest.highwheel.modules.model.Module;
import org.pitest.highwheel.modules.model.rules.Dependency;
import org.pitest.highwheel.modules.model.rules.NoDirectDependency;
import org.pitest.highwheel.modules.model.rules.Rule;
import org.pitest.highwheel.util.base.Optional;

import java.util.*;

public class Compiler {

    public static final String MODULE_REGEX_NOT_WELL_DEFINED = "Regular expression '%s' of module '%s' is not well defined";
    public static final String MODULE_ALREADY_DEFINED = "Module '%s' has already been defined";
    public static final String MODULE_HAS_NOT_BEEN_DEFINED = "Module '%s' referenced in rule '%s' has not been defined";

    public static class Definition {
        public final Collection<Module> modules;
        public final Collection<Rule> rules;

        public Definition(Collection<Module> modules, Collection<Rule> rules) {
            this.modules = modules;
            this.rules = rules;
        }

        @Override
        public String toString() {
            return "Definition{" +
                    "modules=" + modules +
                    ", rules=" + rules +
                    '}';
        }
    }

    public Definition compile(SyntaxTree.Definition definition) {
        final Map<String,Module> modules = compileModules(definition.moduleDefinitions);
        final List<Rule> rules = compileRules(definition.rules,modules);

        return new Definition(modules.values(),rules);
    }

    private Map<String,Module> compileModules(List<SyntaxTree.ModuleDefinition> definitions) {
        final Map<String,Module> modules = new HashMap<String, Module>(definitions.size());

        for(SyntaxTree.ModuleDefinition moduleDefinition : definitions) {
            final Optional<Module> optionalModule = Module.make(moduleDefinition.moduleName,moduleDefinition.moduleRegex);
            if(!optionalModule.isPresent()) {
                throw new CompilerException(String.format(MODULE_REGEX_NOT_WELL_DEFINED,moduleDefinition.moduleRegex,moduleDefinition.moduleName));
            } else if(modules.get(moduleDefinition.moduleName) != null) {
                throw new CompilerException(String.format(MODULE_ALREADY_DEFINED, moduleDefinition.moduleName));
            } else {
                modules.put(moduleDefinition.moduleName,optionalModule.get());
            }
        }

        return modules;
    }

    private List<Rule> compileRules(List<SyntaxTree.Rule> rulesDefinition, Map<String, Module> modules) {
        final List<Rule> rules = new ArrayList<Rule>();
        for(SyntaxTree.Rule ruleDefinition: rulesDefinition) {
            if(ruleDefinition instanceof SyntaxTree.ChainDependencyRule) {
                SyntaxTree.ChainDependencyRule chainDependencyRule = (SyntaxTree.ChainDependencyRule) ruleDefinition;
                rules.addAll(compileChainDependencies(chainDependencyRule.moduleNameChain,modules));
            } else if(ruleDefinition instanceof SyntaxTree.NoDependentRule){
                SyntaxTree.NoDependentRule noDependentRule = (SyntaxTree.NoDependentRule) ruleDefinition;
                rules.add(compileNoDependency(noDependentRule,modules));
            }
        }
        return rules;
    }

    private List<Rule> compileChainDependencies(List<String> chainDependencies, Map<String,Module> modules) {
        final List<Rule> result = new ArrayList<Rule>(chainDependencies.size() - 1);
        for(int i = 0; i < chainDependencies.size() -1; ++i) {
            final String current = chainDependencies.get(i);
            final String next = chainDependencies.get(i+1);
            if(modules.get(current) == null) {
                throw new CompilerException(String.format(MODULE_HAS_NOT_BEEN_DEFINED, current, join(" -> ", chainDependencies)));
            } else if(modules.get(next) == null) {
                throw new CompilerException(String.format(MODULE_HAS_NOT_BEEN_DEFINED, next, join(" -> ", chainDependencies)));
            } else {
                result.add(new Dependency(modules.get(current),modules.get(next)));
            }
        }
        return result;
    }

    private NoDirectDependency compileNoDependency(SyntaxTree.NoDependentRule noDependentRule, Map<String,Module> modules) {
        if(modules.get(noDependentRule.left) == null) {
            throw new CompilerException(String.format(MODULE_HAS_NOT_BEEN_DEFINED, noDependentRule.left, join(" -/-> ", Arrays.asList(noDependentRule.left,noDependentRule.right))));
        } else if(modules.get(noDependentRule.right) == null) {
            throw new CompilerException(String.format(MODULE_HAS_NOT_BEEN_DEFINED, noDependentRule.right, join(" -/-> ", Arrays.asList(noDependentRule.left,noDependentRule.right))));
        } else {
            return new NoDirectDependency(modules.get(noDependentRule.left),modules.get(noDependentRule.right));
        }
    }

    private static <T> String join(String separator, Iterable<T> iterable) {
        final StringBuilder buff = new StringBuilder("");
        String sep = "";
        for(T item : iterable) {
            buff.append(sep).append(item.toString());
            sep = separator;
        }
        return buff.toString();
    }
}
