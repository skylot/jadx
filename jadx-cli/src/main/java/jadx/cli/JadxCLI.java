package jadx.cli;

import jadx.api.Decompiler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JadxCLI {
	private static final Logger LOG = LoggerFactory.getLogger(JadxCLI.class);

	public static void main(String[] args) {
		JadxArgs jadxArgs = new JadxArgs(args, true);
		Decompiler jadx = new Decompiler(jadxArgs);
		jadx.processAndSaveAll();
		System.exit(jadx.getErrorsCount());
	}
}
