package jadx.plugins.input.raung;

import java.io.Closeable;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.skylot.raung.asm.RaungAsm;

public class RaungConvert implements Closeable {
	private static final Logger LOG = LoggerFactory.getLogger(RaungConvert.class);

	@Nullable
	private Path tmpJar;

	public boolean execute(List<Path> input) {
		List<Path> raungInputs = filterRaungFiles(input);
		if (raungInputs.isEmpty()) {
			return false;
		}
		try {
			this.tmpJar = Files.createTempFile("jadx-raung-", ".jar");
			RaungAsm.create()
					.output(tmpJar)
					.inputs(input)
					.execute();
			return true;
		} catch (Exception e) {
			LOG.error("Raung process error", e);
		}
		close();
		return false;
	}

	private List<Path> filterRaungFiles(List<Path> input) {
		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**.raung");
		return input.stream()
				.filter(matcher::matches)
				.collect(Collectors.toList());
	}

	public List<Path> getFiles() {
		if (tmpJar == null) {
			return Collections.emptyList();
		}
		return Collections.singletonList(tmpJar);
	}

	@Override
	public void close() {
		try {
			if (tmpJar != null) {
				Files.deleteIfExists(tmpJar);
			}
		} catch (Exception e) {
			LOG.error("Failed to remove tmp jar file: {}", tmpJar, e);
		}
	}
}
