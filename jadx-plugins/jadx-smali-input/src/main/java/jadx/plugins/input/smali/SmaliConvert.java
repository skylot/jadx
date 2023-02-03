package jadx.plugins.input.smali;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.jf.smali.Smali;
import org.jf.smali.SmaliOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmaliConvert implements Closeable {
	private static final Logger LOG = LoggerFactory.getLogger(SmaliConvert.class);

	@Nullable
	private Path tmpDex;

	public boolean execute(List<Path> input) {
		List<Path> smaliFiles = filterSmaliFiles(input);
		if (smaliFiles.isEmpty()) {
			return false;
		}
		LOG.debug("Compiling smali files: {}", smaliFiles.size());
		try {
			this.tmpDex = Files.createTempFile("jadx-", ".dex");
			if (compileSmali(tmpDex, smaliFiles)) {
				return true;
			}
		} catch (Exception e) {
			LOG.error("Smali process error", e);
		}
		close();
		return false;
	}

	private static boolean compileSmali(Path output, List<Path> inputFiles) throws IOException {
		SmaliOptions options = new SmaliOptions();
		options.outputDexFile = output.toAbsolutePath().toString();
		options.verboseErrors = true;
		options.apiLevel = 27; // TODO: add as plugin option

		List<String> inputFileNames = inputFiles.stream()
				.map(p -> p.toAbsolutePath().toString())
				.distinct()
				.collect(Collectors.toList());

		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			boolean result = collectSystemErrors(out, () -> Smali.assemble(options, inputFileNames));
			if (!result) {
				LOG.error("Smali compilation error:\n{}", out);
			}
			return result;
		}
	}

	private static boolean collectSystemErrors(OutputStream out, Callable<Boolean> exec) {
		PrintStream systemErr = System.err;
		try (PrintStream err = new PrintStream(out)) {
			System.setErr(err);
			try {
				return exec.call();
			} catch (Exception e) {
				e.printStackTrace(err);
				return false;
			}
		} finally {
			System.setErr(systemErr);
		}
	}

	private List<Path> filterSmaliFiles(List<Path> input) {
		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**.smali");
		return input.stream()
				.filter(matcher::matches)
				.collect(Collectors.toList());
	}

	public List<Path> getDexFiles() {
		if (tmpDex == null) {
			return Collections.emptyList();
		}
		return Collections.singletonList(tmpDex);
	}

	@Override
	public void close() {
		try {
			if (tmpDex != null) {
				Files.deleteIfExists(tmpDex);
			}
		} catch (Exception e) {
			LOG.error("Failed to remove tmp dex file: {}", tmpDex, e);
		}
	}
}
