package ch.agent.util.ioc;

import static ch.agent.util.STRINGS.msg;
import ch.agent.util.STRINGS.U;
import ch.agent.util.args.Args;
import ch.agent.util.base.Misc;

/**
 * A minimal abstract implementation of the {@link Command} interface. It
 * provides a useful implementation of {@link #getName} and {@link #execute}. It
 * adds two methods to be overridden by actual commands:
 * {@link #defineParameters}, which is called only once and
 * {@link #execute(Args)}.
 * 
 * @param <T>
 *            the type of the underlying object
 */
public abstract class AbstractCommand<T> implements Command<T> {

	private String name;
	private Args args;
	
	public AbstractCommand(String name) {
		if (Misc.isEmpty(name))
			throw new ConfigurationException(msg(U.C51));
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * Define execution parameters.
	 * <p>
	 * This default implementation does not define any parameter. The method is
	 * called only once in the life time of the command.
	 * 
	 * @param parameters
	 *            the parameters object
	 */
	public abstract void defineParameters(Args parameters);
	
	/**
	 * Execute the command.
	 * <p>
	 * 
	 * @param parameters
	 *            the parameters object
	 * @return true unless there was some error
	 * @throws Exception
	 *             to signal critical problems
	 */
	public abstract boolean execute(Args parameters) throws Exception;

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
