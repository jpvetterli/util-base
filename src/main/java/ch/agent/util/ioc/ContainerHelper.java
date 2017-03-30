package ch.agent.util.ioc;

import static ch.agent.util.STRINGS.lazymsg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import ch.agent.util.STRINGS.U;
import ch.agent.util.args.Args;
import ch.agent.util.base.Misc;
import ch.agent.util.ioc.ContainerToolBox.ManagedModule;
import ch.agent.util.ioc.ContainerToolBox.SimpleCommandRegistry;
import ch.agent.util.logging.LoggerBridge;

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
	
	private ContainerToolBox<C,B,D,M> tools;
	private C configuration;
	private Map<String, ManagedModule<D,M>> modules;
	private SimpleCommandRegistry commandRegistry;
	private Map<String, Set<String>> dependents;
	private boolean commandsGuard;
	
 	/**
	 * Constructor.
	 * 
	 * @param logger a logger or null
	 * @param cb the configuration builder to use
	 */
	public ContainerHelper(LoggerBridge logger, ConfigurationBuilder<C,B,D,M> cb) {
		tools = new ContainerToolBox<C,B,D,M>(logger, cb);
	}
	
	public void reset() {
		configuration = null;
		modules = null;
		commandRegistry = null;
		commandsGuard = false;
		dependents = null;
	}
	
	/**
	 * Shutdown all modules. Only those modules which have been initialized
	 * successfully are shutdown. Modules are shut down in the reverse
	 * initialization sequence.
	 * 
	 */
	public void shutdown() {
		if (modules != null)
			tools.shutdown(modules);
	}
	
	/**
	 * Shutdown one module. It is not allowed to shutdown a module still
	 * required by another module.
	 * <p>
	 * See also {@link #shutdown()}.
	 * 
	 */
	public void shutdown(M module) {
		if (modules != null) {
			tools.ensureModulesShutdown(module.getName(), getDependents().get(module.getName()), modules);
			module.shutdown();
			modules.get(module.getName()).setShutdown();
		}
	}
	
	/**
	 * Parse the textual system specification.
	 * 
	 * @param specification
	 *            a string
	 * @throws IllegalStateException
	 *             if the method is called twice without reset
	 * @throws ConfigurationException
	 *             if something is wrong with the configuration
	 */
	public void parse(String specification) {
		if (configuration != null)
			throw new IllegalStateException("cannot call #parse twice without reset");
		configuration = tools.parseConfiguration(specification);
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
	 * @throws IllegalStateException
	 *             if #parse not called successfully
	 * @throws IllegalStateException
	 *             if the method is called twice without reset
	 * @throws Exception
	 *             as soon as creating or configuring a module fails
	 */
	public void configure() throws Exception {
		if (configuration == null)
			throw new IllegalStateException("#parse not called successfully");
		if (modules != null)
			throw new IllegalStateException("cannot call #configure twice without reset");
		modules = tools.configureModules(configuration);
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
		if (modules == null)
			throw new IllegalStateException("#configure not called successfully");
		for (ManagedModule<D,M> memo : modules.values()) {
			M module = memo.getModule();
			initialize(module);
		}
	}
	
	/**
	 * Initialize one module in a sequence which guarantees that required
	 * modules are initialized before modules requiring them.
	 * 
	 * @throws IllegalStateException
	 *             if #configure not called successfully
	 * @throws IllegalStateException
	 *             if the method is called twice without reset
	 * @throws Exception
	 *             as soon as the initialization of a module fails
	 */
	public void initialize(M module) throws Exception {
		if (modules == null)
			throw new RuntimeException("modules null, module: " + module.getName());
		tools.logger.debug(lazymsg(U.C18, module.getName()));
		if (commandRegistry == null)
			commandRegistry = new SimpleCommandRegistry();
		// next ensures that (1) module not initialized and (2) required modules initialized
		tools.initializeModule(module, modules, commandRegistry);
		modules.get(module.getName()).setInitialized();
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
		if (modules == null)
			throw new IllegalStateException("#initialize not called successfully");
		if (commandsGuard)
			throw new IllegalStateException("cannot call #execute twice without reset");
		
		tools.ensureAllModulesInitialized(modules);
		if (commandRegistry != null)
			tools.executeCommands(configuration, commandRegistry);
		commandsGuard = true;
	}

	/**
	 * Get all modules.
	 * 
	 * @return a collection of a modules
	 * @throws IllegalStateException
	 *             if #configure not called successfully
	 */
	public Collection<M> getModules() {
		if (modules == null)
			throw new IllegalStateException("#configure not called successfully");
		return tools.asList(modules);
	}
	
	/**
	 * Get a module by name.
	 * 
	 * @param name
	 *            the name of the module, non-null
	 * @return a module, non-null
	 * @throws NoSuchElementException
	 *             if there is no module with that name
	 * @throws IllegalStateException
	 *             if #configure not called successfully
	 */
	public M getModule(String name) {
		if (modules == null)
			throw new IllegalStateException("#configure not called successfully");
		Misc.nullIllegal(name, "name null");
		ManagedModule<D,M> mm = modules.get(name);
		if (mm == null)
			throw new NoSuchElementException(name);
		return mm.getModule();
	}
	
	/**
	 * Get a command by name. The name to specify is the full name of 
	 * the command.
	 * 
	 * @param name
	 *            the name of the command, non-null
	 * @return a command, non-null
	 * @throws NoSuchElementException
	 *             if there is no command with that name
	 */
	public Command<?> getCommand(String name) {
		if (commandRegistry == null)
			throw new IllegalStateException("modules not initialized");
		Misc.nullIllegal(name, "name null");
		Command<?> command = commandRegistry.getCommands().get(name);
		if (command == null)
			throw new NoSuchElementException(name);
		return command;
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
	 * Return dependents. This is a map of module names to set of module names.
	 * Values identify modules which have the module named in the key as a
	 * requirement or a predecessor.
	 * 
	 * @return a map of module name to set of module name
	 */
	public Map<String, Set<String>> getDependents() {
		if (modules == null)
			throw new IllegalStateException("#configure not called successfully");
		if (dependents == null) {
			for (ManagedModule<D,M> mm : modules.values()) {
				addDependents(dependents, mm.getModule().getName(), mm.getModuleDefinition().getPrerequisites());
			}
		}
		return dependents;
	}
	
	protected void addDependents(Map<String, Set<String>> map, String key, String[] values) {
		for (String value : values) {
			Set<String> deps = map.get(value);
			if (deps == null) {
				deps = new HashSet<String>();
				map.put(value, deps);
			}
			deps.add(key);
		}
	}
	
}
