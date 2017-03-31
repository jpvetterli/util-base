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
 *   predecessor*=<em>module name</em> (abbreviated pred)
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
	
	private Args parameters;
	
	public ModuleDefinitionBuilder() {
	}

	/**
	 * Build a module definition from a textual specification.
	 * <p>
	 * Note: clients with an array of specifications can concatenate elements of
	 * the array with white space as separators.
	 * 
	 * @param specification
	 *            a string containing the specification
	 * @return a module definition
	 * @throws ConfigurationException
	 *             if something is wrong
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
	 * Define the parameter syntax.
	 * 
	 * @param p
	 *            the object taking parameters
	 */
	protected void defineSyntax(Args p) {
		p.def(MODULE_NAME);
		p.def(MODULE_CLASS);
		p.defList(MODULE_REQUIREMENT).aka(MODULE_REQUIREMENT_AKA);
		p.defList(MODULE_PREDECESSOR).aka(MODULE_PREDECESSOR_AKA);
	}

	/**
	 * Build a module definition from an object encapsulating parameter values.
	 * 
	 * @param p
	 *            the object taking parameters
	 * @return a module specification
	 * @throws ConfigurationException
	 *             if something is wrong
	 */
	@SuppressWarnings("unchecked")
	protected MD build(Args p) {
		return (MD) new ModuleDefinition<M>(
				p.get(MODULE_NAME), 
				p.get(MODULE_CLASS), 
				p.getVal(MODULE_REQUIREMENT).stringArray(), 
				p.getVal(MODULE_PREDECESSOR).stringArray());
	}

}
