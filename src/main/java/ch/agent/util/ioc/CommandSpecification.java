package ch.agent.util.ioc;

import java.io.Serializable;

import ch.agent.util.base.Misc;

/**
 * A command specification consists of a module name and a command name.
 */
public class CommandSpecification implements Serializable {

	private static final long serialVersionUID = -1426998440999221081L;

	/**
	 * The separator between module and command names is a period (.).
	 */
	public static final String NAME_SEPARATOR = ".";

	private final String command;
	private final String module;
	private final boolean parmeterless; // irrelevant for equals and hashCode

	/**
	 * Constructor.
	 * 
	 * @param module
	 *            the module name or null for a built-in command
	 * @param command
	 *            the command name, not null
	 * @param parameterless
	 *            if true, it is a parameterless command
	 */
	public CommandSpecification(String module, String command, boolean parameterless) {
		super();
		Misc.nullIllegal(command, "command null");
		this.module = module;
		this.command = command;
		this.parmeterless = parameterless;
	}

	/**
	 * Get the complete name of the command. Module and command names are joined
	 * with a period to achieve uniqueness in a system where module names are
	 * unique. Command <code>foo.bar</code> is the full name of command
	 * <code>bar</code> in module <code>foo</code>. For built-in commands the
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

	/**
	 * Test if it is a parameterless command.
	 * 
	 * @return true if it is parameterless command
	 */
	public boolean isParameterless() {
		return parmeterless;
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
