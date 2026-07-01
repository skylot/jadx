package jadx.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.xmlgen.ResContainer;
import jadx.plugins.input.dex.DexInputPlugin;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class JadxDecompilerTest {

	@TempDir
	File testDir;

	@Test
	public void testExampleUsage() {
		File sampleApk = getFileFromSampleDir("app-with-fake-dex.apk");

		// test simple apk loading
		JadxArgs args = new JadxArgs();
		args.getInputFiles().add(sampleApk);
		args.setOutDir(testDir);

		try (JadxDecompiler jadx = new JadxDecompiler(args)) {
			jadx.load();
			jadx.save();
			jadx.printErrorsReport();

			// test class print
			for (JavaClass cls : jadx.getClasses()) {
				System.out.println(cls.getCode());
			}

			assertThat(jadx.getClasses()).hasSize(3);
			assertThat(jadx.getErrorsCount()).isEqualTo(0);
		}
	}

	@Test
	public void testDirectDexInput() throws IOException {
		try (JadxDecompiler jadx = new JadxDecompiler();
				InputStream in = new FileInputStream(getFileFromSampleDir("hello.dex"))) {
			jadx.addCustomCodeLoader(new DexInputPlugin().loadDexFromInputStream(in, "input"));
			jadx.load();
			for (JavaClass cls : jadx.getClasses()) {
				System.out.println(cls.getCode());
			}
			assertThat(jadx.getClasses()).hasSize(1);
			assertThat(jadx.getErrorsCount()).isEqualTo(0);
		}
	}

	@Test
	public void testResourcesLoad() {
		File sampleApk = getFileFromSampleDir("app-with-fake-dex.apk");

		JadxArgs args = new JadxArgs();
		args.getInputFiles().add(sampleApk);
		args.setOutDir(testDir);
		args.setSkipSources(true);
		try (JadxDecompiler jadx = new JadxDecompiler(args)) {
			jadx.load();
			List<ResourceFile> resources = jadx.getResources();
			assertThat(resources).hasSize(8);
			ResourceFile arsc = resources.stream()
					.filter(r -> r.getType() == ResourceType.ARSC)
					.findFirst().orElseThrow();
			ResContainer resContainer = arsc.loadContent();
			ResContainer xmlRes = resContainer.getSubFiles().stream()
					.filter(r -> r.getName().equals("res/values/colors.xml"))
					.findFirst().orElseThrow();
			assertThat(xmlRes.getText())
					.code()
					.containsOne("<color name=\"colorPrimary\">#008577</color>");
		}
	}

	private static final String TEST_SAMPLES_DIR = "test-samples/";

	public static File getFileFromSampleDir(String fileName) {
		URL resource = JadxDecompilerTest.class.getClassLoader().getResource(TEST_SAMPLES_DIR + fileName);
		assertThat(resource).isNotNull();
		String pathStr = resource.getFile();
		return new File(pathStr);
	}

	// TODO add more tests

	@Test
	public void testConvertPackageHierarchy() {
		JadxDecompiler jadx = new JadxDecompiler();
		RootNode root = new RootNode(jadx);
		PackageNode leafPkgNode = PackageNode.getOrBuild(root, "com.example.app");
		PackageNode rootPkgNode = leafPkgNode.getParentPkg().getParentPkg();

		JavaPackage rootPkg = jadx.convertPackageNode(rootPkgNode);

		assertThat(rootPkg.getFullName()).isEqualTo("com");
		assertThat(rootPkg.isRoot()).isTrue();
		assertThat(rootPkg.isLeaf()).isFalse();
		assertThat(rootPkg.getSubPackages())
				.extracting(JavaPackage::getFullName)
				.containsExactly("com.example");
		JavaPackage middlePkg = rootPkg.getSubPackages().get(0);
		assertThat(middlePkg.getFullName()).isEqualTo("com.example");
		assertThat(middlePkg.getSubPackages())
				.extracting(JavaPackage::getFullName)
				.containsExactly("com.example.app");
		JavaPackage leafPkg = middlePkg.getSubPackages().get(0);
		assertThat(leafPkg.getFullName()).isEqualTo("com.example.app");
		assertThat(leafPkg.isLeaf()).isTrue();
	}

	@Test
	public void testGetJavaNodeByRefReusesConvertedPackage() {
		JadxDecompiler jadx = new JadxDecompiler();
		RootNode root = new RootNode(jadx);
		PackageNode pkgNode = PackageNode.getOrBuild(root, "com.example");

		JavaNode javaNode = jadx.getJavaNodeByRef(pkgNode);

		assertThat(javaNode).isNotNull();
		assertThat(javaNode.getCodeNodeRef()).isSameAs(pkgNode);
		assertThat(jadx.getJavaNodeByRef(pkgNode)).isSameAs(javaNode);
		assertThat(jadx.convertPackageNode(pkgNode)).isSameAs(javaNode);
	}
}
