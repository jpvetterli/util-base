package ch.agent.util.ioc;

/**
 * Interface implemented by an object which keeps track of commands.
 */
public interface CommandRegistry {

	/**
	 * Register a command. The method is called by a module to register a
	 * command. The registry identifies the command using its full name.
	 * 
	 * @param command
	 *            a command
	 * @throws IllegalStateException
	 *             if a command with that full name was already registered
	 */
	void register(Command<?> command);
	
}
