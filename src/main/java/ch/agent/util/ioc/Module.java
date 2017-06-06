package ch.agent.util.ioc;

import java.util.Map;

/**
 * A module is an adapter allowing an underlying object to be manipulated in a
 * standard way. Modules are created and managed by a {@link Container}. The
 * container expects a module constructor to have a single parameter: the module
 * name.
 * <p>
 * The life-cycle events of a module are, in sequence:
 * <ul>
 * <li>Creation, with the constructor.
 * <li>Configuration, with {@link #configure(String)} invoked exactly once.
 * <li>Setting of prerequisites, with {@link #add(Module)} invoked zero or more
 * times.
 * <li>Initialization, with {@link #initialize()} invoked exactly once.
 * <li>Execution of commands, with {@link #execute(String, String)} invoked a
 * number of times.
 * <li>Shutdown, with {@link #shutdown()} invoked exactly once.
 * </ul>
 * <p>
 * Commands are created by the module constructor or during configuration. 
 * They are added with {@link #add(String, Command)}. 
 * <p>
 * A concrete module should document in the comment of its
 * {@link #add(Module)} method the module types it requires and whether
 * they are mandatory or optional.
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
	 * @throws IllegalArgumentException
	 *             if there are errors during configuration
	 * @throws IllegalStateException
	 *             if called more than once
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
	 * @throws IllegalArgumentException
	 *             if the command is unknown or if there is an error when
	 *             parsing parameters
	 * @throws Exception
	 *             to signal a critical problem during actual execution
	 */
	void execute(String name, String parameters) throws Exception;

	/**
	 * Add a command. Command names are unique within a module. Commands are
	 * executed by calling {@link #execute(String, String)} with the command
	 * name and an opaque parameter string. This method cannot be used after
	 * {@link #getCommands()} has been called.
	 * 
	 * @param name
	 *            non-null name to use for the command
	 * 
	 * @param command
	 *            a command
	 * @throws IllegalArgumentException
	 *             if there is already a command with the same name
	 * @throws IllegalStateException
	 *             if it is too late to add commands
	 */
	void add(String name, Command<?> command);

	/**
	 * Get all commands. Once this method has been used, adding more commands
	 * with {@link #add(String, Command)} is forbidden.
	 * 
	 * @return a map of commands keyed by name, possibly empty but never null
	 */
	Map<String, Command<?>> getCommands();

	/**
	 * Add a required module. This method is used to add any prerequisite
	 * module.
	 * 
	 * @param module
	 *            a module, initialized
	 * @return true if the module is accepted else false
	 */
	boolean add(Module<?> module);

	/**
	 * Get the underlying module object. Some modules don't have an underlying
	 * object distinct from themselves and return null. In many implementations,
	 * calling this method before the module has been initialized throws an
	 * <code>IllegalStateException</code>. To avoid this problem, get the object
	 * from the required module in {@link #initialize()} and not in
	 * {@link #add(Module)}.
	 * 
	 * @return the underlying object or null
	 * @throws IllegalStateException
	 *             if the object is not yet available
	 */
	T getObject();

	/**
	 * Initialize the module. If there is a critical problem, which makes
	 * further work meaningless or harmful, the method should throw an
	 * exception, checked or unchecked. In such a case, the module should clean
	 * up after itself, as {@link #shutdown()} will not be called.
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
	 * module throws an exception at a later point than initialization.
	 * 
	 * @throws IllegalStateException
	 *             if called more than once
	 */
	void shutdown();

}
