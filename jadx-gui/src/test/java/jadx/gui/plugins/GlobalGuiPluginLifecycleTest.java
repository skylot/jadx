package jadx.gui.plugins;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.gui.plugins.JadxGlobalGuiPlugin;
import jadx.api.gui.plugins.JadxGuiContextExt;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.loader.JadxBasePluginLoader;
import jadx.core.plugins.PluginContext;
import jadx.gui.plugins.context.TestMainWindowShim;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobalGuiPluginLifecycleTest {

	@TempDir
	Path tmp;

	@Test
	public void lifecycleAcrossSeveralProjects() {
		GuiPluginsManager manager = new GuiPluginsManager(TestMainWindowShim.build());
		RecordingGlobalPlugin plugin = new RecordingGlobalPlugin("counting-global-plugin");
		manager.loadGlobalPlugins(new JadxArgs(), new TestPluginsLoader(plugin));

		assertThat(plugin.globalInitCount).isEqualTo(1);
		assertThat(plugin.projectInitCount).isEqualTo(0);
		assertThat(plugin.unloadCount).isEqualTo(0);
		assertThat(plugin.globalUnloadCount).isEqualTo(0);

		// open and close two projects
		for (int i = 1; i <= 2; i++) {
			try (JadxDecompiler projectDecompiler = new JadxDecompiler()) {
				manager.initGuiPluginsContext(projectDecompiler);
				manager.injectGlobalPlugins(projectDecompiler);
				projectDecompiler.getPluginManager().initResolved();

				assertThat(plugin.projectInitCount).isEqualTo(i);
				assertThat(projectDecompiler.getPluginManager().getAllPluginContexts().first()
						.getPluginInstance()).isSameAs(plugin);

				// project init receives context bound to project decompiler
				JadxPluginContext projectContext = plugin.projectContexts.get(i - 1);
				assertThat(projectContext.getDecompiler()).isSameAs(projectDecompiler);
				assertThat(projectContext.getArgs()).isSameAs(projectDecompiler.getArgs());
				assertThat(projectContext.getGuiContext()).isInstanceOf(JadxGuiContextExt.class);
			}
			manager.resetProjectScope();
			assertThat(plugin.unloadCount).isEqualTo(i);
			assertThat(plugin.globalInitCount).isEqualTo(1);
			assertThat(plugin.globalUnloadCount).isEqualTo(0);
		}

		// jadx-gui exit
		manager.runGlobalUnload();
		assertThat(plugin.globalUnloadCount).isEqualTo(1);
		assertThat(plugin.globalInitCount).isEqualTo(1);
		assertThat(plugin.projectInitCount).isEqualTo(2);
		assertThat(plugin.unloadCount).isEqualTo(2);
	}

	@Test
	public void projectLoadAndReloadPasses() throws Exception {
		GuiPluginsManager manager = new GuiPluginsManager(TestMainWindowShim.build());
		RecordingGlobalPlugin plugin = new RecordingGlobalPlugin("counting-global-plugin");
		manager.loadGlobalPlugins(new JadxArgs(), new TestPluginsLoader(plugin));

		Path smali = tmp.resolve("HelloWorld.smali");
		Files.write(smali, (".class Lsmali/HelloWorld;\n"
				+ ".super Ljava/lang/Object;\n").getBytes(StandardCharsets.UTF_8));
		JadxArgs projectArgs = new JadxArgs();
		projectArgs.getInputFiles().add(smali.toFile());
		projectArgs.setOutDir(tmp.resolve("out").toFile());
		// load project plugins from classpath, global plugins will be injected
		List<JadxPlugin> projectPlugins = new JadxBasePluginLoader().load();
		projectPlugins.removeIf(p -> p instanceof JadxGlobalGuiPlugin);
		projectArgs.setPluginLoader(new TestPluginsLoader(projectPlugins));

		try (JadxDecompiler projectDecompiler = new JadxDecompiler(projectArgs)) {
			manager.initGuiPluginsContext(projectDecompiler);
			manager.injectGlobalPlugins(projectDecompiler);
			projectDecompiler.load();
			assertThat(projectDecompiler.getClasses()).hasSize(1);
			assertThat(plugin.projectInitCount).isEqualTo(1);

			manager.resetProjectScope();
			projectDecompiler.reloadPasses();
			assertThat(plugin.projectInitCount).isEqualTo(2);
			assertThat(plugin.unloadCount).isEqualTo(1);
			assertThat(projectDecompiler.getPluginManager().getResolvedPluginContexts())
					.extracting(PluginContext::getPluginId)
					.contains("counting-global-plugin");
		}
		assertThat(plugin.unloadCount).isEqualTo(2);
		assertThat(plugin.globalInitCount).isEqualTo(1);
		assertThat(plugin.globalUnloadCount).isEqualTo(0);
	}
}
