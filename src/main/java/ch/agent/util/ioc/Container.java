package ch.agent.util.ioc;

import static ch.agent.util.STRINGS.lazymsg;
import static ch.agent.util.STRINGS.msg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
 * <p>
 * This container uses {@link ContainerHelper} with {@link Configuration
 * <ModuleDefinition>} and {@link ModuleDefinition} as actual classes.
 * 
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

	private List<Module<?>> sortedModules;
	private long start; // start time of the #run method
	
	private ContainerHelper<
		Configuration<ModuleDefinition<Module<?>>, Module<?>>, 
		ModuleDefinitionBuilder<ModuleDefinition<Module<?>>, Module<?>>, 
		ModuleDefinition<Module<?>>,
		Module<?>> helper;
	
	/**
	 * Constructor.
	 */
	public Container() {
		sortedModules = new ArrayList<Module<?>>();
		helper = new ContainerHelper<
				Configuration<ModuleDefinition<Module<?>>, Module<?>>, 
				ModuleDefinitionBuilder<ModuleDefinition<Module<?>>, Module<?>>, 
				ModuleDefinition<Module<?>>,
				Module<?>>();
	}
	
	/**
	 * Clear the data structures before a new configuration.
	 */
	protected void reset() {
		sortedModules.clear();
	}
	
	/**
	 * Return the list of sorted modules. These sequence is valid with regard to
	 * dependency constraints.
	 * 
	 * @return a list of modules
	 */
	protected List<Module<?>> getModules() {
		return sortedModules;
	}
	
	/**
	 * Get a module by name.
	 * <p>
	 * This method has package visibility to support unit testing.
	 * 
	 * @param name
	 *            the name of the module, non-null
	 * @return a module, non-null
	 * @throws NoSuchElementException
	 *             if no module with that name was specified
	 */
	Module<?> getModule(String name) {
		Misc.nullIllegal(name, "name null");
		for (Module<?> m : sortedModules) {
			if (name.equals(m.getName()))
				return m; 
		}
		throw new NoSuchElementException(name);
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
		reset();
		start = System.currentTimeMillis();
		logger.info(lazymsg(U.C20, Misc.truncate(Arrays.toString((String[]) parameters), 60, " (etc.)")));
		try {
			ModuleDefinitionBuilder<ModuleDefinition<Module<?>>, Module<?>> builder = 
					new ModuleDefinitionBuilder<ModuleDefinition<Module<?>>, Module<?>>();
			Configuration<ModuleDefinition<Module<?>>, Module<?>> configuration = helper.parseConfiguration(Misc.join(" ", parameters), builder);
			sortedModules = helper.configureModules(configuration);
			Map<String, Command<?>> commands = helper.initializeModules(configuration, sortedModules);
			logInitializationMessage(sortedModules);
			helper.executeCommands(configuration, commands);
		} catch (EscapeException e) {
			logger.warn(msg(U.C19, e.getMessage()));
		} catch (Exception e) {
			logger.error(msg(U.C23, e.getClass().getSimpleName()));
			Throwable cause = e;
			while (cause != null) {
				logger.error(cause.getMessage());
				cause = cause == cause.getCause() ? null : cause.getCause();
			}
			throw e;
		}
	}

	private void logInitializationMessage(List<Module<?>> modules) {
		List<String> names = new ArrayList<String>();
		for (Module<?> m : modules) {
			names.add(m.getName());
		}
		if (names.size() > 0)
			logger.info(msg(U.C18, Misc.join("\", \"", names)));
	}
	
	/**
	 * Shutdown all modules. The sequence is the reverse of the initialization
	 * sequence.
	 */
	public void shutdown() {
		helper.shutdown(getModules());
		logger.info(lazymsg(U.C21, Misc.dhms(System.currentTimeMillis() - start)));
	}
		
}
