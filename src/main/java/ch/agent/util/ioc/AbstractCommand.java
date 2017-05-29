package ch.agent.util.ioc;

import static ch.agent.util.STRINGS.msg;
import ch.agent.util.STRINGS.U;
import ch.agent.util.args.Args;
import ch.agent.util.base.Misc;

/**
 * A minimal abstract implementation of the {@link Command} interface. It
 * provides a useful implementation of {@link #getName()},
 * {@link #rename(String)}, and {@link #execute(String)}. It adds two methods to
 * be overridden by actual commands: {@link #defineParameters(Args)}, which is
 * called only once and {@link #execute(Args)}.
 * 
 * @param <T>
 *            the type of the underlying object
 */
public abstract class AbstractCommand<T> implements Command<T> {

	private String name;
	private Args args;

	/**
	 * Constructor. The command name cannot be empty and may not contain a
	 * period. However it is possible to rename the command with a name
	 * containing one or more periods. This feature is used by the
	 * implementation of {@link AbstractModule#add(Module)}.
	 * 
	 * @param name
	 *            command name
	 * @throws IllegalArgumentException
	 *             if the name is empty or contains a period
	 */
	public AbstractCommand(String name) {
		if (Misc.isEmpty(name) || name.indexOf(CommandSpecification.NAME_SEPARATOR) >= 0)
			throw new ConfigurationException(msg(U.C50));
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isParameterless() {
		return false;
	}

	@Override
	public void rename(String name) {
		if (Misc.isEmpty(name))
			throw new ConfigurationException(msg(U.C51));
		this.name = name;
	}

	/**
	 * Define execution parameters.
	 * <p>
	 * This default implementation does not define any parameter. The method is
	 * called only once in the life time of the command. Subclasses should throw
	 * an {@link IllegalArgumentException} when something is wrong; this is
	 * usually achieved by not catching exceptions when using {@link Args} or
	 * {@link Args.Definition} methods.
	 * 
	 * @param parameters
	 *            the parameters object
	 * @throws IllegalArgumentException
	 *             as described in the comment
	 */
	public void defineParameters(Args parameters) {
	}

	/**
	 * Execute the command using an Args object.
	 * <p>
	 * 
	 * @param parameters
	 *            the parameters object
	 * @throws IllegalArgumentException
	 *             if there is a problem with parameters
	 * @throws Exception
	 *             to signal a critical problem during actual execution
	 */
	public void execute(Args parameters) throws Exception {
	}

	@Override
	public void execute(String parameters) throws Exception {
		if (args == null) {
			args = new Args();
			defineParameters(args);
		} else
			args.reset();
		args.parse(parameters);
		execute(args);
	}

	@Override
	public String toString() {
		return "command " + getName();
	}

}
