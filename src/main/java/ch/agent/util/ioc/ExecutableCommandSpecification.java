package ch.agent.util.ioc;

import ch.agent.util.base.Misc;

/**
 * An executable command specification is a command specification with
 * parameters.
 */
public class ExecutableCommandSpecification extends CommandSpecification {

	private static final long serialVersionUID = -5325049542871436808L;

	private final String parameters;

	/**
	 * Constructor for an executable command definition. The command is only
	 * executable if the parameters argument is not null.
	 * 
	 * @param spec
	 *            a command specification, not null
	 * @param parameters
	 *            command parameters, not null
	 */
	public ExecutableCommandSpecification(CommandSpecification spec, String parameters) {
		super(spec.getModule(), spec.getCommand(), spec.isParameterless());
		Misc.nullIllegal(parameters, "parameters null");
		this.parameters = parameters;
	}

	/**
	 * Get the parameter string.
	 * 
	 * @return an opaque string with command parameters
	 */
	public String getParameters() {
		return parameters;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExecutableCommandSpecification other = (ExecutableCommandSpecification) obj;
		if (parameters == null) {
			if (other.parameters != null)
				return false;
		} else if (!parameters.equals(other.parameters))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return parameters.length() < 50 ? String.format("%s %s", getName(), parameters) : String.format("%s %s...", getName(), parameters.substring(0, 50));
	}

}
