package ch.agent.util.ioc;

/**
 * A command specification consists of module name and a command name.
 */
public class CommandSpecification {

	public static final String NAME_SEPARATOR = "."; 
	
	private final String command;
	private final String module;

	/**
	 * Constructor.
	 * 
	 * @param module
	 *            the module name or null for a built-in command
	 * @param command
	 *            the command name
	 */
	public CommandSpecification(String module, String command) {
		super();
		this.module = module;
		this.command = command;
	}

	/**
	 * Get the name of the command. The name combines the module name and the
	 * command name with a separating period, to achieve uniqueness in a system
	 * where module names are unique. Command {@code foo.bar} is the full name
	 * of command {@code bar} in module {@code foo}. For built-in commands the
	 * method returns only the command name because there is no module name.
	 * 
	 * @return a non-null and non-empty string
	 */
	public String getName() {
		return module == null ? command : module + NAME_SEPARATOR + command;
	}

	/**
	 * Get the module name.
	 * 
	 * @return a string or null for a built-in command
	 */
	public String getModule() {
		return module;
	}

	/**
	 * Get the command name.
	 * 
	 * @return a string
	 */
	public String getCommand() {
		return command;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((command == null) ? 0 : command.hashCode());
		result = prime * result + ((module == null) ? 0 : module.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CommandSpecification other = (CommandSpecification) obj;
		if (command == null) {
			if (other.command != null)
				return false;
		} else if (!command.equals(other.command))
			return false;
		if (module == null) {
			if (other.module != null)
				return false;
		} else if (!module.equals(other.module))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getName();
	}

}
