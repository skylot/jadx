package jadx.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxDecompiler;

public class JadxCLI {
	private static final Logger LOG = LoggerFactory.getLogger(JadxCLI.class);

	public static void main(String[] args) {
		try {
			JadxCLIArgs jadxArgs = new JadxCLIArgs();
			if (jadxArgs.processArgs(args)) {
				processAndSave(jadxArgs);
			}
		} catch (Exception e) {
			LOG.error("jadx error: {}", e.getMessage(), e);
			System.exit(1);
		}
	}

	static void processAndSave(JadxCLIArgs inputArgs) {
		JadxDecompiler jadx = new JadxDecompiler(inputArgs.toJadxArgs());
		jadx.load();
		jadx.save();
		if (jadx.getErrorsCount() != 0) {
			jadx.printErrorsReport();
			LOG.error("finished with errors");
		} else {
			LOG.info("done");
		}
	}
}
