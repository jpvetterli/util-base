package ch.agent.util.ioc;

import ch.agent.util.args.Args;
import ch.agent.util.base.Misc;

/**
 * A minimal abstract implementation of the {@link Module} interface. It
 * provides a useful implementation of {@link #getName} and
 * {@link #configure(String)}, leaves {@link #getObject} to subclasses, and
 * provides dummy implementations of all other methods. It adds two methods to
 * be overriden by actual modules: {@link #defineConfiguration} and
 * {@link #configure(Args)}.
 * 
 * @param <T>
 *            the type of the underlying object
 */
public abstract class AbstractModule<T> implements Module<T> {

	private String name;
	private boolean configure;
	
	public AbstractModule(String name) {
		if (Misc.isEmpty(name))
			throw new IllegalArgumentException("name null or emtpy");
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * Define configuration parameters.
	 * <p>
	 * Subclasses should usually call the super method first, before adding
	 * their definitions. The method always returns a new object.
	 * 
	 * @return the configuration object
	 */
	public Args defineConfiguration() {
		return new Args();
	}
	
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
	public void configure(Args config) {
	}

	@Override
	public void configure(String specs) {
		if (configure)
			throw new RuntimeException("bug found: configure called again, module: " + getName());
		configure = true;
		Args config = defineConfiguration();
		config.parse(specs);
		configure(config);
	}

	@Override
	public void registerCommands(CommandRegistry registry) {
	}

	@Override
	public boolean add(Module<?> module) {
		return false;
	}

	@Override
	public void initialize() {
	}

	@Override
	public void shutdown() {
	}

}
