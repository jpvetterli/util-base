package ch.agent.util.ioc;

import ch.agent.util.args.Args;
import ch.agent.util.base.Misc;

/**
 * A minimal abstract implementation of the {@link Command} interface. It
 * provides a useful implementation of {@link #getModule}, {@link #getName}, and
 * {@link #execute}. It adds two methods to be overriden by actual
 * commands: {@link #defineParameters} and {@link #execute(Args)}.
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
	 * Define execution parameters or reset values.
	 * <p>
	 * Subclasses should usually call the super method first, before adding
	 * their definitions. This method does not add any definition of its own.
	 * Subclasses can test if the result is a new object or it has only been
	 * reset using the {@link Args#size} method.
	 * 
	 * @return the parameters object
	 */
	public Args defineParameters() {
		if (args == null)
			args = new Args();
		else
			args.reset();
		return args;
	}
	
	/**
	 * Execute the command.
	 * <p>
	 * Subclasses should usually call the super method first, before performing
	 * their own configuration.
	 * 
	 * @param parameters
	 *            the parameters object
	 * @throws IllegalArgumentException
	 *             if there is an error
	 */
	public int execute(Args parameters) {
		return 1;
	}

	@Override
	public int execute(String parameters) {
		Args args = defineParameters();
		args.parse(parameters);
		return execute(args);
	}

}
