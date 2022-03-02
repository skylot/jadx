package jadx.plugins.input.javaconvert;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.android.tools.r8.CompilationFailedException;
import com.android.tools.r8.CompilationMode;
import com.android.tools.r8.D8;
import com.android.tools.r8.D8Command;
import com.android.tools.r8.Diagnostic;
import com.android.tools.r8.DiagnosticsHandler;
import com.android.tools.r8.OutputMode;

public class D8Converter {
	private static final Logger LOG = LoggerFactory.getLogger(D8Converter.class);

	public static void run(Path path, Path tempDirectory, JavaConvertOptions options) throws CompilationFailedException {
		D8Command d8Command = D8Command.builder(new LogHandler())
				.addProgramFiles(path)
				.setOutput(tempDirectory, OutputMode.DexIndexed)
				.setMode(CompilationMode.DEBUG)
				.setMinApiLevel(30)
				.setIntermediate(true)
				.setDisableDesugaring(!options.isD8Desugar())
				.build();
		D8.run(d8Command);
	}

	private static class LogHandler implements DiagnosticsHandler {
		@Override
		public void error(Diagnostic diagnostic) {
			LOG.error("D8 error: {}", format(diagnostic));
		}

		@Override
		public void warning(Diagnostic diagnostic) {
			LOG.warn("D8 warning: {}", format(diagnostic));
		}

		@Override
		public void info(Diagnostic diagnostic) {
			LOG.info("D8 info: {}", format(diagnostic));
		}

		public static String format(Diagnostic diagnostic) {
			return diagnostic.getDiagnosticMessage()
					+ ", origin: " + diagnostic.getOrigin()
					+ ", position: " + diagnostic.getPosition();
		}
	}
}
