package jadx.gui.utils.cache.code;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.api.impl.NoOpCodeCache;
import jadx.core.dex.nodes.ClassNode;
import jadx.gui.cache.code.disk.DiskCodeCache;
import jadx.tests.api.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

class DiskCodeCacheTest extends IntegrationTest {
	private static final Logger LOG = LoggerFactory.getLogger(DiskCodeCacheTest.class);

	@TempDir
	public Path tempDir;

	@Test
	public void test() throws IOException {
		disableCompilation();
		getArgs().setCodeCache(NoOpCodeCache.INSTANCE);
		ClassNode clsNode = getClassNode(DiskCodeCacheTest.class);
		ICodeInfo codeInfo = clsNode.getCode();

		DiskCodeCache cache = new DiskCodeCache(clsNode.root(), tempDir);

		String clsKey = clsNode.getFullName();
		cache.add(clsKey, codeInfo);

		ICodeInfo readCodeInfo = cache.get(clsKey);

		assertThat(readCodeInfo).isNotNull();
		assertThat(readCodeInfo.getCodeStr()).isEqualTo(codeInfo.getCodeStr());
		assertThat(readCodeInfo.getCodeMetadata().getLineMapping()).isEqualTo(codeInfo.getCodeMetadata().getLineMapping());
		LOG.info("Disk code annotations: {}", readCodeInfo.getCodeMetadata().getAsMap());
		assertThat(readCodeInfo.getCodeMetadata().getAsMap()).hasSameSizeAs(codeInfo.getCodeMetadata().getAsMap());

		cache.close();
	}
}
