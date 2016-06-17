package ch.agent.util.ioc;

import ch.agent.util.args.Args;

/**
 * A minimal abstract implementation of the {@link Module} interface. It provides
 * a useful implementation of {@link #getName}, leaves {@link #getObject} to
 * subclasses, and provides dummy implementations of all other methods.
 * 
 * @param <T>
 *            the type of the underlying object
 */
public abstract class AbstractModule<T> implements Module<T> {

	private String name;
	
	public AbstractModule(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void define(Args config) {
	}

	@Override
	public void configure(Args config) throws Exception {
	}

	@Override
	public boolean add(Module<?> module) {
		return false;
	}

	@Override
	public int start() {
		return 0;
	}

	@Override
	public boolean stop() {
		return false;
	}

	@Override
	public boolean remove(Module<?> module) {
		return false;
	}

	@Override
	public boolean update(Args config) throws Exception {
		return false;
	}

}
