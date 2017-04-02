package ch.agent.util.ioc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.agent.util.base.Misc;

/**
 * A configuration is an immutable object encapsulating the information
 * necessary to set up and launch a system of modules.
 * <p>
 * The configuration consists of:
 * <ul>
 * <li>a list of module definitions in a sequence compatible with dependency
 * constraints,
 * <li>an <em>execution</em> specification.
 * </ul>
 * 
 * @param <D>
 *            the module definition type
 * @param <M>
 *            the module type
 */
public class Configuration<D extends ModuleDefinition<M>, M extends Module<?>> {

	int review_javadoc; // configuration
	
	private final String execution;
	private final Map<String, D> modules;

	/**
	 * Constructor. Modules must be provided in a valid dependency sequence.
	 * 
	 * @param definitions
	 *            the list of module definitions in valid initialization sequence
	 * @param execution
	 *            the execution specification or null
	 */
	public Configuration(List<D> definitions, String execution) {
		this.execution = Misc.isEmpty(execution) ? null : execution;
		this.modules = new LinkedHashMap<String, D>();
		for (D def : definitions) {
			this.modules.put(def.getName(), def);
		}
	}

	/**
	 * Get a copy of all module names in valid initialization sequence.
	 * 
	 * @return a list of module names
	 */
	public List<String> getModuleNames() {
		return new ArrayList<String>(modules.keySet());
	}
	
	/**
	 * Get a copy of all module definitions in valid initialization sequence.
	 * 
	 * @return a list of module definitions
	 */
	public List<D> getModuleDefinitions() {
		return new ArrayList<D>(modules.values());
	}

	/**
	 * Return the number of module definitions.
	 * 
	 * @return a non-negative number
	 */
	public int getModuleCount() {
		return modules.size();
	}
	
	/**
	 * Get a module definition.
	 * 
	 * @param name
	 *            the module name
	 * @return the module specification or null if none such
	 */
	public D getModuleDefinition(String name) {
		return modules.get(name);
	}
	
	/**
	 * Return the <em>execution</em> specification. The execution specification
	 * is an opaque block of text which contains instructions on module commands
	 * to be executed after all modules have been initialized. The instruction
	 * for a given command can be extracted using the command name. The details
	 * of how to perform this extraction is not the responsibility of this
	 * object.
	 * 
	 * @return the execution string or null
	 */
	public String getExecution() {
		return execution;
	}
	
	/**
	 * Extract sub-configuration for a module. The sub-configuration includes
	 * the module and all its direct and indirect prerequisites. The execution
	 * specification, which belongs to the top-level configuration, is not
	 * included.
	 * 
	 * @param base
	 *            name of the base module of the sub-configuration
	 * @return a configuration
	 */
	public Configuration<D,M> extract(String base) {
		Set<String> scope = new HashSet<String>(); 
		add(getModuleDefinition(base), scope);
		List<D> extract = new ArrayList<D>(scope.size());
		for (D def : getModuleDefinitions()) {
			if (scope.contains(def.getName()))
				extract.add(def);
		}
		return new Configuration<D,M>(extract, null);
	}
	
	private void add(D def, Set<String> scope) {
		// no risk of stack overflow because original configuration has no cycle
		scope.add(def.getName());
		for (String pre : def.getPrerequisites()) {
			add(getModuleDefinition(pre), scope);
		}
	}


}
