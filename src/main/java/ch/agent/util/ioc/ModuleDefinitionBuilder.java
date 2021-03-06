package ch.agent.util.ioc;

import ch.agent.util.args.Args;

/**
 * A module definition builder turns a specification into a module definition.
 * <p>
 * The builder is designed to be extensible. In a typical use case the client
 * code uses the builder to create a module definition from a textual
 * specification with {@link #build(String)}. In most situations, subclasses
 * override only {@link #defineSyntax(Args)} and {@link #build(Args)}.
 * <p>
 * The builder extracts the module specification from:
 * 
 * <pre>
 * <code>
 * module=[
 *   name=<em>module name</em> 
 *   class=<em>class name</em> 
 *   requirement*=<em>module name</em> (abbreviated require)
 *   predecessor*=<em>module name</em> (abb. pred)
 *   configuration?=<em>configuration string</em> (abb. config)
 * ]
 * </code>
 * </pre>
 * 
 * @param <MD>
 *            the module definition type
 * @param <M>
 *            the module type
 */
public class ModuleDefinitionBuilder<MD extends ModuleDefinition<M>, M extends Module<?>> {

	public static final String MODULE_NAME = "name";
	public static final String MODULE_CLASS = "class";
	public static final String MODULE_REQUIREMENT = "requirement";
	public static final String MODULE_REQUIREMENT_AKA = "require";
	public static final String MODULE_PREDECESSOR = "predecessor";
	public static final String MODULE_PREDECESSOR_AKA = "pred";
	public static final String MODULE_CONFIG = "configuration";
	public static final String MODULE_CONFIG_AKA = "config";

	private Args parameters;

	public ModuleDefinitionBuilder() {
	}

	/**
	 * Build a module definition from a textual specification.
	 * <p>
	 * Note: clients with an array of specifications can concatenate elements of
	 * the array with white spaces as separators.
	 * 
	 * @param specification
	 *            a string containing the specification
	 * @return a module definition
	 * @throws IllegalArgumentException
	 *             if there is an error with the specification
	 */
	public MD build(String specification) {
		if (parameters == null) {
			parameters = new Args();
			defineSyntax(parameters);
		} else
			parameters.reset();
		parameters.parse(specification);
		return build(parameters);
	}

	/**
	 * Define the parameter syntax using an <code>Args</code> object.
	 * 
	 * @param p
	 *            the object taking parameters
	 */
	protected void defineSyntax(Args p) {
		p.def(MODULE_NAME);
		p.def(MODULE_CLASS);
		p.def(MODULE_REQUIREMENT).aka(MODULE_REQUIREMENT_AKA).repeatable().init("");
		p.def(MODULE_PREDECESSOR).aka(MODULE_PREDECESSOR_AKA).repeatable().init("");
		p.def(MODULE_CONFIG).aka(MODULE_CONFIG_AKA).init("");
	}

	/**
	 * Build a module definition from an <code>Args</code> object.
	 * 
	 * @param p
	 *            the object taking parameters
	 * @return a module specification
	 * @throws IllegalArgumentException
	 *             if there is an error with parameters
	 */
	@SuppressWarnings("unchecked")
	protected MD build(Args p) {
		// get configuration in raw mode to avoid resolution errors
		return (MD) new ModuleDefinition<M>(p.get(MODULE_NAME), p.get(MODULE_CLASS), p.split(MODULE_REQUIREMENT), p.split(MODULE_PREDECESSOR), p.getVal(MODULE_CONFIG).rawValue());
	}

}
