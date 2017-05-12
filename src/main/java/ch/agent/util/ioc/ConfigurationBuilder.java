package ch.agent.util.ioc;

import static ch.agent.util.STRINGS.msg;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ch.agent.util.STRINGS.U;
import ch.agent.util.args.Args;
import ch.agent.util.base.Misc;

/**
 * A configuration builder turns a specification into a configuration.
 * <p>
 * The builder is designed to be extensible. In a typical use case the client
 * code uses the builder to create a configuration from a textual specification
 * with {@link #build(String)}. In most situations, subclasses override only
 * {@link #defineSyntax(Args)} and {@link #build(Args)}.
 * <p>
 * This builder extracts a series of module specifications from an input like:
 * 
 * <pre>
 * <code>
 * module=[<em>string containing the module specification</em>]
 * module=[<em>string containing the module specification</em>]
 * ...
 * module=[<em>string containing the module specification</em>]
 * </code>
 * </pre>
 * 
 * The configuration builder does not know anything about module specifications
 * and delegates their handling to a <em>module definition builder</em> passed
 * to the constructor.
 * <p>
 * The builder also extracts the <em>execution</em>
 * specification from:
 * 
 * <pre>
 * <code>
 * execution=[<em>execution string</em>]  (abbreviated exec)
 * </code>
 * </pre>
 * 
 * @param <C>
 *            the configuration type
 * @param <B>
 *            the module definition builder type
 * @param <D>
 *            the module definition type
 * @param <M>
 *            the module type
 */
public class ConfigurationBuilder<C extends Configuration<D,M>, B extends ModuleDefinitionBuilder<D,M>, D extends ModuleDefinition<M>, M extends Module<?>> {

	public static final String MODULE = "module";
	public static final String EXEC = "execution";
	public static final String EXEC_AKA = "exec";
	
	private B moduleDefinitionBuilder;
	private Args parameters;
	
	/**
	 * Constructor.
	 * 
	 * @param builder the module definition builder to use
	 */
	public ConfigurationBuilder(B builder) {
		super();
		this.moduleDefinitionBuilder = builder;
	}
	
	/**
	 * Build a configuration from a textual specification.
	 * <p>
	 * Note: clients with an array of specifications can concatenate elements of
	 * the array with white space as separators.
	 * 
	 * @param specification
	 *            a string containing the specification
	 * @return a configuration
	 * @throws ConfigurationException
	 *             if something is wrong
	 */
	public C build(String specification) {
		if (parameters == null) {
			parameters = new Args();
			defineSyntax(parameters);
		} else
			parameters.reset();
		parameters.parse(specification);
		return build(parameters);
	}
	
	/**
	 * Define the parameter syntax.
	 * 
	 * @param p
	 *            the object taking parameters
	 */
	protected void defineSyntax(Args p) {
		p.def(MODULE).repeatable().init("");
		p.def(EXEC).aka(EXEC_AKA).init(""); // can be omitted
	}

	/**
	 * Build a configuration from an object encapsulating parameter values.
	 * 
	 * @param p
	 *            the object taking parameters
	 * @return a configuration
	 * @throws ConfigurationException
	 *             if something is wrong
	 */
	@SuppressWarnings("unchecked")
	protected C build(Args p) {
		String[] moduleStatements = p.split(MODULE);
		String exec = p.get(EXEC);
		List<D> sortedModules = parseModuleSpecifications(moduleStatements);
		return (C) new Configuration<D,M>(sortedModules, exec);
	}

	/**
	 * Parse an array of module specifications into a list of module
	 * definitions. The list sequence takes dependencies into account. This is
	 * the order to use for initializing modules and the reverse order for
	 * shutting down.
	 * <p>
	 * This method encapsulates some of the essential logic of the builder and
	 * can be reused when overriding {@link #build(Args)}. Subclasses which need
	 * to enforce constraints on dependency requirements should override
	 * {@link #sortDependencies} and/or {@link #validatePrerequisite}.
	 * 
	 * @param specifications
	 *            an array of module specifications
	 * @return a list of module definitions
	 * @throws ConfigurationException
	 *             when a configuration error is detected
	 */
	protected List<D> parseModuleSpecifications(String[] specifications) {
		Map<String, D> definitions = new LinkedHashMap<String, D>(specifications.length);
		parseModuleSpecifications(specifications, definitions);
		validatePrerequisites(definitions);
		return sortDependencies(definitions);
	}

	/**
	 * Parse module specifications and add them to a map keyed by module name.
	 * 
	 * @param specifications
	 *            array of specifications
	 * @param definitions
	 *            a map of module definitions keyed by module name
	 * @throws ConfigurationException
	 *             when a configuration error is detected
	 */
	protected void parseModuleSpecifications(String[] specifications, Map<String, D> definitions) {
		for (String spec : specifications) {
			try {
				D def = moduleDefinitionBuilder.build(spec);
				if (definitions.put(def.getName(), def) != null)
					throw new ConfigurationException(msg(U.C11, def.getName()));
			} catch (Exception e) {
				throw new ConfigurationException(msg(U.C15, spec), e);
			}
		}
	}

	/**
	 * Validate module requirements.
	 * 
	 * @param definitions
	 *            a map of module definitions keyed by name
	 * @throws ConfigurationException
	 *             if one or more required modules are missing
	 */
	protected void validatePrerequisites(Map<String, D> definitions) {
		List<String> missing = new ArrayList<String>();
		for (D def : definitions.values()) {
			for (String name : def.getPredecessors()) {
				D pred = definitions.get(name);
				if (pred == null)
					missing.add(name);
				else
					validatePrerequisite(def, pred, false);
			}
			for (String name : def.getRequirements()) {
				D req = definitions.get(name);
				if (req == null)
					missing.add(name);
				else
					validatePrerequisite(def, req, true);
			}
		}
		if (missing.size() > 0)
			throw new ConfigurationException(msg(U.C16, Misc.join("\", \"", missing)));
	}
	
	/**
	 * Validate a prerequisite for a module. This method is a hook for
	 * subclasses.
	 * 
	 * @param module
	 *            the definition of the module which has the prerequisite
	 * @param prerequisite
	 *            the definition of the prerequisite
	 * @param requirement
	 *            true if it is a requirement, false if it is a predecessor
	 * @throws ConfigurationException
	 *             if the requirement is rejected
	 */
	protected void validatePrerequisite(D module, D prerequisite, boolean requirement) {
	}
	
	/**
	 * Determine a valid sequence in which modules can be initialized. At
	 * {@link Container#shutdown}, the modules are stopped in the inverse
	 * sequence.
	 * <p>
	 * The only constraint in this implementation is that a requirement or a
	 * predecessor cannot appear in the result list after a module depending
	 * directly or indirectly on it. Such a list always exists, unless there is
	 * a dependency cycle.
	 * <p>
	 * Additional constraints are delegated to subclasses. Typically, the
	 * overriding method would call the super method, inspect the result list,
	 * and throw a {@link ConfigurationException} if something is wrong.
	 * 
	 * @param definitions
	 *            a map of module definitions keyed by name
	 * @return a list module definitions in valid initialization sequence
	 * @throws ConfigurationException
	 *             if it is impossible to compute a valid sequence
	 */
	protected List<D> sortDependencies(Map<String, D> definitions) {
		// an exception during construction is a bug because the input is clean ...
		DAG<String> dag = new DAG<String>();
		dag.add(definitions.keySet());
		for (ModuleDefinition<M> spec : definitions.values()) {
			dag.addLinks(spec.getName(), spec.getPrerequisites());
		}
		// ... except for a possible cycle
		List<String> sequence;
		try {
			sequence = dag.sort();
		} catch (Exception e) {
			throw new ConfigurationException(msg(U.C09), e);
		}
		List<D> sorted = new ArrayList<D>(definitions.size());
		for (String name : sequence) {
			sorted.add(definitions.get(name));
		}
		// bug detector:
		if (sorted.size() != definitions.size())
			throw new IllegalStateException(String.format("bug found: size before=%d, size after=%d", definitions.size(), sorted.size()));
		return sorted;
	}

}
