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
 * <p>
 * When subclassing, the default implementation of {@link #configure(String)},
 * {@link #initialize}, {@link #registerCommands}, and {@link #shutdown} perform
 * a check that the method is called only once. Method
 * {@link #configure(String)} does not need itself to be subclassed, since it
 * splits the work into two easier methods, which need to be subclassed in 
 * modules with configuration parameters:
 * {@link #defineConfiguration(Args)} and {@link #configure(Args)}.
 * 
 * 
 * @param <T>
 *            the type of the underlying object
 */
public abstract class AbstractModule<T> implements Module<T> {

	private String name;
	private boolean configure;
	private boolean initialize;
	private boolean registerCommands;
	private boolean shutdown;
	
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
	 * 
	 * @param config the configuration object
	 */
	public void defineConfiguration(Args config) {
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
		Args config = new Args();
		defineConfiguration(config);
		config.parse(specs);
		configure(config);
	}

	@Override
	public void registerCommands(CommandRegistry registry) {
		if (registerCommands)
			throw new RuntimeException("bug found: #registerCommands called again, module: " + getName());
		registerCommands = true;
	}

	@Override
	public boolean add(Module<?> module) {
		return false;
	}

	@Override
	public void initialize() throws Exception {
		if (initialize)
			throw new RuntimeException("bug found: #initialize called again, module: " + getName());
		initialize = true;
	}

	@Override
	public void shutdown() {
		if (shutdown)
			throw new RuntimeException("bug found: #shutdown called again, module: " + getName());
		shutdown = true;
	}

}
