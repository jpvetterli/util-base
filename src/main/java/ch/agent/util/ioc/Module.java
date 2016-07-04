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
 * The design for adding and removing modules dynamically, and for updating the
 * configuration is not ready.
 * <p>
 * <b>IMPORTANT:</b> An actual module should carefully document in the comment of
 * its {@link #add} method the module types that it requires and whether they
 * are mandatory or optional.
 * 
 * 
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
	 * <p>
	 * This method can be called only once. 
	 * 
	 * @param specs
	 *            a string containing specifications
	 * @throws IllegalStateException
	 *             if called more than once
	 * @throws InvalidArgumentException
	 *             if there are errors in the specification
	 */
	void configure(String specs);

	/**
	 * Register module commands with the registry.
	 * <p>
	 * This method may be called only once.
	 * 
	 * @param registry
	 *            the command registry
	 * @throws IllegalStateException
	 *             if called more than once
	 */
	void registerCommands(CommandRegistry registry);
	
	/**
	 * Add a required module. This method is used to add any module declared
	 * with the <em>requirement</em> parameter of the <em>module</em> statement in
	 * the container configuration.
	 * 
	 * @param module
	 *            a module, already parameterized
	 * @return true if the module is accepted else false
	 */
	boolean add(Module<?> module);
	
	/**
	 * Get the underlying object implementing the module. Some modules don't
	 * have an underlying object distinct from themselves and return null.
	 * 
	 * @return an object
	 * @throws IllegalStateException
	 *             if the object is not yet available
	 */
	T getObject();
	
	/**
	 * Initialize the module.
	 * <p>
	 * This method may be called only once.
	 * 
	 * @throws IllegalStateException
	 *             if called more than once
	 * @throws Exception
	 *             anything can happen during initialization
	 */
	void initialize() throws Exception;
	
	/**
	 * Stop execution of the underlying object implementing the module.
	 * <p>
	 * This method may be called only once. It is possible that the method will
	 * be called even after the module has thrown an exception.
	 * 
	 * @throws IllegalStateException
	 *             if called more than once
	 */
	void shutdown();

}
