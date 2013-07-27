package jadx.cli;

import jadx.api.Decompiler;

public class JadxCLI {

	public static void main(String[] args) {
		JadxArgs jadxArgs = new JadxArgs(args, true);
		Decompiler jadx = new Decompiler(jadxArgs);
		jadx.processAndSaveAll();
		System.exit(jadx.getErrorsCount());
	}
}
