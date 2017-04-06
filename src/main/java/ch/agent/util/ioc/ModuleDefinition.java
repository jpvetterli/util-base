package ch.agent.util.ioc;

import static ch.agent.util.STRINGS.msg;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.agent.util.STRINGS.U;
import ch.agent.util.base.Misc;

/**
 * A module definition is an immutable object which encapsulates the information
 * needed to create and configure a module. The definition consists of:
 * <ul>
 * <li>a name, used to identify the module within a system of modules,
 * <li>a class name, used to create the module,
 * <li>zero or more requirements,
 * <li>zero or more predecessors, and
 * <li>a configuration string.
 * </ul>
 * The module class must have a constructor taking the module name as parameter.
 * Requirements and predecessors are names of other modules in the system. Such
 * modules are prerequisites and must be initialized before the module itself.
 * Requirements are added to the module using {@link Module#add} but
 * predecessors are not.
 * 
 * @param <M>
 *            the module type
 */
public class ModuleDefinition<M extends Module<?>> {
	
	int review_javadoc; // configuration string
	
	private final String name;
	private final String className;
	private final String[] req; // module names required by this module
	private final String[] pred; // module names preceding but not required
	private final String configuration;
	
	/**
	 * Constructor.
	 * 
	 * @param name
	 *            the module name, not null
	 * @param className
	 *            the module class, not null
	 * @param required
	 *            array of required modules
	 * @param predecessors
	 *            array of predecessor modules
	 * @param configuration
	 *            a configuration string or null
	 * @throws ConfigurationException
	 *             if something is wrong
	 */
	public ModuleDefinition(String name, String className, String[] required, String[] predecessors, String configuration) {
		Misc.nullIllegal(name, "name null");
		Misc.nullIllegal(className, "className null");
		Set<String> duplicates = new HashSet<String>();
		for (String req : required) {
			if (name.equals(req))
				throw new ConfigurationException(msg(U.C06, name));
			if (!duplicates.add(req))
				throw new ConfigurationException(msg(U.C13, name, req));
		}
		for (String prec : predecessors) {
			if (name.equals(prec))
				throw new ConfigurationException(msg(U.C06, name));
			if (!duplicates.add(prec))
				throw new ConfigurationException(msg(U.C13, name, prec));
		}
		
		this.name = name;
		this.className = className;
		this.req = required;
		this.pred = predecessors;
		this.configuration = Misc.isEmpty(configuration) ? null : configuration;
	}
	
	/**
	 * Constructor.
	 * 
	 * @param original name
	 *            an existing module definition
	 */
	public ModuleDefinition(ModuleDefinition<M> original) {
		this.name = original.name;
		this.className = original.className;
		this.req = original.req;
		this.pred = original.pred;
		this.configuration = original.configuration;
	}

	/**
	 * Create the module. The module is created using a constructor taking a
	 * single argument: the module name.
	 * 
	 * @return a module object
	 * @throws ConfigurationException if creation fails
	 */
	protected M create() {
		try {
			@SuppressWarnings("unchecked")
			Class<M> classe = (Class<M>) Class.forName(getClassName());
			Constructor<M> constructor = classe.getConstructor(String.class);
			return constructor.newInstance(getName());
		} catch (Exception e) {
			throw new ConfigurationException(msg(U.C03, getName(), getClassName()), e);
		}
	}
	
	/**
	 * Create and configure the module using this definition. The module map
	 * provides all required modules. The steps performed are:
	 * <ul>
	 * <li>a new module is created
	 * <li>all required modules are added to the module
	 * <li>the configuration specification, if any, is passed to the module
	 * <li>the module registers zero or more commands if a registry is provided
	 * </ul>
	 * <p>
	 * IMPORTANT: this method can be called only once.
	 * 
	 * @param registry
	 *            configuration registry
	 * @return the module
	 * @throws ConfigurationException
	 *             in case of configuration failure
	 */
	public M configure(ConfigurationRegistry<M> registry) {
		M module = create();
		addRequiredModules(module, registry.getModules());
		if (getConfiguration() != null)
			module.configure(getConfiguration());
		for(Command<?> command : module.getCommands()) {
			registry.addUnique(module.getName(), command.getName());
		}
		return module;
	}
	
	/**
	 * Get the module name.
	 * 
	 * @return a non-null string
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Get the name of the module class.
	 * @return a non-null string
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Return the <em>configuration</em> specification. The configuration
	 * specification is an opaque block of text which contains instructions
	 * understood by the module. The configuration is used by the
	 * {@link #configure} method.
	 * 
	 * @return the configuration string or null
	 */
	public String getConfiguration() {
		return configuration;
	}

	/**
	 * Get a copy of the names of required modules.
	 * 
	 * @return an array of strings
	 */
	public String[] getRequirements() {
		String[] copy = new String[req.length];
		System.arraycopy(req, 0, copy, 0, req.length);
		return copy;
	}
	
	/**
	 * Get a copy of the names of predecessor modules.
	 * 
	 * @return an array of strings
	 */
	public String[] getPredecessors() {
		String[] copy = new String[pred.length];
		System.arraycopy(pred, 0, copy, 0, pred.length);
		return copy;
	}
	
	/**
	 * Return the array of all requirements and predecessors. The result is the
	 * concatenation of the results of {@link #getRequirements} and
	 * {@link #getPredecessors}
	 * 
	 * @return an array of names
	 */
	public String[] getPrerequisites() {
		return concat(req,  pred);
	}
	
	/**
	 * Add all modules required. Required modules must be available in the map.
	 * The map can contain other modules, it is not used to decide if a module
	 * is required.
	 * 
	 * @param requiring
	 *            the requiring module
	 * @param modules
	 *            map with at least the required modules
	 * @throws ConfigurationException
	 *             if required modules are missing or are rejected
	 */
	protected void addRequiredModules(M requiring, Map<String, M> modules) {
		List<String> missing = new ArrayList<String>();
		List<String> rejected = new ArrayList<String>();
		for (String name : getRequirements()) {
			M required = modules.get(name);
			if (required == null)
				missing.add(name);
			else {
				if (!requiring.add(required))
					rejected.add(name);
			}
		}
		if (missing.size() > 0 || rejected.size() > 0) {
			String message;
			if (missing.size() == 0)
				message = msg(U.C52, requiring.getName(), Misc.join("\", \"", rejected));
			else if (rejected.size() == 0)
				message = msg(U.C53, requiring.getName(), Misc.join("\", \"", missing));
			else	
				message = msg(U.C54, requiring.getName(), Misc.join("\", \"", rejected), Misc.join("\", \"", missing));
			throw new ConfigurationException(message);
		}
	}

	protected String[] concat(String[] arr1, String[] arr2) {
		String[] c = new String[arr1.length + arr2.length];
		System.arraycopy(arr1, 0, c, 0, arr1.length);
		System.arraycopy(arr2, 0, c, arr1.length, arr2.length);
		return c;
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
}
