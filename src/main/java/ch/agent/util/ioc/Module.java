package ch.agent.util.ioc;


/**
 * A module is an adapter allowing an underlying object to be manipulated in a
 * standard way. Modules are created and managed by a {@link Container}. The
 * container expects a module constructor to have a single parameter: the module
 * name.
 * <p>
 * In the life-cycle of the module the following methods are called in sequence:
 * <ul>
 * <li>The constructor.
 * <li>{@link #configure}, exactly once
 * <li>{@link #add}, zero or more times
 * <li>{@link #registerCommands}, exactly once 
 * <li>{@link #initialize}, exactly once
 * <li>commands, zero or more times
 * <li>{@link #shutdown}, exactly once
 * </ul>
 * The design for adding and removing modules dynamically, and for updating the configuration
 * is not ready. 
 * 
 * @param <T>
 *            the type of the underlying object
 */
public interface Module<T> {

	/**
	 * Get the module name.
	 * 
	 * @return a non-null and non-empty string
	 */
	String getName();
	
	/**
	 * Configure the module. 
	 * 
	 * @param specs
	 *            a string containing specifications
	 * @throws IllegalStateException
	 *             if the method is called more than once
	 * @throws InvalidArgumentException
	 *             if there are errors in the specification
	 */
	void configure(String specs) throws IllegalStateException, IllegalArgumentException;

	/**
	 * Register module commands with the registry.
	 * 
	 * @param registry
	 *            the command registry
	 */
	void registerCommands(CommandRegistry registry);
	
	/**
	 * Add a required module. This method is called for all modules, as required
	 * by the configuration, after {@link #configure} is called.
	 * 
	 * @param module
	 *            a module, already parameterized
	 * @return true if the module is accepted else false
	 */
	boolean add(Module<?> module);
	
	/**
	 * Get the underlying object implementing the module.
	 * 
	 * @return an object
	 * @throws IllegalStateException
	 *             if the object is not yet available
	 */
	T getObject();
	
	/**
	 * Initialize the module.
	 */
	void initialize();
	
	/**
	 * Stop execution of the underlying object implementing the module.
	 * <p>
	 * This method can be called only once. It is possible that the method will
	 * be called even after the module has thrown an exception.
	 */
	void shutdown();
	
}
