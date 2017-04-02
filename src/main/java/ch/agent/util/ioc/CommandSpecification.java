package ch.agent.util.ioc;

/**
 * A command specification consists of a command and of a parameter string.
 */
public class CommandSpecification {

	private final Command<?> command;
	private final String parameters;
	
	/**
	 * Constructor.
	 * 
	 * @param command the command 
	 * @param parameters an opaque string of parameters
	 */
	public CommandSpecification(Command<?> command, String parameters) {
		super();
		this.command = command;
		this.parameters = parameters;
	}
	
	/**
	 * Get the command.
	 * 
	 * @return a command
	 */
	public Command<?> getCommand() {
		return command;
	}
	
	/**
	 * Get the parameter string.
	 * 
	 * @return a string
	 */
	public String getParameters() {
		return parameters;
	}
}
