package jadx;

import jadx.utils.exceptions.JadxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		JadxArgs jadxArgs = new JadxArgs();
		jadxArgs.parse(args);
		if (jadxArgs.isPrintHelp()) {
			jadxArgs.printUsage();
			System.exit(0);
		}

		if (jadxArgs.isVerbose()) {
			ch.qos.logback.classic.Logger rootLogger =
					(ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
			rootLogger.setLevel(ch.qos.logback.classic.Level.DEBUG);
		}

		try {
			jadxArgs.processArgs();
		} catch (JadxException e) {
			LOG.error("Error: " + e.getMessage());
			System.exit(1);
		}

		int result = Jadx.run(jadxArgs);
		System.exit(result);
	}
}
