package ch.agent.util.ioc;

/**
 * Interface implemented by an object which keeps track of commands.
 */
public interface CommandRegistry {

	/**
	 * Register a command. The method is called by a module to register a
	 * command. The registry is allowed to use a name different from
	 * {@link Command#getName} for the command. The actual command name is
	 * returned.
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
