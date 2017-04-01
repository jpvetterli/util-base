package ch.agent.util.ioc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
	 *            the execution specification
	 */
	public Configuration(List<D> definitions, String execution) {
		this.execution = execution;
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
	public ModuleDefinition<M> getModuleDefinition(String name) {
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
	 * @return a string, possibly empty, not null
	 */
	public String getExecution() {
		return execution;
	}

}
