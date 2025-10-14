package jadx.plugins.mappings;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.api.JavaClass;
import jadx.api.plugins.loader.JadxBasePluginLoader;
import jadx.core.plugins.files.SingleDirFilesGetter;

import static org.assertj.core.api.Assertions.assertThat;

public class BaseRenameMappingsTest {
	private static final Logger LOG = LoggerFactory.getLogger(BaseRenameMappingsTest.class);

	@TempDir
	Path testDir;

	Path outputDir;

	JadxArgs jadxArgs;

	String testResDir = "";

	@BeforeEach
	public void setUp() {
		outputDir = testDir.resolve("output");
		jadxArgs = new JadxArgs();
		jadxArgs.setOutDir(outputDir.toFile());
		jadxArgs.setFilesGetter(new SingleDirFilesGetter(testDir));
		jadxArgs.setPluginLoader(new JadxBasePluginLoader());
	}

	public File loadResourceFile(String fileName) {
		String path = testResDir + '/' + fileName;
		try {
			URL resource = getClass().getClassLoader().getResource(path);
			assertThat(resource).isNotNull();
			return new File(resource.getFile());
		} catch (Exception e) {
			throw new RuntimeException("Failed to load resource file: " + path, e);
		}
	}

	public void printClassesCode(List<JavaClass> classes) {
		LOG.debug("Printing code for {} classes:", classes.size());
		for (JavaClass jCls : classes) {
			LOG.debug("Class: {}\n{}\n---\n", jCls.getFullName(), jCls.getCode());
		}
	}
}
