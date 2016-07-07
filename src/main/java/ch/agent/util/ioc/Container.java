package ch.agent.util.ioc;

import static ch.agent.util.STRINGS.lazymsg;
import static ch.agent.util.STRINGS.msg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import ch.agent.util.STRINGS.U;
import ch.agent.util.args.Args;
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
 * 
 */
public class Container implements CommandRegistry {

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
	private Map<String, Module<?>> modulesByName;
	private Map<String, Command<?>> commands; // key is the actual command name 
	private long start; // start time of the #run method
	
	/**
	 * Constructor.
	 */
	public Container() {
		sortedModules = new ArrayList<Module<?>>();
		modulesByName = new HashMap<String, Module<?>>();
		commands = new HashMap<String, Command<?>>();
	}
	
	/**
	 * Clear the data structures before a new configuration.
	 */
	protected void reset() {
		sortedModules.clear();
		modulesByName.clear();
		commands.clear();
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
		Module<?> found = modulesByName.get(name);
		if (found == null)
			throw new NoSuchElementException(name);
		return found;
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
		logger.info(lazymsg(U.C20, Arrays.toString((String[]) parameters)));
		try {
			Configuration<ModuleDefinition> configuration = parseConfiguration(Misc.join(" ", parameters));
			configureModules(configuration);
			initializeModules(configuration);
			executeCommands(configuration);
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

	/**
	 * Shutdown all modules. The sequence is the reverse of the initialization
	 * sequence.
	 */
	public void shutdown() {
		shutdown(getModules());
		logger.info(lazymsg(U.C21, dhms(System.currentTimeMillis() - start)));
	}
	
	/**
	 * Shutdown all modules.
	 * 
	 * @param modules
	 *            list of modules in original (initialization) sequence
	 */
	public void shutdown(List<Module<?>> modules) {
		int i = modules.size();
		while (--i >= 0) {
			try {
				modules.get(i).shutdown();
			} catch (Exception e) {
				// ignore
			}
		}
	}
	
	@Override
	public String register(Command<?> command) {
		if (commands == null)
			throw new IllegalStateException("#run not called");
		String name = command.getName();
		Command<?> existing = commands.get(name);
		if (existing == null)
			commands.put(name, command);
		else {
			String moduleName = command.getModule().getName();
			if (existing.getModule().getName().equals(moduleName))
				throw new IllegalStateException(msg(U.C12, moduleName, name));
			else {
				name = moduleName + "." + name;
				if (commands.get(name) != null)
					throw new RuntimeException("bug found " + name);
				else
					commands.put(name, command);
			}
		}
		return name;
	}

	/**
	 * Turn a textual specification into a configuration.
	 * The actual builder classes used are {@link ConfigurationBuilder} and
	 * {@link ModuleDefinitionBuilder}.
	 * 
	 * @param specification a string
	 * @return a configuration object
	 * @throws Exception
	 */
	protected Configuration<ModuleDefinition> parseConfiguration(String specification) throws Exception {
		ConfigurationBuilder<Configuration<ModuleDefinition>, ModuleDefinition> builder = 
				new ConfigurationBuilder<Configuration<ModuleDefinition>, ModuleDefinition>(
						new ModuleDefinitionBuilder<ModuleDefinition>());
		return builder.build(specification);
	}

	/**
	 * Create and configure all modules.
	 * <p>
	 * The method creates a little {@link Args} language to parse the
	 * <em>configuration</em> specification. In this language, parameter names
	 * are the module names, their expected value is a string, and they can be
	 * omitted. As an example, if there are three modules A, B, and C, the
	 * following configuration will be parsed successfully:
	 * 
	 * <pre>
	 * <code>
	 * configuration=[
	 *   B = [<em>configuration details for module B</em>]
	 *   A = [<em>configuration details for module A</em>]
	 *   # C is omitted
	 * ]
	 * </code>
	 * </pre>
	 * 
	 * @param configuration
	 *            the configuration object
	 * @throws Exception
	 *             as soon as creating or configuring a module fails
	 */
	protected void configureModules(Configuration<ModuleDefinition> configuration) throws Exception {
		reset();
		Args moduleConfig = new Args();
		for (ModuleDefinition module : configuration.getModuleDefinitions()) {
			moduleConfig.def(module.getName()).init(""); // it is okay to omit the statement
		}
		moduleConfig.parse(configuration.getConfiguration());
		
		for (ModuleDefinition spec : configuration) {
			try {
				Module<?> m = spec.create();
				m.configure(moduleConfig.get(spec.getName()));
				sortedModules.add(m);
				modulesByName.put(m.getName(),  m);
			} catch (Exception e) {
				throw new ConfigurationException(msg(U.C14, spec.getName()), e);
			}
		}
	}

	/**
	 * Initialize all modules in a sequence which guarantees that required
	 * modules are initialized before modules requiring them.
	 * 
	 * @param configuration
	 *            the configuration object
	 * @throws Exception
	 *             as soon as the initialization of a module fails
	 */
	/**
	 * @throws Exception
	 */
	protected void initializeModules(Configuration<ModuleDefinition> configuration) throws Exception {
		for (Module<?> module : getModules()) {
			initializeModule(module, configuration.get(module.getName()).getRequirements());
			logger.info(msg(U.C08, module.getName()));
		}
	}

	/**
	 * Initialize a module. Initialization consists of the following steps:
	 * <ol>
	 * <li>Required modules are added with {@link Module#add}.
	 * <li>Module commands are registered with{@link Module#registerCommands}.
	 * <li>The module is initialized with {@link Module#initialize}.
	 * </ol>
	 * 
	 * IMPORTANT: required modules must already have been configured.
	 * 
	 * @param module
	 *            the module
	 * @param requirements
	 *            zero or more names of required modules
	 * @throws Exception
	 *             in case of failure
	 */
	protected void initializeModule(Module<?> module, String... requirements) throws Exception {
		try {
			Module<?>[] prerequisites = getRequiredModules(requirements);
			addRequiredModules(module, prerequisites);
			module.registerCommands(this);
			module.initialize();
		} catch (Exception e) {
			throw new Exception(msg(U.C07, module.getName()), e);
		}
	}
	
	/**
	 * Execute all commands in the <em>execution</em> specification.
	 * <p>
	 * The method creates a little {@link Args} language to parse the
	 * <em>execution</em> specification. In this language, parameter names are
	 * the command names (possibly prefixed for uniqueness by the module name),
	 * their expected value is a string, and they are repeatable (and can be
	 * omitted). As an example, if there are three commands A.X, B.X, and Y, the
	 * following configuration will be parsed successfully:module
	 * 
	 * <pre>
	 * <code>
	 * execution=[
	 *   A.X = [<em>execution details for command A.X</em>]
	 *   A.X = [<em>possibly different execution details for command A.X</em>]
	 *   Y = [<em>execution details for command Y</em>]
	 *   # B.X is omitted
	 * ]
	 * </code>
	 * </pre>
	 * 
	 * @param configuration
	 *            the configuration object
	 * @throws Exception
	 *             execution of commands can throw exceptions
	 */
	protected void executeCommands(Configuration<ModuleDefinition> configuration) throws Exception {
		Args execSyntax = new Args();
		for (String commandName : commands.keySet()) {
			execSyntax.defList(commandName); // a command can be executed 0 or more times
		}
		execSyntax.setSequenceTrackingMode(true);
		execSyntax.parse(configuration.getExecution());
		List<String[]> statements = execSyntax.getSequence();
		for (String[] statement : statements) {
			Command<?> command = commands.get(statement[0]);
			try {
				command.execute(statement[1]);
			} catch (Exception e) {
				throw new Exception(msg(U.C22, command.getName(), command.getModule().getName(), statement[1]), e);
			}
		}
	}
	
	/**
	 * Add all modules required by a module. The container must guarantee that
	 * any required module has already been created and configured. On the other
	 * hand a required module may or may not have been initialized.
	 * 
	 * @param requiring
	 *            the requiring module
	 * @param required
	 *            array of required modules
	 * @throws Exception
	 *             in case of one or more failures
	 */
	protected void addRequiredModules(Module<?> requiring, Module<?>[] required) throws Exception {
		int errors = 0;
		for (Module<?> m : required) {
			if (m == null)
				throw new RuntimeException("bug found, a module required by \"" + requiring.getName() + "\" is null");
			if (!requiring.add(m)) {
				logger.error(msg(U.C05, requiring.getName(), m.getName()));
				errors++;
			}
		}
		if (errors > 0)
			throw new Exception(msg(U.C10, requiring.getName()));
	}

	/**
	 * Turn array of module names into array of modules. This method may not be
	 * called if any named module has not yet been created and configured.
	 * 
	 * @param names
	 *            array of module names
	 * @return array of modules
	 */
	protected Module<?>[] getRequiredModules(String[] names) throws Exception {
		Module<?>[] required = new Module<?>[names.length];
		int i = 0;
		for (String name : names) {
			Module<?> m = getModule(name);
			if (m == null)
				throw new RuntimeException("bug found, module \"" + name + "\" is null");
			required[i++] = m;
		}
		return required;
	}

	/**
	 * Convert milliseconds into string with days, hours, minutes, and seconds.
	 * Leading days and hours are omitted if zero.
	 * 
	 * @param millis
	 *            milliseconds
	 * @return a string representing days, hours, minutes, and seconds
	 */
	private static String dhms(long t) {
		final int MPD = 24*60*60*1000;
		long days = (t / MPD);
		int s = (int) (t - days * MPD) / 1000;
		int m = s / 60;
		int h = m / 60;
		s = s - m * 60;
		m = m - h * 60;
		String result = "";
		if (days > 0)
			result = String.format("%dd%dh%dm%ds", days, h, m, s);
		else {
			if (h > 0)
				result = String.format("%dh%dm%ds", h, m, s);
			else
				result = String.format("%dm%ds", m, s);
		}
		return result;
	}
	
}
