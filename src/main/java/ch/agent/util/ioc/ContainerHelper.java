package ch.agent.util.ioc;

import static ch.agent.util.STRINGS.lazymsg;
import static ch.agent.util.STRINGS.msg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.agent.util.STRINGS.U;
import ch.agent.util.args.Args;
import ch.agent.util.base.Misc;
import ch.agent.util.logging.LoggerBridge;
import ch.agent.util.logging.LoggerManager;

/**
 * The simple container helper provides a collection of methods useful for
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
	
	final static LoggerBridge logger = LoggerManager.getLogger(ContainerHelper.class);
	
	/**
	 * Command registry used during module initialization.
	 *
	 */
	public static class SimpleCommandRegistry implements CommandRegistry {

		private Map<String, Command<?>> commands; // key is the actual command name 
		
		public SimpleCommandRegistry() {
			commands = new HashMap<String, Command<?>>();
		}

		@Override
		public void register(Command<?> command) {
			String name = command.getFullName();
			Command<?> existing = commands.get(name);
			if (existing != null)
				throw new ConfigurationException(msg(U.C12, command.getModule().getName(), command.getFullName()));
			commands.put(name, command);
		}
	
		@Override
		public Map<String, Command<?>> getCommands() {
			return commands;
		}
		
	}
	
	private C configuration;
	private Map<String, M> modules;
	private SimpleCommandRegistry commandRegistry;
	private boolean commandsGuard;
	
 	/**
	 * Constructor.
	 * 
	 * @param configuration the configuration
	 */
	public ContainerHelper(C configuration) {
		Misc.nullIllegal(configuration, "configuration null");
		this.configuration = configuration;
		this.modules = new HashMap<String, M>(configuration.getModuleCount());
		commandRegistry = new SimpleCommandRegistry();
	}
	
	/**
	 * Shutdown all modules. Only those modules which have been initialized
	 * successfully are shutdown. Modules are shut down in the reverse
	 * initialization sequence.
	 * 
	 */
	public void shutdown() {
		List<M> list = new ArrayList<M>(modules.values());
		int i = list.size();
		while (--i >= 0) {
			try {
				M module = list.get(i);
				module.shutdown();
			} catch (Exception e) {
				// ignore
			}
		}
	}
	
	/**
	 * Create and configure all modules.
	 * 
	 * @throws IllegalStateException
	 *             if #parse not called successfully
	 * @throws IllegalStateException
	 *             if the method is called twice without reset
	 * @throws Exception
	 *             as soon as creating or configuring a module fails
	 */
	public void configure() throws Exception {
		for (String name : configuration.getModuleNames()) {
			configure(name);
		}
	}
	
	/**
	 * Create and configure one module.
	 */
	public M configure(String name) throws Exception {
		M module = configuration.getModuleDefinition(name).configure(modules, commandRegistry);
		if (modules.put(name, module) != null)
			throw new IllegalStateException(msg(U.C55, name));
		return module;
	}

	/**
	 * Initialize all modules in a sequence which guarantees that required
	 * modules are initialized before modules requiring them.
	 * 
	 * @throws IllegalStateException
	 *             if #configure not called successfully
	 * @throws IllegalStateException
	 *             if the method is called twice without reset
	 * @throws Exception
	 *             as soon as the initialization of a module fails
	 */
	public void initialize() throws Exception {
		for (M module : modules.values()) {
			if (!module.initialize())
				logger.warn(lazymsg(U.C25, module.getName()));
			else
				logger.debug(lazymsg(U.C18, module.getName()));
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
	 * following configuration will be parsed successfully:
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
	 * @throws IllegalStateException
	 *             if #initialize not called successfully
	 * @throws IllegalStateException
	 *             if the method is called twice without reset
	 * @throws Exception
	 *             execution of commands can throw exceptions
	 */
	public void execute() throws Exception {
		if (commandsGuard)
			throw new IllegalStateException("cannot call #execute twice without reset");
		if (commandRegistry != null)
			executeCommands(configuration, commandRegistry);
		commandsGuard = true;
	}

	/**
	 * Get all the modules which have been already configured.
	 * 
	 * @return a collection of modules
	 */
	public Collection<M> getModules() {
		return modules.values();
	}
	
	/**
	 * Get a module by name.
	 * 
	 * @param name
	 *            the name of the module
	 * @return a module or null if the module has not yet been configured (or does not exist)
	 */
	public M getModule(String name) {
		return modules.get(name);
	}
	
	/**
	 * Get a command by name. The name to specify is the full name of the
	 * command.
	 * 
	 * @param name
	 *            the name of the command
	 * @return a command or null if there is no such command in the registry
	 */
	public Command<?> getCommand(String name) {
		return commandRegistry.getCommands().get(name);
	}
	
	/**
	 * Get all commands belonging to a module. If the module name is null, all
	 * commands are returned.
	 * 
	 * @param moduleName
	 *            the name of the module or null for all commands
	 * @return a collection of commands, possibly empty
	 */
	public Collection<Command<?>> getCommands(String moduleName) {
		if (commandRegistry == null)
			throw new IllegalStateException("no #initialize method called");
		Collection<Command<?>> commands = new ArrayList<Command<?>>();
		for (Command<?> command : commandRegistry.getCommands().values()) {
			if (moduleName == null || moduleName.equals(command.getModule().getName()))
				commands.add(command);
		}
		return commands;
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
	 *            the non-null configuration object
	 * @param registry
	 *            a non-null command registry
	 * @throws Exception
	 *             execution of commands can throw exceptions
	 */
	public void executeCommands(C configuration, CommandRegistry registry) throws Exception {
		Args execSyntax = new Args();
		Map<String, Command<?>> commands = registry.getCommands();
		for (String commandName : commands.keySet()) {
			execSyntax.defList(commandName); // a command can be executed 0 or more times
		}
		execSyntax.setSequenceTrackingMode(true);
		
		try {
			execSyntax.parse(configuration.getExecution());
		} catch (Exception e) {
			throw new ConfigurationException(msg(U.C24), e);
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
	 * Return dependents. This is a map of module names to set of module names.
	 * Values identify modules which have the module named in the key as a
	 * requirement or a predecessor.
	 * 
	 * @return a map of module name to set of module name
	 */
	public Map<String, Set<String>> getDependents() {
		Map<String, Set<String>> dependents = new HashMap<String, Set<String>>();
		for (D def : configuration.getModuleDefinitions()) {
			addDependents(dependents, def.getName(), def.getPrerequisites());
		}
		return dependents;
	}
	
	protected void addDependents(Map<String, Set<String>> map, String module, String[] prereqs) {
		for (String prereq : prereqs) {
			Set<String> deps = map.get(prereq);
			if (deps == null) {
				deps = new HashSet<String>();
				map.put(prereq, deps);
			}
			deps.add(module);
		}
	}
	
}
