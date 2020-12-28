package jadx.plugins.input.javaconvert;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.android.tools.r8.CompilationMode;
import com.android.tools.r8.D8;
import com.android.tools.r8.D8Command;
import com.android.tools.r8.Diagnostic;
import com.android.tools.r8.DiagnosticsHandler;
import com.android.tools.r8.OutputMode;

public class D8Converter {
	private static final Logger LOG = LoggerFactory.getLogger(D8Converter.class);

	public static void run(Path pathToBeConverted, Path outputDirectory) {
		LOG.info("Converting {} to dex - output dir {}", pathToBeConverted, outputDirectory);
		D8Command.Builder builder = D8Command.builder(new LogHelper()).addProgramFiles(pathToBeConverted)
				.setMinApiLevel(28)
				.setMode(CompilationMode.DEBUG)
				.setDisableDesugaring(true)
				.setOutput(outputDirectory, OutputMode.DexIndexed);
		try {
			D8Command command = builder.build();
			D8.run(command);
		} catch (Exception e) {
			throw new RuntimeException("d8 conversion failed: " + e.getMessage(), e);
		}
	}

	private static class LogHelper implements DiagnosticsHandler {

		@Override
		public void error(Diagnostic diagnostic) {
			LOG.error(diagnosticToString(diagnostic));
		}

		@Override
		public void warning(Diagnostic diagnostic) {
			LOG.warn(diagnosticToString(diagnostic));
		}

		@Override
		public void info(Diagnostic diagnostic) {
			LOG.info(diagnosticToString(diagnostic));
		}

		private static String diagnosticToString(Diagnostic diagnostic) {
			return "D8:" + diagnostic.getDiagnosticMessage() + " origin=" + diagnostic.getOrigin() + " position="
					+ diagnostic.getPosition();
		}

	}
}
