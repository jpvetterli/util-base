package ch.agent.util.ioc;

import java.util.Map;

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
	
	/**
	 * Get the command map. The keys are command names as defined by
	 * {@link Command#getName()} if possible, or the same prefixed with
	 * {@link Module#getName()} and a period to achieve name uniqueness
	 * within a system.
	 * 
	 * @return the command map
	 */
	Map<String, Command<?>> getCommands();
	
}
