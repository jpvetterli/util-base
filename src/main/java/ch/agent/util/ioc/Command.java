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
 *            the type of the underlying module object
 */
public interface Command<T> {

	/**
	 * Get the command name. The command name is unique within a module.
	 * 
	 * @return a non-null and non-empty string
	 */
	String getName();
	
	/**
	 * Execute the command with the given parameters. If there is a critical
	 * problem, which makes further work meaningless or harmful, the method
	 * should throw an exception, checked or unchecked.
	 * 
	 * @param parameters
	 *            a string containing parameters
	 * @throws Exception
	 *             to signal critical problems
	 */
	void execute(String parameters) throws Exception;

}
