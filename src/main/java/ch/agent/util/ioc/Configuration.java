package ch.agent.util.ioc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
 * <li>a <em>configuration</em> specification, and
 * <li>an <em>execution</em> specification.
 * </ul>
 * 
 * @param <T>
 *            the module definition type
 */
public class Configuration<T extends ModuleDefinition> implements Iterable<T> {

	private final String configuration;
	private final String execution;

	private final Map<String, Integer> names;
	private final List<T> modules;

	/**
	 * Constructor. The modules in the list must be in a valid dependency
	 * sequence.
	 * 
	 * @param modules
	 *            list of module definitions
	 * @param configuration
	 *            the configuration specification
	 * @param execution
	 *            the execution specification
	 */
	public Configuration(List<T> modules, String configuration, String execution) {
		this.modules = new ArrayList<T>(modules);
		this.configuration = configuration;
		this.execution = execution;
		names = new HashMap<String, Integer>();
		for (int i = 0; i < modules.size(); i++) {
			names.put(modules.get(i).getName(), i);
		}
	}

	@Override
	public Iterator<T> iterator() {
		return modules.iterator();
	}

	/**
	 * Get a copy of the module definitions list.
	 * 
	 * @return a list of module definitions
	 */
	public List<T> getModuleDefinitions() {
		return new ArrayList<T>(modules);
	}
	
	/**
	 * Get a module definition.
	 * 
	 * @param name
	 *            the module name
	 * @return the module specification or null if none such
	 */
	public ModuleDefinition get(String name) {
		Integer i = names.get(name);
		return i == null ? null : modules.get(i);
	}
	
	/**
	 * Return the <em>configuration</em> specification. The configuration
	 * specification is an opaque block of text which contains configuration
	 * details for all modules. The configuration for a given module can be
	 * extracted using the module name. The details of how to perform
	 * this extraction is not the responsibility of this object.
	 * 
	 * @return a string, possibly empty, not null
	 */
	public String getConfiguration() {
		return configuration;
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
