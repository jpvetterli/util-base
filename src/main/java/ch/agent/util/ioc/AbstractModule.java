package ch.agent.util.ioc;

import static ch.agent.util.STRINGS.msg;

import java.util.HashMap;
import java.util.Map;

import ch.agent.util.STRINGS.U;
import ch.agent.util.args.Args;
import ch.agent.util.base.Misc;

/**
 * A minimal abstract implementation of the {@link Module} interface. It
 * provides a useful implementation of {@link #getName} and
 * {@link #configure(String)}, leaves {@link #getObject} to subclasses, and
 * provides dummy implementations of all other methods. It adds two methods to
 * be overriden by actual modules: {@link #defineParameters} and
 * {@link #configure(Args)}.
 * <p>
 * When subclassing, the default implementation of {@link #configure(String)},
 * {@link #initialize}, {@link #registerCommands}, and {@link #shutdown} perform
 * a check that the method is called only once. Method
 * {@link #configure(String)} does not need itself to be subclassed, since it
 * splits the work into two easier methods, which need to be subclassed in 
 * modules with configuration parameters:
 * {@link #defineParameters(Args)} and {@link #configure(Args)}.
 * 
 * 
 * @param <T>
 *            the type of the underlying object
 */
public abstract class AbstractModule<T> implements Module<T> {

	private String name;
	private boolean configure;
	private boolean initialize;
	private boolean register;
	private boolean shutdown;
	private Map<String, Command<?>> commandTable;
	
	public AbstractModule(String name) {
		if (Misc.isEmpty(name))
			throw new ConfigurationException(msg(U.C51));
		this.name = name;
		this.commandTable = new HashMap<String, Command<?>>();
	}

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public T getObject() {
		return null;
	}

	/**
	 * Define configuration parameters.
	 * <p>
	 * Subclasses should usually call the super method first, before adding
	 * their own definitions.
	 * 
	 * @param config
	 *            the configuration object
	 */
	public void defineParameters(Args config) {}
	
	/**
	 * Configure the module.
	 * <p>
	 * Subclasses should usually call the super method first, before performing
	 * their own configuration.
	 * 
	 * @param config
	 *            the configuration object
	 * @throws IllegalArgumentException
	 *             if there is an error
	 */
	public void configure(Args config) {}

	@Override
	public void configure(String specs) {
		if (configure)
			throw new IllegalStateException("bug found: #configure called again, module: " + getName());
		configure = true;
		Args config = new Args();
		defineParameters(config);
		config.parse(specs);
		configure(config);
	}
	
	@Override
	public void execute(String name, String parameters) throws Exception {
		Command<?> command = commandTable.get(name);
		if (command == null)
			throw new ConfigurationException(msg(U.C17, name, getName()));
		command.execute(parameters);
	}

	public void add(Command<?> command) {
		if (register)
			throw new IllegalStateException(msg(U.C56, command.getName(), getName()));
		if (commandTable.put(command.getName(), command) != null)
			throw new ConfigurationException(msg(U.C14, command.getName(), getName()));

	}
	
	@Override
	public void registerCommands(ConfigurationRegistry<?> registry) {
		if (register)
			throw new IllegalStateException("bug found: #registerCommands called again, module: " + getName());
		for (Command<?> command : commandTable.values())
			registry.addUnique(getName(), command.getName());
		register = true;
	}

	@Override
	public boolean add(Module<?> module) {
		return false;
	}

	@Override
	public void initialize() throws Exception {
		if (initialize)
			throw new IllegalStateException("bug found: #initialize called again, module: " + getName());
		initialize = true;
	}

	@Override
	public void shutdown() {
		if (shutdown)
			throw new IllegalStateException("bug found: #shutdown called again, module: " + getName());
		shutdown = true;
	}

	@Override
	public String toString() {
		return "module " + getName();
	}
	
}
