package ch.agent.util.ioc;

/**
 * A command is an adapter allowing to perform operations with the underlying
 * object of a module in a standard way. A command belongs to a module and a
 * module can have zero or more commands. A module and all its commands have the
 * same underlying object.
 * <p>
 * It is important that a command behaves in <em>non-modal</em> fashion. For
 * this it should directly support all its relevant parameters and should not
 * rely on parameters set with other commands. On the other hand it can rely on
 * parameters set during configuration, if these cannot be modified by another
 * command.
 * 
 * @param <T>
 *            the type of the underlying object
 */
public interface Command<T> {

	/**
	 * Get the module to which the command belongs.
	 * 
	 * @return a non-null module
	 */
	Module<T> getModule();

	/**
	 * Get the command name.
	 * 
	 * @return a non-null and non-empty string
	 */
	String getName();

	/**
	 * Execute the command with the given parameters.
	 * 
	 * @param parameters
	 *            a string containing parameters
	 * @throws Exception anything can happen during execution
	 */
	void execute(String parameters) throws Exception;

}
