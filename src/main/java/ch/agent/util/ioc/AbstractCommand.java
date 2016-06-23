package ch.agent.util.ioc;

import ch.agent.util.args.Args;
import ch.agent.util.base.Misc;

/**
 * A minimal abstract implementation of the {@link Command} interface. It
 * provides a useful implementation of {@link #getModule}, {@link #getName}, and
 * {@link #execute}. It adds two methods to be overridden by actual commands:
 * {@link #defineParameters}, which is called only once and
 * {@link #execute(Args)}.
 * 
 * @param <T>
 *            the type of the underlying object
 */
public abstract class AbstractCommand<T> implements Command<T> {

	private Module<T> module;
	private String name;
	private Args args;
	
	public AbstractCommand(Module<T> module, String name) {
		Misc.nullIllegal(module, "module null");
		if (Misc.isEmpty(name))
			throw new IllegalArgumentException("name null or emtpy");
		this.module = module;
		this.name = name;
	}

	@Override
	public Module<T> getModule() {
		return module;
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
	public void defineParameters(Args parameters) {
	}
	
	/**
	 * Execute the command.
	 * <p>
	 * 
	 * @param parameters
	 *            the parameters object
	 * @throws IllegalArgumentException
	 *             if there is an error
	 */
	public void execute(Args parameters) {
	}

	@Override
	public void execute(String parameters) {
		if (args == null) {
			args = new Args();
			defineParameters(args);
		} else
			args.reset();
		args.parse(parameters);
		execute(args);
	}

}
