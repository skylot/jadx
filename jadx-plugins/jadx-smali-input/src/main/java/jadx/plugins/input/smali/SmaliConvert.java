package jadx.plugins.input.smali;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.android.tools.smali.smali.SmaliOptions;

import jadx.plugins.input.dex.utils.IDexData;
import jadx.plugins.input.dex.utils.SimpleDexData;

public class SmaliConvert {
	private static final Logger LOG = LoggerFactory.getLogger(SmaliConvert.class);

	private final List<IDexData> dexData = new ArrayList<>();

	public boolean execute(List<Path> input, SmaliInputOptions options) {
		List<Path> smaliFiles = filterSmaliFiles(input);
		if (smaliFiles.isEmpty()) {
			return false;
		}
		try {
			compile(smaliFiles, options);
		} catch (Exception e) {
			LOG.error("Smali process error", e);
		}
		return !dexData.isEmpty();
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private void compile(List<Path> inputFiles, SmaliInputOptions options) {
		SmaliOptions smaliOptions = new SmaliOptions();
		smaliOptions.apiLevel = options.getApiLevel();
		smaliOptions.verboseErrors = true;
		smaliOptions.allowOdexOpcodes = false;
		smaliOptions.printTokens = false;

		int threads = options.getThreads();
		LOG.debug("Compiling smali files: {}, threads: {}", inputFiles.size(), threads);
		long start = System.currentTimeMillis();
		if (threads == 1 || inputFiles.size() == 1) {
			for (Path inputFile : inputFiles) {
				assemble(inputFile, smaliOptions);
			}
		} else {
			try {
				ExecutorService executor = Executors.newFixedThreadPool(threads);
				for (Path inputFile : inputFiles) {
					executor.execute(() -> assemble(inputFile, smaliOptions));
				}
				executor.shutdown();
				executor.awaitTermination(1, TimeUnit.DAYS);
			} catch (InterruptedException e) {
				LOG.error("Smali compile interrupted", e);
			}
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("Smali compile done in: {}ms", System.currentTimeMillis() - start);
		}
	}

	private void assemble(Path inputFile, SmaliOptions smaliOptions) {
		Path path = inputFile.toAbsolutePath();
		try {
			byte[] dexContent = SmaliUtils.assemble(path.toFile(), smaliOptions);
			dexData.add(new SimpleDexData(path.toString(), dexContent));
		} catch (Exception e) {
			LOG.error("Failed to assemble smali file: {}", path, e);
		}
	}

	private static void collectSystemErrors(OutputStream out, Runnable exec) {
		PrintStream systemErr = System.err;
		try (PrintStream err = new PrintStream(out)) {
			System.setErr(err);
			try {
				exec.run();
			} catch (Exception e) {
				e.printStackTrace(err);
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

	public List<IDexData> getDexData() {
		return dexData;
	}
}
