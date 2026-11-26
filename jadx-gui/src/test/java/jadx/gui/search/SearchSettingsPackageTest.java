package jadx.gui.search;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.data.impl.JadxCodeData;
import jadx.api.data.impl.JadxCodeRename;
import jadx.api.data.impl.JadxNodeRef;
import jadx.gui.JadxWrapper;
import jadx.gui.plugins.context.TestMainWindowShim;
import jadx.gui.treemodel.JPackage;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.NLS;
import jadx.gui.utils.pkgs.PackageHelper;

import static org.assertj.core.api.Assertions.assertThat;

class SearchSettingsPackageTest {

	@BeforeAll
	static void initNls() {
		NLS.setLocale(NLS.defaultLocale());
	}

	@Test
	void testUnrenamedAndLeafRenamedPackages(@TempDir Path tempDir) throws Exception {
		try (JadxDecompiler decompiler = loadTestDecompiler(tempDir)) {
			// Unrenamed packages
			assertThat(filterClasses(decompiler, "com"))
					.containsExactlyInAnyOrder("com.oldpkg.Alpha", "com.oldpkg.Alpha.Inner", "com.oldpkg.sub.Beta");
			assertThat(filterClasses(decompiler, "com.oldpkg"))
					.containsExactlyInAnyOrder("com.oldpkg.Alpha", "com.oldpkg.Alpha.Inner", "com.oldpkg.sub.Beta");
			assertThat(filterClasses(decompiler, "com.oldpkg.sub"))
					.containsExactly("com.oldpkg.sub.Beta");
			assertThat(preparePkgFilter(decompiler, "com.old")).isEqualTo(NLS.str("search_dialog.package_not_found"));

			// Whole-package leaf rename: com.oldpkg -> com.renamedpkg
			applyRenames(decompiler, List.of(
					new JadxCodeRename(JadxNodeRef.forPkg("com.oldpkg"), "renamedpkg")));

			assertThat(filterClasses(decompiler, "com.renamedpkg"))
					.containsExactlyInAnyOrder("com.renamedpkg.Alpha", "com.renamedpkg.Alpha.Inner", "com.renamedpkg.sub.Beta");
			assertThat(filterClasses(decompiler, "com.renamedpkg.sub"))
					.containsExactly("com.renamedpkg.sub.Beta");
			assertThat(preparePkgFilter(decompiler, "com.oldpkg")).isEqualTo(NLS.str("search_dialog.package_not_found"));
		}
	}

	@Test
	void testFullAliasPackageRenameAndSyntheticAncestors(@TempDir Path tempDir) throws Exception {
		try (JadxDecompiler decompiler = loadTestDecompiler(tempDir)) {
			// Full-alias rename: com.oldpkg -> org.custom.app
			applyRenames(decompiler, List.of(
					new JadxCodeRename(JadxNodeRef.forPkg("com.oldpkg"), "org.custom.app")));

			assertThat(filterClasses(decompiler, "org"))
					.containsExactlyInAnyOrder("org.custom.app.Alpha", "org.custom.app.Alpha.Inner", "org.custom.app.sub.Beta");
			assertThat(filterClasses(decompiler, "org.custom"))
					.containsExactlyInAnyOrder("org.custom.app.Alpha", "org.custom.app.Alpha.Inner", "org.custom.app.sub.Beta");
			assertThat(filterClasses(decompiler, "org.custom.app"))
					.containsExactlyInAnyOrder("org.custom.app.Alpha", "org.custom.app.Alpha.Inner", "org.custom.app.sub.Beta");
			assertThat(filterClasses(decompiler, "org.custom.app.sub"))
					.containsExactly("org.custom.app.sub.Beta");
			assertThat(preparePkgFilter(decompiler, "com")).isEqualTo(NLS.str("search_dialog.package_not_found"));
		}
	}

	@Test
	void testMultiplePackagesUnderSharedAliasPrefixAndSingleClassRename(@TempDir Path tempDir) throws Exception {
		try (JadxDecompiler decompiler = loadTestDecompiler(tempDir)) {
			applyRenames(decompiler, List.of(
					new JadxCodeRename(JadxNodeRef.forPkg("com.oldpkg"), "shared.one"),
					new JadxCodeRename(JadxNodeRef.forPkg("other.pkg"), "shared.two")));

			assertThat(filterClasses(decompiler, "shared"))
					.containsExactlyInAnyOrder(
							"shared.one.Alpha", "shared.one.Alpha.Inner", "shared.one.sub.Beta", "shared.two.Gamma");
			assertThat(filterClasses(decompiler, "shared.two"))
					.containsExactly("shared.two.Gamma");

			// Single-class full-name rename
			applyRenames(decompiler, List.of(
					new JadxCodeRename(JadxNodeRef.forPkg("com.oldpkg"), "shared.one"),
					new JadxCodeRename(JadxNodeRef.forCls("other.pkg.Gamma"), "moved.pkg.GammaRenamed")));

			assertThat(filterClasses(decompiler, "moved"))
					.containsExactly("moved.pkg.GammaRenamed");
			assertThat(filterClasses(decompiler, "moved.pkg"))
					.containsExactly("moved.pkg.GammaRenamed");
		}
	}

	@Test
	void testSyntheticParentJPackageFullNameInPackageHelper(@TempDir Path tempDir) throws Exception {
		try (JadxDecompiler decompiler = loadTestDecompiler(tempDir)) {
			applyRenames(decompiler, List.of(
					new JadxCodeRename(JadxNodeRef.forPkg("com.oldpkg"), "org.custom.app"),
					new JadxCodeRename(JadxNodeRef.forPkg("other.pkg"), "org.other")));

			MainWindow mainWindow = TestMainWindowShim.build();
			JadxWrapper wrapper = new JadxWrapper(mainWindow);
			Field decompilerField = JadxWrapper.class.getDeclaredField("decompiler");
			decompilerField.setAccessible(true);
			decompilerField.set(wrapper, decompiler);

			PackageHelper helper = new PackageHelper(wrapper, new JNodeCache(wrapper));
			List<JPackage> roots = helper.getRoots(false);
			JPackage orgRoot = roots.stream()
					.filter(p -> "org".equals(p.getName()))
					.findFirst()
					.orElseThrow();

			assertThat(orgRoot.isSynthetic()).isTrue();
			assertThat(orgRoot.getFullName()).isEqualTo("org");
			assertThat(orgRoot.makeLongString()).isEqualTo("org");
		}
	}

	private static JadxDecompiler loadTestDecompiler(Path tempDir) throws Exception {
		writeSmali(tempDir, "Alpha.smali", ""
				+ ".class public Lcom/oldpkg/Alpha;\n"
				+ ".super Ljava/lang/Object;\n"
				+ ".method public constructor <init>()V\n"
				+ "    .registers 1\n"
				+ "    invoke-direct {p0}, Ljava/lang/Object;-><init>()V\n"
				+ "    return-void\n"
				+ ".end method\n");
		writeSmali(tempDir, "Alpha$Inner.smali", ""
				+ ".class public Lcom/oldpkg/Alpha$Inner;\n"
				+ ".super Ljava/lang/Object;\n"
				+ ".annotation system Ldalvik/annotation/InnerClass;\n"
				+ "    accessFlags = 0x9\n"
				+ "    name = \"Inner\"\n"
				+ ".end annotation\n"
				+ ".method public constructor <init>()V\n"
				+ "    .registers 1\n"
				+ "    invoke-direct {p0}, Ljava/lang/Object;-><init>()V\n"
				+ "    return-void\n"
				+ ".end method\n");
		writeSmali(tempDir, "Beta.smali", ""
				+ ".class public Lcom/oldpkg/sub/Beta;\n"
				+ ".super Ljava/lang/Object;\n"
				+ ".method public constructor <init>()V\n"
				+ "    .registers 1\n"
				+ "    invoke-direct {p0}, Ljava/lang/Object;-><init>()V\n"
				+ "    return-void\n"
				+ ".end method\n");
		writeSmali(tempDir, "Gamma.smali", ""
				+ ".class public Lother/pkg/Gamma;\n"
				+ ".super Ljava/lang/Object;\n"
				+ ".method public constructor <init>()V\n"
				+ "    .registers 1\n"
				+ "    invoke-direct {p0}, Ljava/lang/Object;-><init>()V\n"
				+ "    return-void\n"
				+ ".end method\n");

		JadxArgs args = new JadxArgs();
		args.setInputFiles(List.of(
				tempDir.resolve("Alpha.smali").toFile(),
				tempDir.resolve("Alpha$Inner.smali").toFile(),
				tempDir.resolve("Beta.smali").toFile(),
				tempDir.resolve("Gamma.smali").toFile()));
		JadxDecompiler decompiler = new JadxDecompiler(args);
		decompiler.load();
		return decompiler;
	}

	private static void writeSmali(Path dir, String fileName, String content) throws Exception {
		Files.writeString(dir.resolve(fileName), content);
	}

	private static void applyRenames(JadxDecompiler decompiler, List<JadxCodeRename> renames) {
		JadxCodeData codeData = new JadxCodeData();
		codeData.setRenames(List.copyOf(renames));
		decompiler.getArgs().setCodeData(codeData);
		decompiler.reloadCodeData();
	}

	private static String preparePkgFilter(JadxDecompiler decompiler, String pkgFilter) {
		SearchSettings settings = new SearchSettings("test");
		settings.setSearchPkgStr(pkgFilter);
		return settings.prepare(decompiler);
	}

	private static List<String> filterClasses(JadxDecompiler decompiler, String pkgFilter) {
		SearchSettings settings = new SearchSettings("test");
		settings.setSearchPkgStr(pkgFilter);
		assertThat(settings.prepare(decompiler)).isNull();
		return decompiler.getClassesWithInners().stream()
				.filter(settings::isInSearchPkg)
				.map(JavaClass::getFullName)
				.collect(Collectors.toList());
	}
}
