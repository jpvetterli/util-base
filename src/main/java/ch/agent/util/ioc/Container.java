package ch.agent.util.ioc;

import static ch.agent.util.STRINGS.lazymsg;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.NoSuchElementException;

import ch.agent.util.STRINGS.U;
import ch.agent.util.base.Misc;
import ch.agent.util.logging.LoggerBridge;
import ch.agent.util.logging.LoggerManager;

/**
 * A container sets up and launches a system of modules.
 * <p>
 * The system of modules is configured from a textual specification, which is
 * turned into a {@link Configuration} with the help of a
 * {@link ConfigurationBuilder}. The specifications of individual modules is
 * turned into {@link ModuleDefinition}s by a {@link ModuleDefinitionBuilder}.
 * <p>
 * The container initializes modules in the sequence determined by the
 * configuration. This sequence takes dependencies into account. When shutting
 * down the container, modules are shut down in the reverse order of the
 * initialization sequence. Exceptions occurring during shutdown are discarded.
 * <p>
 * Details on the configuration of individual modules are extracted from the
 * <em>configuration</em> specification. This is a sequence of statements named
 * after the modules. There is at most one statement for each module. The syntax
 * of the module configuration is defined by the module itself.
 * <p>
 * Commands to be executed and their parameters are extracted from the
 * <em>execution</em> specification. This is a sequence of statements named
 * after the commands registered by modules during module initialization. The
 * names actually used are either given by {@link Command#getName} or, if that
 * name is already registered by another module, the concatenation of the module
 * name and the command name, with a period between them. The syntax of command
 * parameters is defined by the command themselves. The container passes the
 * value verbatim to the {@link Command#execute} methods.
 */
public class Container {

	final static LoggerBridge logger = LoggerManager.getLogger(Container.class);

	/**
	 * A main method which can be used out of the box.
	 * 
	 * @param args
	 *            an array of strings passed by the run time environment
	 */
	public static void main(String[] args) {
		Container c =  new Container();
		int exit = 0;
		try {
			c.run(args);
		} catch (Exception e) {
			e.printStackTrace();
			exit = 1;
		} finally {
			c.shutdown();
		}
		System.exit(exit);
	}

	private long start; // start time of the #run method
	private ConfigurationRegistry<Module<?>> registry;
	private Configuration<ModuleDefinition<Module<?>>, Module<?>> configuration;
	
	/**
	 * Constructor.
	 */
	public Container() {
	}

	/**
	 * Get a new configuration builder.
	 * 
	 * @return a configuration builder
	 */
	public ConfigurationBuilder<
		Configuration<ModuleDefinition<Module<?>>, Module<?>>,
		ModuleDefinitionBuilder<ModuleDefinition<Module<?>>, Module<?>>,
		ModuleDefinition<Module<?>>,
		Module<?>
	> getBuilder() {
		return new ConfigurationBuilder<
				Configuration<ModuleDefinition<Module<?>>, Module<?>>,
				ModuleDefinitionBuilder<ModuleDefinition<Module<?>>, Module<?>>,
				ModuleDefinition<Module<?>>, 
				Module<?>
			>(
				new ModuleDefinitionBuilder<ModuleDefinition<Module<?>>, Module<?>>()
			);
	}

	/**
	 * Get a module by name.
	 * 
	 * @param name
	 *            the name of the module, non-null
	 * @return a module, non-null
	 * @throws NoSuchElementException
	 *             if there is no module with that name
	 * @throws IllegalArgumentException
	 *             if method used after configuration error
	 */
	public Module<?> getModule(String name) {
		if (registry == null)
			throw new IllegalArgumentException("Bug: #getModule used after configuration error.");
		return registry.getModules().get(name);
	}
	
	/**
	 * Configure and initialize modules, and execute commands. Any
	 * {@link Exception} during processing is caught and thrown again, after
	 * logging a termination message followed by all exception messages in the
	 * cause chain. If a stack trace is wanted, it can be produced by catching
	 * the exception thrown by {@link #run}.
	 * <p>
	 * The method does not perform the {@link #shutdown} because some
	 * applications or unit tests need to access module data after {@link #run}
	 * has returned or has been interrupted by an exception. Performing the
	 * {@link #shutdown} is therefore the responsibility of the client,
	 * typically in a <em>finally</em> clause.
	 * 
	 * @param parameters
	 *            an array of command line parameters
	 * @throws Exception
	 *             anything can happen during execution
	 */
	public void run(String[] parameters) throws Exception {
		start = System.currentTimeMillis();
		logger.info(lazymsg(U.C20, Misc.truncate(Arrays.toString((String[]) parameters), 60, " (etc.)")));
		try {
			configuration = getBuilder().build(Misc.join(" ", parameters));
			registry = configuration.configure();
			configuration.executeCommands(registry, configuration.parseCommands(registry.getCommands().values()));
		} catch (EscapeException e) {
			logger.warn(lazymsg(U.C19, e.getMessage()));
		} catch (Exception e) {
			logger.error(lazymsg(U.C23, e.getClass().getSimpleName()));
			Throwable cause = e;
			while (cause != null) {
				if (!(cause instanceof InvocationTargetException))
					logger.error(cause.getMessage());
				cause = cause == cause.getCause() ? null : cause.getCause();
			}
			throw e;
		}
	}
	/**
	 * Shutdown all modules in the reverse initialization sequence. The method does
	 * nothing in case of configuration errors, except logging the termination.
	 */
	public void shutdown() {
		if (configuration != null && registry != null)
			configuration.shutdown(registry);
		logger.info(lazymsg(U.C21, Misc.dhms(System.currentTimeMillis() - start)));
	}
		
}
