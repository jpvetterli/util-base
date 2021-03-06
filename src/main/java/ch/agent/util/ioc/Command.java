package ch.agent.util.ioc;

/**
 * A command is an adapter allowing to perform operations on a module's
 * underlying object. A command belongs to a {@link Module} and a module can
 * have zero or more commands. A module and all its commands have the same
 * underlying object.
 * <p>
 * It is important that a command behaves in <em>non-modal</em> fashion. For
 * this it should provide all relevant parameters and should not
 * rely on parameters set with other commands. On the other hand, it can rely on
 * those parameters which can only be set during module configuration.
 * 
 * @param <T>
 *            the type of the underlying module object
 */
public interface Command<T> {

	/**
	 * Test if it is a parameterless command.
	 * 
	 * @return true if it is a parameterless command
	 */
	boolean isParameterless();

	/**
	 * Execute the command with the given parameters. If there is a critical
	 * problem, which makes further work meaningless or harmful, the method
	 * should throw an exception, checked or unchecked.
	 * 
	 * @param name
	 *            the non-empty name used to address the command
	 * @param parameters
	 *            a string containing parameters
	 * @throws IllegalArgumentException
	 *             if there is a problem with the parameters
	 * @throws Exception
	 *             if there is a critical problem during actual execution
	 */
	void execute(String name, String parameters) throws Exception;

}
