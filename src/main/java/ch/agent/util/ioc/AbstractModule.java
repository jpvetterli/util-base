package ch.agent.util.ioc;

import static ch.agent.util.STRINGS.msg;

import java.util.HashMap;
import java.util.Map;

import ch.agent.util.STRINGS.U;
import ch.agent.util.args.Args;
import ch.agent.util.base.Misc;

/**
 * A useful abstract implementation of the {@link Module} interface. It provides
 * implementations of most methods. Most subclasses probably need to provide
 * implementations {@link #getObject()}, {@link #add(Module)},
 * {@link #defineParameters(Args)} and {@link #configure(Args)} with either a
 * complete override or by extending the functionality.
 * 
 * @param <T>
 *            the type of the underlying object
 */
public abstract class AbstractModule<T> implements Module<T> {

	private final String name;
	private boolean configure;
	private boolean initialize;
	private boolean commandsLocked;
	private boolean shutdown;
	private Map<String, Command<?>> commandTable;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            the module name
	 * @throws IllegalArgumentException
	 *             if the name is empty
	 */
	public AbstractModule(String name) {
		if (Misc.isEmpty(name))
			throw new IllegalArgumentException(msg(U.C51));
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
	 * their own definitions. They should throw an
	 * {@link IllegalArgumentException} when something is wrong; this is usually
	 * achieved by not catching exceptions when using {@link Args} or
	 * {@link Args.Definition} methods.
	 * 
	 * @param config
	 *            the configuration object
	 * @throws IllegalArgumentException
	 *             as described in the comment
	 */
	public void defineParameters(Args config) {
	}

	/**
	 * Configure the module using an Args object.
	 * <p>
	 * Subclasses should usually call the super method first, before performing
	 * their own configuration.
	 * 
	 * @param config
	 *            the configuration object
	 * @throws IllegalArgumentException
	 *             if there is a problem with configuration parameters
	 */
	public void configure(Args config) {
	}

	@Override
	public void configure(String specs) {
		if (configure)
			throw new IllegalStateException(msg(U.C61, getName()));
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
			throw new IllegalArgumentException(msg(U.C17, name, getName()));
		command.execute(parameters);
	}

	@Override
	public void add(String name, Command<?> command) {
		add(name, false, command);
	}
	
	private void add(String name, boolean composite, Command<?> command) {
		if (Misc.isEmpty(name))
			throw new IllegalArgumentException(msg(U.C51));
		if (!composite && name.indexOf(CommandSpecification.NAME_SEPARATOR) > 0)
			throw new IllegalArgumentException(msg(U.C50, name));
		if (commandsLocked)
			throw new IllegalStateException(msg(U.C56, name, getName()));
		if (commandTable.put(name, command) != null)
			throw new IllegalArgumentException(msg(U.C14, name, getName()));
	}

	@Override
	public Map<String, Command<?>> getCommands() {
		commandsLocked = true;
		return commandTable;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation adds all commands of the module specified and returns
	 * true, which means the module is accepted as a requirement.
	 * <p>
	 * As a consequence, when a subclass does not provide an implementation for
	 * {@link #add(Module)} all requirements are accepted and all their commands
	 * become available. If this is not the intention, the subclass must
	 * override the method to return false.
	 * <p>
	 * All commands of the required module are added to this module with the
	 * name of the required module prefixed to the command names. For example
	 * after adding module <em>foo</em> with command <em>bar,</em> this module
	 * will have a command named <em>foo.bar.</em>
	 */
	@Override
	public boolean add(Module<?> module) {
		for (Map.Entry<String, Command<?>> entry : module.getCommands().entrySet()) {
			String compositeName = module.getName() + CommandSpecification.NAME_SEPARATOR + entry.getKey();
			add(compositeName, true, entry.getValue());
		}
		return true;
	}

	@Override
	public void initialize() throws Exception {
		if (initialize)
			throw new IllegalStateException(msg(U.C62, getName()));
		initialize = true;
	}

	@Override
	public void shutdown() {
		if (shutdown)
			throw new IllegalStateException(msg(U.C63, getName()));
		shutdown = true;
	}

	@Override
	public String toString() {
		return "module " + getName();
	}

}
