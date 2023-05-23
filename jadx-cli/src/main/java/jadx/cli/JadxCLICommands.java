package jadx.cli;

import java.util.Map;
import java.util.TreeMap;

import com.beust.jcommander.JCommander;

import jadx.cli.commands.CommandPlugins;
import jadx.cli.commands.ICommand;

public class JadxCLICommands {
	private static final Map<String, ICommand> COMMANDS_MAP = new TreeMap<>();

	static {
		JadxCLICommands.register(new CommandPlugins());
	}

	public static void register(ICommand command) {
		COMMANDS_MAP.put(command.name(), command);
	}

	public static void append(JCommander.Builder builder) {
		COMMANDS_MAP.forEach(builder::addCommand);
	}

	public static boolean process(JCommanderWrapper<?> jcw, JCommander jc, String parsedCommand) {
		ICommand command = COMMANDS_MAP.get(parsedCommand);
		if (command == null) {
			throw new IllegalArgumentException("Unknown command: " + parsedCommand);
		}
		JCommander subCommander = jc.getCommands().get(parsedCommand);
		command.process(jcw, subCommander);
		return true;
	}
}
