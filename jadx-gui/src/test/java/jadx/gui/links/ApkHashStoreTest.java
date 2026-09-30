package jadx.gui.links;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class ApkHashStoreTest {

	@TempDir
	Path tempDir;

	@Test
	void recordFindPersist() throws Exception {
		Path storeFile = tempDir.resolve("apk-hashes.json");
		Path apk = tempDir.resolve("app.apk");
		Files.writeString(apk, "hello");
		String expected = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"; // sha256("hello")

		ApkHashStore store = new ApkHashStore(storeFile);
		assertThat(store.record(apk)).isEqualTo(expected);
		assertThat(store.findByHashPrefix("2CF24DBA5F")).isEqualTo(apk.toAbsolutePath());
		assertThat(store.findByHashPrefix("0000000000")).isNull();

		// reloaded from disk
		ApkHashStore reloaded = new ApkHashStore(storeFile);
		assertThat(reloaded.entries()).hasSize(1);
		assertThat(reloaded.entries().get(0).getHash()).isEqualTo(expected);

		// content changed => no longer matches
		Files.writeString(apk, "changed");
		assertThat(reloaded.findByHashPrefix("2cf24dba5f")).isNull();

		reloaded.remove(apk.toAbsolutePath().normalize().toString());
		assertThat(new ApkHashStore(storeFile).entries()).isEmpty();
	}

	@Test
	void isApk() {
		assertThat(ApkHashStore.isApk(Path.of("a/App.APK"))).isTrue();
		assertThat(ApkHashStore.isApk(Path.of("a/app.dex"))).isFalse();
	}
}
