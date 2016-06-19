package ch.agent.util.ioc;

import ch.agent.util.args.Args;

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
	
	public AbstractCommand(Module<T> module, String name) {
		if (module == null)
			throw new IllegalArgumentException("module null");
		if (name == null || name.length() == 0)
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
	 * Subclasses should usually call the super method first, before adding
	 * their definitions.
	 * 
	 * @return the parameters object
	 */
	public Args defineParameters() {
		return new Args();
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
