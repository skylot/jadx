package jadx.gui.search;

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
import jadx.api.JavaClass;
import jadx.api.data.ICodeRename;
import jadx.api.data.impl.JadxCodeData;
import jadx.api.data.impl.JadxCodeRename;
import jadx.api.data.impl.JadxNodeRef;
import jadx.gui.plugins.context.TestMainWindowShim;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;

import static org.assertj.core.api.Assertions.assertThat;

class SearchPackageTest {

	@TempDir
	Path tmpDir;

	private final List<ICodeRename> renames = new ArrayList<>();
	private JadxDecompiler decompiler;
	private MainWindow mainWindow;

	@AfterEach
	void close() {
		if (decompiler != null) {
			decompiler.close();
		}
	}

	@Test
	void renamedPackage() throws IOException {
		renamePkg("a.b", "com.foo");
		load("a.b.First", "a.b.First$Inner", "a.b.sub.Second", "x.Other");

		assertThat(search("com.foo")).containsExactlyInAnyOrder("a.b.First", "a.b.First$Inner", "a.b.sub.Second");
		assertThat(search("com.foo.sub")).containsExactly("a.b.sub.Second");
		assertThat(search("com")).containsExactlyInAnyOrder("a.b.First", "a.b.First$Inner", "a.b.sub.Second");
		assertNotFound("a.b");
		assertNotFound("com.fo");
	}

	@Test
	void renamedLeafPackage() throws IOException {
		renamePkg("p.q", "qq");
		load("p.q.Left", "p.r.Right");

		assertThat(search("p.qq")).containsExactly("p.q.Left");
		assertThat(search("p")).containsExactlyInAnyOrder("p.q.Left", "p.r.Right");
		assertNotFound("p.q");
	}

	@Test
	void classMovedToRenamedPackage() throws IOException {
		renamePkg("a.b", "com.foo");
		renameCls("x.Mover", "com.foo.Mover");
		load("a.b.First", "x.Mover", "x.Other");

		assertThat(search("com.foo")).containsExactlyInAnyOrder("a.b.First", "x.Mover");
		assertThat(search("x")).containsExactly("x.Other");
	}

	@Test
	void defaultPackage() throws IOException {
		load("Root", "a.Other");

		assertThat(search("defpackage")).containsExactly("Root");
	}

	private List<String> search(String pkg) {
		SearchSettings settings = buildSettings(pkg);
		assertThat(settings.prepare(mainWindow)).isNull();
		return decompiler.getClassesWithInners().stream()
				.filter(settings::isInSearchPkg)
				.map(JavaClass::getRawName)
				.collect(Collectors.toList());
	}

	private void assertNotFound(String pkg) {
		SearchSettings settings = buildSettings(pkg);
		assertThat(settings.prepare(mainWindow)).isEqualTo(NLS.str("search_dialog.package_not_found"));
	}

	private static SearchSettings buildSettings(String pkg) {
		SearchSettings settings = new SearchSettings("test");
		settings.setSearchPkgStr(pkg);
		settings.setResFilterStr("");
		return settings;
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
		decompiler.getPackages(); // init package nodes, as the packages tree in jadx-gui does
		mainWindow = TestMainWindowShim.build();
		TestMainWindowShim.setDecompiler(mainWindow, decompiler);
	}

	private File writeSmali(String cls) throws IOException {
		StringBuilder smali = new StringBuilder();
		smali.append(".class public ").append(toType(cls)).append('\n');
		smali.append(".super Ljava/lang/Object;\n");
		int innerSep = cls.indexOf('$');
		if (innerSep != -1) {
			smali.append(".annotation system Ldalvik/annotation/EnclosingClass;\n");
			smali.append("    value = ").append(toType(cls.substring(0, innerSep))).append('\n');
			smali.append(".end annotation\n");
			smali.append(".annotation system Ldalvik/annotation/InnerClass;\n");
			smali.append("    accessFlags = 0x9\n");
			smali.append("    name = \"").append(cls.substring(innerSep + 1)).append("\"\n");
			smali.append(".end annotation\n");
		}
		Path file = tmpDir.resolve(cls + ".smali");
		Files.writeString(file, smali.toString());
		return file.toFile();
	}

	private static String toType(String cls) {
		return 'L' + cls.replace('.', '/') + ';';
	}
}
