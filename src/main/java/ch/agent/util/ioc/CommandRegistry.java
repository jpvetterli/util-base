package ch.agent.util.ioc;

/**
 * Interface implemented by an object which keeps track of commands.
 */
public interface CommandRegistry {

	/**
	 * Register a command. The method is called by a module to register a
	 * command. When the command name is already in use, it will be modified by
	 * the registry to achieve uniqueness of the command name. The actual
	 * command name is returned.
	 * 
	 * @param command
	 *            a command
	 * @return the actual command name, which is possibly the preferred name,
	 *         but possibly not
	 * @throws IllegalStateException
	 *             if the command was already registered by the same module
	 */
	String register(Command<?> command);
	
}
