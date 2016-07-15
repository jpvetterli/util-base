package ch.agent.util.ioc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import ch.agent.util.args.Args;
import ch.agent.util.base.Misc;
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
public class ContainerHelper<C extends Configuration<D,M>, B extends ModuleDefinitionBuilder<D,M>, D extends ModuleDefinition<M>, M extends Module<?>> implements Iterable<M> {
	
	private ContainerToolBox<C,B,D,M> tools;
	private C configuration;
	private Map<String, M> modulesByName;
	private List<M> initialized;
	private Map<String, Command<?>> commandsByName;
	private boolean commandsGuard;
	
 	/**
	 * Constructor.
	 * 
	 * @param logger a logger or null
	 * @param mdb the module definition builder to use
	 */
	public ContainerHelper(LoggerBridge logger, B mdb) {
		tools = new ContainerToolBox<C,B,D,M>(logger, mdb);
		initialized = new ArrayList<M>();
	}
	
	public void reset() {
		configuration = null;
		modulesByName = null;
		commandsByName = null;
		commandsGuard = false;
		initialized.clear();
	}
	
	/**
	 * Shutdown all modules. Only those modules which have been initialized
	 * successfully are shutdown. Modules are shut down in the reverse
	 * initialization sequence.
	 * 
	 */
	public void shutdown() {
		tools.shutdown(initialized);
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
		if (modulesByName != null)
			throw new IllegalStateException("cannot call #configure twice without reset");
		List<M> modules = tools.configureModules(configuration);
		modulesByName = tools.asMap(modules);
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
		if (modulesByName == null)
			throw new IllegalStateException("#configure not called successfully");
		if (commandsByName != null)
			throw new IllegalStateException("cannot call #initialize twice without reset");
		SimpleCommandRegistry registry = new SimpleCommandRegistry();
		for (M module : modulesByName.values()) {
			tools.initializeModule(module, configuration.get(module.getName()).getRequirements(), modulesByName, registry);
			initialized.add(module);
		}
		commandsByName = registry.getCommands();
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
	 * @throws IllegalStateException
	 *             if #initialize not called successfully
	 * @throws IllegalStateException
	 *             if the method is called twice without reset
	 * @throws Exception
	 *             execution of commands can throw exceptions
	 */
	public void execute() throws Exception {
		if (commandsByName == null)
			throw new IllegalStateException("#initialize not called successfully");
		if (commandsGuard)
			throw new IllegalStateException("cannot call #execute twice without reset");
		tools.executeCommands(configuration, commandsByName);
		commandsGuard = true;
	}

	@Override
	public Iterator<M> iterator() {
		return new Iterator<M>() {
			private Iterator<M> it = modulesByName == null ? null : modulesByName.values().iterator();
			@Override
			public boolean hasNext() {
				return it == null ? false : it.hasNext();
			}
			@Override
			public M next() {
				if (it == null)
					throw new NoSuchElementException();
				return it.next();
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
	/**
	 * Get a module by name.
	 * 
	 * @param name
	 *            the name of the module, non-null
	 * @return a module, non-null
	 * @throws NoSuchElementException
	 *             if there is no module with that name
	 */
	public M getModule(String name) {
		Misc.nullIllegal(name, "name null");
		M module = modulesByName == null ? null : modulesByName.get(name);
		if (module == null)
			throw new NoSuchElementException(name);
		return module;
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
		Misc.nullIllegal(name, "name null");
		Command<?> command = commandsByName.get(name);
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
		Collection<Command<?>> commands = new ArrayList<Command<?>>();
		for (Command<?> command : commandsByName.values()) {
			if (moduleName == null || moduleName.equals(command.getModule().getName()))
				commands.add(command);
		}
		return commands;
	}
	
}
