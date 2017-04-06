package ch.agent.util.ioc;

import java.util.Collection;



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
 * <li>{@link #add(Module)}, zero or more times
 * <li>{@link #initialize}, exactly once
 * <li>commands, zero or more times
 * <li>{@link #shutdown}, exactly once
 * </ul>
 * Commands are added with {@link #add(Command)}.
 * <p>
 * <b>IMPORTANT:</b> An actual module should carefully document in the comment of
 * its {@link #add(Module)} method the module types that it requires and whether they
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
	 * @throws ConfigurationException
	 *             if there are errors during configuration
	 */
	void configure(String specs);
	
	/**
	 * Execute a module command.
	 * <p>
	 * This method can be called many times.
	 * 
	 * @param name
	 *            the simple command name
	 * @param parameters
	 *            an opaque string containing command parameters
	 * @throws ConfigurationException
	 *             if the command is unknown
	 * @throws IllegalArgumentException
	 *             if there is an error when parsing parameters
	 * @throws Exception
	 *             if there is an error when executing the command
	 */
	void execute(String name, String parameters) throws Exception;

	/**
	 * Add a command. Command names are unique within a module.
	 * Commands are executed by calling {@link #execute} with
	 * the command name and an opaque parameter string. 
	 * 
	 * @param command a command
	 */
	void add(Command<?> command);
	
	/**
	 * Return all commands. Once this method has been used, adding more commands
	 * with {@link #add(Command)} is forbidden.
	 * 
	 * @return a collection of commands, possibly empty but never null
	 */
	Collection<Command<?>> getCommands();
	
	/**
	 * Add a required module. This method is used to add any prerequisite
	 * module. All commands of the added module will be added to this module
	 * with the name of the added module prefixed to the command names. For
	 * example if adding a module named <em>foo</em> which has a command named
	 * <em>bar</em>, this module will have a command named <em>foo.bar</em>.
	 * 
	 * @param module
	 *            a module, initialized
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
	 * Initialize the module. If there is a critical problem, which makes
	 * further work meaningless or harmful, the method should throw an
	 * exception, checked or unchecked. In such a case, the module should clean
	 * up after itself, as {@link #shutdown} will not be called.
	 * <p>
	 * This method may be called only once.
	 * 
	 * @throws IllegalStateException
	 *             if called more than once
	 * @throws Exception
	 *             to signal critical problems
	 */
	void initialize() throws Exception;
	
	/**
	 * Close the module and the underlying object as the system is shutting
	 * down.
	 * <p>
	 * This method may be called only once. The method will not be called if
	 * {@link #initialize} was never called, or was called, but threw an
	 * exception. However, the method will be called, if possible, when the
	 * module or underlying object throw an exception at a later point than
	 * initialization.
	 * 
	 * @throws IllegalStateException
	 *             if called more than once
	 */
	void shutdown();

}
