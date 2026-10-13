package jadx.gui.utils.pkgs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.data.ICodeRename;
import jadx.api.data.impl.JadxCodeData;
import jadx.api.data.impl.JadxCodeRename;
import jadx.api.data.impl.JadxNodeRef;
import jadx.gui.JadxWrapper;
import jadx.gui.plugins.context.TestMainWindowShim;
import jadx.gui.treemodel.JPackage;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.JNodeCache;

import static org.assertj.core.api.Assertions.assertThat;

class PackageHelperTest {

	@TempDir
	Path tmpDir;

	private final List<ICodeRename> renames = new ArrayList<>();
	private JadxDecompiler decompiler;
	private JadxWrapper wrapper;

	@AfterEach
	void close() {
		if (decompiler != null) {
			decompiler.close();
		}
	}

	@Test
	void classMovedToRenamedPackage() throws IOException {
		renamePkg("a.b", "com.foo");
		renameCls("x.Mover", "com.foo.Mover");
		load("a.b.First", "a.b.sub.Second", "x.Mover", "x.Other");

		JPackage pkg = findPkg(getRoots(false), "com.foo");
		assertThat(classes(pkg)).containsExactly("a.b.First", "x.Mover");
		assertThat(pkg.getSubPackages()).extracting(JPackage::getName).containsExactly("sub");

		List<JPackage> flatRoots = getRoots(true);
		assertThat(flatRoots).extracting(JPackage::getName).containsExactly("com.foo", "com.foo.sub", "x");
		assertThat(classes(findPkg(flatRoots, "com.foo"))).containsExactly("a.b.First", "x.Mover");
	}

	@Test
	void classMovedToRenamedPackageWithoutClasses() throws IOException {
		renamePkg("a.b", "com.foo");
		renameCls("x.Mover", "com.foo.Mover");
		load("a.b.sub.Second", "x.Mover", "x.Other");

		JPackage pkg = findPkg(getRoots(false), "com.foo");
		assertThat(classes(pkg)).containsExactly("x.Mover");
		assertThat(pkg.getSubPackages()).extracting(JPackage::getName).containsExactly("sub");
	}

	private List<JPackage> getRoots(boolean flat) {
		return new PackageHelper(wrapper, new JNodeCache(wrapper)).getRoots(flat);
	}

	private static JPackage findPkg(List<JPackage> packages, String name) {
		List<JPackage> found = packages.stream()
				.filter(p -> p.getName().equals(name))
				.collect(Collectors.toList());
		assertThat(found).hasSize(1);
		return found.get(0);
	}

	private static List<String> classes(JPackage pkg) {
		return pkg.getClasses().stream()
				.map(c -> c.getCls().getRawName())
				.collect(Collectors.toList());
	}

	private void renamePkg(String pkg, String newName) {
		renames.add(new JadxCodeRename(JadxNodeRef.forPkg(pkg), newName));
	}

	private void renameCls(String cls, String newName) {
		renames.add(new JadxCodeRename(JadxNodeRef.forCls(cls), newName));
	}

	private void load(String... classes) throws IOException {
		JadxArgs args = new JadxArgs();
		args.setFsCaseSensitive(true); // use the same values on all systems
		for (String cls : classes) {
			args.getInputFiles().add(writeSmali(cls));
		}
		JadxCodeData codeData = new JadxCodeData();
		codeData.setRenames(renames);
		args.setCodeData(codeData);
		decompiler = new JadxDecompiler(args);
		decompiler.load();
		MainWindow mainWindow = TestMainWindowShim.build();
		TestMainWindowShim.setDecompiler(mainWindow, decompiler);
		wrapper = mainWindow.getWrapper();
	}

	private File writeSmali(String cls) throws IOException {
		String smali = ".class public L" + cls.replace('.', '/') + ";\n"
				+ ".super Ljava/lang/Object;\n";
		Path file = tmpDir.resolve(cls + ".smali");
		Files.writeString(file, smali);
		return file.toFile();
	}
}
