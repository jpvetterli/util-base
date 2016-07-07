package ch.agent.util.ioc;

import static ch.agent.util.STRINGS.msg;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

import ch.agent.util.STRINGS.U;
import ch.agent.util.base.Misc;

/**
 * A module definition is an immutable object which encapsulates the information
 * needed to create and configure a module. The definition consists of:
 * <ul>
 * <li>a name, used to identify the module within a system of modules,
 * <li>a class name, used to create the module,
 * <li>zero or more requirements, and
 * <li>zero or more predecessors.
 * 
 * The module class must have a constructor taking the module name as parameter.
 * Requirements and predecessors are names of other modules in the system. Such
 * modules are prerequisites and must be initialized before the module itself.
 * Requirements are added to the module using {@link Module#add} but
 * predecessors are not.
 * 
 */
public class ModuleDefinition {
	
	private final String name;
	private final String className;
	private final String[] req; // module names required by this module
	private final String[] pred; // module names preceding but not required
	
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
	 * @throws ConfigurationException
	 *             if something is wrong
	 */
	public ModuleDefinition(String name, String className, String[] required, String[] predecessors) {
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
	}
	
	/**
	 * Create the module. The module is created using a constructor taking a
	 * single argument: the module name.
	 * 
	 * @return a module object
	 * @throws ConfigurationException if creation fails
	 */
	protected Module<?> create() {
		try {
			@SuppressWarnings("unchecked")
			Class<? extends Module<?>> classe = (Class<? extends Module<?>>) Class.forName(className);
			Constructor<? extends Module<?>> constructor = classe.getConstructor(String.class);
			return (Module<?>) constructor.newInstance(getName());
		} catch (Exception e) {
			throw new ConfigurationException(msg(U.C03, getName(), className), e);
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
	 * @return a non-null string
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Get the names of required modules.
	 * 
	 * @return an array of strings
	 */
	public String[] getRequirements() {
		return req;
	}
	
	/**
	 * Get the names of predecessor modules.
	 * 
	 * @return an array of strings
	 */
	public String[] getPredecessors() {
		return pred;
	}

	@Override
	public String toString() {
		return getName();
	}
	
}
