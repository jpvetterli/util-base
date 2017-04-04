package ch.agent.util.ioc;

import static ch.agent.util.STRINGS.msg;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import ch.agent.util.STRINGS.U;

/**
 * The configuration registry is produced by the configuration procedure. It is
 * the result returned by {@link Configuration#configure}. It provides a map of
 * all modules configured and a map of all commands registered by the modules.
 * The iteration order of the module map in which modules have been added.
 * <p>
 * Modules and commands are added directly to the maps returned by
 * {@link ConfigurationRegistry#getModules} and {@link #getCommands}. The
 * {@link #addUnique} method can be used to attempt to add command with an
 * exception thrown if the command already exists.
 * 
 * 
 * @param <M>
 *            the module type
 */
public class ConfigurationRegistry<M extends Module<?>> {
	
	private Map<String, M> modules;
	private Map<String, CommandSpecification> commands;
	
	/**
	 * Constructor.
	 */
	public ConfigurationRegistry() {
		modules = new LinkedHashMap<String, M>();
		commands = new HashMap<String, CommandSpecification>();
	}
	
	/**
	 * Get the module map. The map is keyed by {@link Module#getName}.
	 * 
	 * @return a name-to-module map
	 */
	public Map<String, M> getModules() {
		return modules;
	}

	/**
	 * Get the command map. The map is keyed by
	 * {@link CommandSpecification#getName} which combines the module and
	 * command names.
	 * 
	 * @return a name-to-command map
	 */
	public Map<String, CommandSpecification> getCommands() {
		return commands;
	}

	/**
	 * Add a command to the registry.
	 * 
	 * @param command a command
	 * @throws ConfigurationException if the command is already in the registry
	 */
	public void addUnique(String module, String command) {
		CommandSpecification spec = new CommandSpecification(module, command);
		CommandSpecification existing = commands.get(spec.getName());
		if (existing != null)
			throw new ConfigurationException(msg(U.C12, module, command));
		commands.put(spec.getName(), spec);
	}
	
}
