package jadx.gui.settings;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.swing.JFrame;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jadx.cli.config.JadxConfigAdapter;
import jadx.commons.app.JadxCommonFiles;
import jadx.gui.utils.NLS;

import static org.assertj.core.api.Assertions.assertThat;

class JadxSettingsVolatileTest {

	@BeforeAll
	static void init() {
		NLS.setLocale(NLS.defaultLocale());
	}

	@Test
	void testIndependentSyncAndReload(@TempDir Path tempDir) throws Exception {
		Path baseConfig = tempDir.resolve("gui.json");
		Path volatileConfig = tempDir.resolve("gui-volatile.json");

		JadxSettings settings = createSettings(baseConfig, volatileConfig);
		settings.loadSettingsData(new JadxSettingsData());

		Path projectPath = tempDir.resolve("sample.jadx").toAbsolutePath().normalize();
		Path openDir = tempDir.resolve("open-dir").toAbsolutePath().normalize();
		Path saveDir = tempDir.resolve("save-dir").toAbsolutePath().normalize();
		Path saveProjectDir = tempDir.resolve("save-project-dir").toAbsolutePath().normalize();

		settings.setSearchResultsPerPage(123);
		settings.setFlattenPackage(true);
		settings.addRecentProject(projectPath);
		settings.setLastOpenFilePath(openDir);
		settings.setLastSaveFilePath(saveDir);
		settings.setLastSaveProjectPath(saveProjectDir);
		settings.setMainWindowExtendedState(JFrame.MAXIMIZED_BOTH);

		// sync() only writes persistent config
		settings.sync();
		assertThat(baseConfig).isRegularFile();
		assertThat(volatileConfig).doesNotExist();

		String baseJson = Files.readString(baseConfig);
		assertThat(baseJson).contains("\"searchResultsPerPage\": 123");
		assertThat(baseJson).contains("\"flattenPackage\": true");
		assertThat(baseJson).doesNotContain("recentProjects");
		assertThat(baseJson).doesNotContain("lastOpenFilePath");
		assertThat(baseJson).doesNotContain("lastSaveFilePath");
		assertThat(baseJson).doesNotContain("lastSaveProjectPath");
		assertThat(baseJson).doesNotContain("windowPos");
		assertThat(baseJson).doesNotContain("mainWindowExtendedState");

		// syncVolatile() only writes volatile config
		settings.setSearchResultsPerPage(456);
		settings.syncVolatile();
		assertThat(volatileConfig).isRegularFile();

		// base config was not rewritten by syncVolatile()
		assertThat(Files.readString(baseConfig)).contains("\"searchResultsPerPage\": 123");

		String volatileJson = Files.readString(volatileConfig);
		assertThat(volatileJson).contains("recentProjects");
		assertThat(volatileJson).contains("sample.jadx");
		assertThat(volatileJson).contains("lastOpenFilePath");
		assertThat(volatileJson).contains("mainWindowExtendedState");
		assertThat(volatileJson).doesNotContain("searchResultsPerPage");
		assertThat(volatileJson).doesNotContain("flattenPackage");

		// syncAll() writes both and reloads cleanly
		settings.syncAll();

		JadxConfigAdapter<JadxSettingsData> reloadBaseAdapter = JadxSettings.buildConfigAdapter();
		reloadBaseAdapter.useConfigRef(baseConfig.toString());
		JadxConfigAdapter<JadxVolatileSettingsData> reloadVolatileAdapter = JadxSettings.buildVolatileConfigAdapter();
		reloadVolatileAdapter.useConfigRef(volatileConfig.toString());

		JadxSettings reloaded = new JadxSettings(reloadBaseAdapter, reloadVolatileAdapter);
		JadxSettingsData loadedData = reloadBaseAdapter.load();
		assertThat(loadedData).isNotNull();
		reloaded.loadSettingsData(loadedData);

		assertThat(reloaded.getSearchResultsPerPage()).isEqualTo(456);
		assertThat(reloaded.isFlattenPackage()).isTrue();
		assertThat(reloaded.getRecentProjects()).containsExactly(projectPath);
		assertThat(reloaded.getLastOpenFilePath()).isEqualTo(openDir);
		assertThat(reloaded.getLastSaveFilePath()).isEqualTo(saveDir);
		assertThat(reloaded.getLastSaveProjectPath()).isEqualTo(saveProjectDir);
		assertThat(reloaded.getMainWindowExtendedState()).isEqualTo(JFrame.MAXIMIZED_BOTH);
	}

	@Test
	void testMigrateFromLegacySingleConfigFile(@TempDir Path tempDir) throws Exception {
		Path baseConfig = tempDir.resolve("gui.json");
		Path volatileConfig = tempDir.resolve("gui-volatile.json");
		Path projectPath = tempDir.resolve("legacy.jadx").toAbsolutePath().normalize();
		Path openDir = tempDir.resolve("legacy-open").toAbsolutePath().normalize();

		JadxVolatileSettingsData legacyVolatile = new JadxVolatileSettingsData();
		legacyVolatile.setRecentProjects(List.of(projectPath));
		legacyVolatile.setLastOpenFilePath(openDir);
		legacyVolatile.setMainWindowExtendedState(JFrame.MAXIMIZED_HORIZ);
		legacyVolatile.getWindowPos().put("MainWindow", new WindowLocation("MainWindow", new Rectangle(10, 20, 800, 600)));

		JadxConfigAdapter<JadxVolatileSettingsData> tempVolatileAdapter = JadxSettings.buildVolatileConfigAdapter();
		String volatilePart = tempVolatileAdapter.objectToJsonString(legacyVolatile);
		// Build a legacy single-file config combining persistent + volatile fields (and older
		// settingsVersion)
		String legacyJson = "{\n"
				+ "  \"settingsVersion\": 22,\n"
				+ "  \"searchResultsPerPage\": 77,\n"
				+ "  \"flattenPackage\": true,\n"
				+ volatilePart.substring(volatilePart.indexOf('{') + 1);
		Files.writeString(baseConfig, legacyJson);
		assertThat(volatileConfig).doesNotExist();

		JadxConfigAdapter<JadxSettingsData> baseAdapter = JadxSettings.buildConfigAdapter();
		baseAdapter.useConfigRef(baseConfig.toString());
		JadxConfigAdapter<JadxVolatileSettingsData> volatileAdapter = JadxSettings.buildVolatileConfigAdapter();
		volatileAdapter.useConfigRef(volatileConfig.toString());

		JadxSettings settings = new JadxSettings(baseAdapter, volatileAdapter);
		JadxSettingsData loadedData = baseAdapter.load();
		assertThat(loadedData).isNotNull();
		settings.loadSettingsData(loadedData);

		assertThat(settings.getSearchResultsPerPage()).isEqualTo(77);
		assertThat(settings.isFlattenPackage()).isTrue();
		assertThat(settings.getRecentProjects()).containsExactly(projectPath);
		assertThat(settings.getLastOpenFilePath()).isEqualTo(openDir);
		assertThat(settings.getMainWindowExtendedState()).isEqualTo(JFrame.MAXIMIZED_HORIZ);

		assertThat(volatileConfig).isRegularFile();
		String migratedBaseJson = Files.readString(baseConfig);
		assertThat(migratedBaseJson).contains("\"searchResultsPerPage\": 77");
		assertThat(migratedBaseJson).contains("\"settingsVersion\": " + JadxSettingsData.CURRENT_SETTINGS_VERSION);
		assertThat(migratedBaseJson).doesNotContain("recentProjects");
		assertThat(migratedBaseJson).doesNotContain("windowPos");

		String migratedVolatileJson = Files.readString(volatileConfig);
		assertThat(migratedVolatileJson).contains("legacy.jadx");
		assertThat(migratedVolatileJson).contains("MainWindow");
	}

	@Test
	void testFixedVolatileFilenameRegardlessOfBaseConfigName(@TempDir Path tempDir) {
		JadxConfigAdapter<JadxVolatileSettingsData> volatileAdapter = JadxSettings.buildVolatileConfigAdapter();
		assertThat(volatileAdapter.getDefaultConfigFileName()).isEqualTo("gui-volatile.json");
		volatileAdapter.useConfigRef("");
		assertThat(volatileAdapter.getConfigPath())
				.isEqualTo(JadxCommonFiles.getConfigDir().resolve("gui-volatile.json"));

		JadxConfigAdapter<JadxSettingsData> customBaseAdapter = JadxSettings.buildConfigAdapter();
		customBaseAdapter.useConfigRef(tempDir.resolve("custom-profile.json").toString());
		JadxSettings settings = new JadxSettings(customBaseAdapter);
		assertThat(settings.getVolatileConfigPath())
				.isEqualTo(JadxCommonFiles.getConfigDir().resolve("gui-volatile.json"));
	}

	@Test
	void testResetPersistentSettingsPreservesVolatileSettings(@TempDir Path tempDir) {
		Path baseConfig = tempDir.resolve("gui.json");
		Path volatileConfig = tempDir.resolve("gui-volatile.json");
		Path projectPath = tempDir.resolve("keep-me.jadx").toAbsolutePath().normalize();

		JadxSettings settings = createSettings(baseConfig, volatileConfig);
		settings.loadSettingsData(new JadxSettingsData());
		settings.setSearchResultsPerPage(99);
		settings.addRecentProject(projectPath);

		// Simulate Preferences -> Reset
		settings.loadSettingsData(new JadxSettingsData());

		assertThat(settings.getSearchResultsPerPage()).isEqualTo(50);
		assertThat(settings.getRecentProjects()).containsExactly(projectPath);
	}

	@Test
	void testCorruptedVolatileFileFallsBackToDefaults(@TempDir Path tempDir) throws Exception {
		Path baseConfig = tempDir.resolve("gui.json");
		Path volatileConfig = tempDir.resolve("gui-volatile.json");
		Files.writeString(volatileConfig, "{ invalid json");

		JadxSettings settings = createSettings(baseConfig, volatileConfig);
		settings.loadSettingsData(new JadxSettingsData());

		assertThat(settings.getRecentProjects()).isEmpty();
		assertThat(settings.getMainWindowExtendedState()).isEqualTo(JFrame.NORMAL);
	}

	private static JadxSettings createSettings(Path baseConfig, Path volatileConfig) {
		JadxConfigAdapter<JadxSettingsData> baseAdapter = JadxSettings.buildConfigAdapter();
		baseAdapter.useConfigRef(baseConfig.toString());
		JadxConfigAdapter<JadxVolatileSettingsData> volatileAdapter = JadxSettings.buildVolatileConfigAdapter();
		volatileAdapter.useConfigRef(volatileConfig.toString());
		return new JadxSettings(baseAdapter, volatileAdapter);
	}
}
