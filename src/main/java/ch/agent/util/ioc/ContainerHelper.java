package ch.agent.util.ioc;

import static ch.agent.util.STRINGS.lazymsg;
import static ch.agent.util.STRINGS.msg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.agent.util.STRINGS.U;
import ch.agent.util.args.Args;
import ch.agent.util.base.Misc;
import ch.agent.util.logging.LoggerBridge;

/**
 * The container helper provides a collection of stateless methods useful for
 * implementing containers.
 * 
 * @param <C>
 *            the configuration type
 * @param <B>
 *            the module definition builder type
 * @param <D>
 *            the module definition type
 * @param <M>
 *            the module type
 */
public class ContainerHelper<C extends Configuration<D,M>, B extends ModuleDefinitionBuilder<D,M>, D extends ModuleDefinition<M>, M extends Module<?>> {

	/**
	 * Command registry used during module initialization.
	 *
	 */
	public static class SimpleCommandRegistry implements CommandRegistry {

		private Map<String, Command<?>> commands; // key is the actual command name 
		
		public SimpleCommandRegistry() {
			commands = new HashMap<String, Command<?>>();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * This implementation changes the command name by using the module
		 * name and a period as prefix.
		 */
		@Override
		public String register(Command<?> command) {
			String name = command.getModule().getName() + "." + command.getName();
			Command<?> existing = commands.get(name);
			if (existing != null)
				throw new IllegalStateException(msg(U.C12, command.getModule().getName(), command.getName()));
			commands.put(name, command);
			return name;
		}
	
		/**
		 * Get the command map. The keys are command names as defined by
		 * {@link Command#getName()} if possible, or the same prefixed with
		 * {@link Module#getName()} and a period to achieve name uniqueness
		 * within a system.
		 * 
		 * @return the command map
		 */
		public Map<String, Command<?>> getCommands() {
			return commands;
		}
		
	}
	
	private LoggerBridge logger;
	
	/**
	 * Constructor.
	 * 
	 * @param logger a logger or null
	 */
	public ContainerHelper(LoggerBridge logger) {
		this.logger = logger;
	}
	
	/**
	 * Shutdown all modules.
	 * 
	 * @param modules
	 *            list of modules in original (initialization) sequence
	 */
	public void shutdown(List<M> modules) {
		int i = modules.size();
		while (--i >= 0) {
			try {
				modules.get(i).shutdown();
			} catch (Exception e) {
				// ignore
			}
		}
	}
	
	/**
	 * Turn a textual specification into a configuration.
	 * 
	 * @param specification
	 *            a string
	 * @param moduleDefinitionBuilder the module definition builder to use
	 * @return a configuration object
	 * @throws ConfigurationException
	 *             if something is wrong with the configuration
	 */
	public C parseConfiguration(String specification, B moduleDefinitionBuilder) {
		ConfigurationBuilder<C,B,D,M> builder = new ConfigurationBuilder<C,B,D,M>(moduleDefinitionBuilder);
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
	 * @return the list of modules in valid sequence
	 * @throws Exception
	 *             as soon as creating or configuring a module fails
	 */
	public List<M> configureModules(C configuration) throws Exception {
		List<M> modules = new ArrayList<M>();
		Args moduleConfig = new Args();
		for (D module : configuration.getModuleDefinitions()) {
			moduleConfig.def(module.getName()).init(""); // it is okay to omit the statement
		}
		moduleConfig.parse(configuration.getConfiguration());
		
		for (D spec : configuration) {
			try {
				M m = spec.create();
				m.configure(moduleConfig.get(spec.getName()));
				modules.add(m);
			} catch (Exception e) {
				throw new ConfigurationException(msg(U.C14, spec.getName()), e);
			}
		}
		return modules;
	}

	/**
	 * Initialize all modules in a sequence which guarantees that required
	 * modules are initialized before modules requiring them.
	 * 
	 * @param configuration
	 *            the configuration object
	 * @param modules
	 *            the module list, in valid sequence
	 * @return the map of commands keyed by command name
	 * @throws Exception
	 *             as soon as the initialization of a module fails
	 */
	public Map<String, Command<?>> initializeModules(C configuration, List<M> modules) throws Exception {
		SimpleCommandRegistry registry = new SimpleCommandRegistry();
		Map<String, M> modulesByName = asMap(modules);
		for (M module : modules) {
			initializeModule(module, configuration.get(module.getName()).getRequirements(), modulesByName, registry);
		}
		return registry.getCommands();
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
	 * @param modulesByName
	 *            a map of modules keyed by module name
	 * @param registry
	 *            the command registry to use for registering module commands
	 * @throws Exception
	 *             in case of failure
	 */
	public void initializeModule(M module, String[] requirements, Map<String, M> modulesByName, CommandRegistry registry) throws Exception {
		try {
			addRequiredModules(module, requirements, modulesByName);
			module.registerCommands(registry);
			if (!module.initialize()) {
				if (logger != null)
					logger.warn(lazymsg(U.C25, module.getName()));
			}
		} catch (EscapeException e) {
			throw e;
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
	 * @param commands
	 *            a map of commands keyed by unique command name
	 * @throws Exception
	 *             execution of commands can throw exceptions
	 */
	public void executeCommands(C configuration, Map<String, Command<?>> commands) throws Exception {
		Args execSyntax = new Args();
		for (String commandName : commands.keySet()) {
			execSyntax.defList(commandName); // a command can be executed 0 or more times
		}
		execSyntax.setSequenceTrackingMode(true);
		
		try {
			execSyntax.parse(configuration.getExecution());
		} catch (Exception e) {
			throw new Exception(msg(U.C24), e);
		}
		
		List<String[]> statements = execSyntax.getSequence();
		for (String[] statement : statements) {
			Command<?> command = commands.get(statement[0]);
			try {
				if (!command.execute(statement[1])) {
					if (logger != null)
						logger.warn(lazymsg(U.C26, command.getName(), command.getModule().getName(), statement[1]));
				}
			} catch (EscapeException e) {
				throw e;
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
	 * @param modulesByName
	 *            a map of modules keyed by module name
	 * @throws Exception
	 *             in case of one or more failures
	 */
	public void addRequiredModules(M requiring, String[] required, Map<String, M> modulesByName) throws Exception {
		List<String> rejected = new ArrayList<String>();
		for (String name : required) {
			Module<?> m = modulesByName.get(name);
			if (m == null)
				throw new RuntimeException("bug found, a module required by \"" + requiring.getName() + "\" is missing");
			if (!requiring.add(m))
				rejected.add(name);
		}
		if (rejected.size() > 0)
			throw new Exception(msg(U.C17, requiring.getName(), Misc.join("\", \"", rejected)));
	}

	/**
	 * Produce a module map from a module list.
	 * 
	 * @param modules
	 *            a list of modules
	 * @return a map of modules keyed by module name
	 */
	public Map<String, M> asMap(List<M> modules) {
		Map<String, M> map = new HashMap<String, M>(modules.size());
		for (M m : modules) {
			map.put(m.getName(), m);
		}
		return map;
	}
	
}
