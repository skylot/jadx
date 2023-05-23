package jadx.cli.commands;

import com.beust.jcommander.JCommander;

import jadx.cli.JCommanderWrapper;

public interface ICommand {
	String name();

	void process(JCommanderWrapper<?> jcw, JCommander subCommander);
}
