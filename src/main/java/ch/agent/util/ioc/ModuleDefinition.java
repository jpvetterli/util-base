package ch.agent.util.ioc;

import static ch.agent.util.STRINGS.msg;

import java.io.Serializable;
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
 * needed to create and configure a module. The module class must have a
 * constructor taking the module name as parameter. Requirements and
 * predecessors are names of other modules in the system. Such modules are
 * prerequisites and must be initialized before the module itself. Requirements
 * are added to the module using {@link Module#add(Module)} but predecessors are
 * not.
 * 
 * @param <M>
 *            the module type
 */
public class ModuleDefinition<M extends Module<?>> implements Serializable {

	private static final long serialVersionUID = -7103451839673077227L;

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
	 *            the non-empty module name
	 * @param className
	 *            the module class, not null
	 * @param required
	 *            array of required modules
	 * @param predecessors
	 *            array of predecessor modules
	 * @param configuration
	 *            a configuration string or null
	 * @throws IllegalArgumentException
	 *             if something is wrong with the arguments
	 */
	public ModuleDefinition(String name, String className, String[] required, String[] predecessors, String configuration) {
		if (Misc.isEmpty(name))
			throw new IllegalArgumentException(msg(U.C51));
		Misc.nullIllegal(className, "className null");
		Set<String> duplicates = new HashSet<String>();
		for (String req : required) {
			if (name.equals(req))
				throw new IllegalArgumentException(msg(U.C06, name));
			if (!duplicates.add(req))
				throw new IllegalArgumentException(msg(U.C13, name, req));
		}
		for (String prec : predecessors) {
			if (name.equals(prec))
				throw new IllegalArgumentException(msg(U.C06, name));
			if (!duplicates.add(prec))
				throw new IllegalArgumentException(msg(U.C13, name, prec));
		}

		this.name = name;
		this.className = className;
		this.req = required;
		this.pred = predecessors;
		this.configuration = Misc.isEmpty(configuration) ? null : configuration;
	}

	/**
	 * Create the module. The module is created using a constructor taking 
	 * the module name as argument.
	 * 
	 * @return a module object
	 * @throws IllegalArgumentException
	 *             if creation fails
	 */
	public M create() {
		try {
			@SuppressWarnings("unchecked")
			Class<M> classe = (Class<M>) Class.forName(getClassName());
			Constructor<M> constructor = classe.getConstructor(String.class);
			return constructor.newInstance(getName());
		} catch (Exception e) {
			throw new IllegalArgumentException(msg(U.C03, getName(), getClassName()), e);
		}
	}

	/**
	 * Configure the module using this definition. The registry provides all
	 * required modules. The steps performed are:
	 * <ul>
	 * <li>all required modules are added to the module
	 * <li>the module is configured using the configuration string
	 * <li>the module inserts zero or more commands into the registry.
	 * </ul>
	 * <p>
	 * IMPORTANT: this method can be called only once.
	 * 
	 * @param module
	 *            the module to configure
	 * @param registry
	 *            configuration registry
	 * @throws IllegalArgumentException
	 *             in case of configuration failure
	 */
	public void configure(M module, ConfigurationRegistry<M> registry) {
		addRequiredModules(module, registry.getModules());
		if (getConfiguration() != null)
			module.configure(getConfiguration());
		for (Map.Entry<String, Command<?>> entry : module.getCommands().entrySet()) {
			registry.addUnique(new CommandSpecification(module.getName(), entry.getKey(), entry.getValue().isParameterless()));
		}
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
	 * 
	 * @return a non-null string
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Return the configuration specification. The configuration specification
	 * is an opaque block of text which contains instructions understood by the
	 * module. The configuration is used by the
	 * {@link #configure(Module, ConfigurationRegistry)} method.
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
	 * Return the names of all requirements and predecessors. The result is the
	 * concatenation of the results of {@link #getRequirements()} and
	 * {@link #getPredecessors()}
	 * 
	 * @return an array of names
	 */
	public String[] getPrerequisites() {
		return concat(req, pred);
	}

	/**
	 * Add all modules required. Required modules must be available in the map
	 * (the map can contain more modules than those required).
	 * 
	 * @param requiring
	 *            the requiring module
	 * @param modules
	 *            map with at least the required modules
	 * @throws IllegalArgumentException
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
			throw new IllegalArgumentException(message);
		}
	}

	private String[] concat(String[] arr1, String[] arr2) {
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
