package jadx.gui.treemodel;

import jadx.api.Factory;
import jadx.api.IJadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaPackage;
import jadx.core.dex.nodes.ClassNode;
import jadx.gui.JadxWrapper;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JSourcesTest {

	private JSources sources;
	private JadxDecompiler decompiler;

	@Before
	public void init() {
		JRoot root = mock(JRoot.class);
		when(root.isFlatPackages()).thenReturn(false);
		JadxWrapper wrapper = mock(JadxWrapper.class);
		sources = new JSources(root, wrapper);
		decompiler = new JadxDecompiler(mock(IJadxArgs.class));
	}

	@Test
	public void testHierarchyPackages() {
		String pkgName = "a.b.c.d.e";

		List<JavaPackage> packages = Arrays.asList(newPkg(pkgName));
		List<JPackage> out = sources.getHierarchyPackages(packages);

		assertEquals(out.size(), 1);
		JPackage jpkg = out.get(0);
		assertEquals(jpkg.getName(), pkgName);
		assertEquals(jpkg.getClasses().size(), 1);
	}

	@Test
	public void testHierarchyPackages2() {
		List<JavaPackage> packages = Arrays.asList(
				newPkg("a.b"),
				newPkg("a.c"),
				newPkg("a.d")
		);
		List<JPackage> out = sources.getHierarchyPackages(packages);

		assertEquals(out.size(), 1);
		JPackage jpkg = out.get(0);
		assertEquals(jpkg.getName(), "a");
		assertEquals(jpkg.getClasses().size(), 0);
		assertEquals(jpkg.getInnerPackages().size(), 3);
	}

	@Test
	public void testHierarchyPackages3() {
		List<JavaPackage> packages = Arrays.asList(
				newPkg("a.b.p1"),
				newPkg("a.b.p2"),
				newPkg("a.b.p3")
		);
		List<JPackage> out = sources.getHierarchyPackages(packages);

		assertEquals(out.size(), 1);
		JPackage jpkg = out.get(0);
		assertEquals(jpkg.getName(), "a.b");
		assertEquals(jpkg.getClasses().size(), 0);
		assertEquals(jpkg.getInnerPackages().size(), 3);
	}

	@Test
	public void testHierarchyPackages4() {
		List<JavaPackage> packages = Arrays.asList(
				newPkg("a.p1"),
				newPkg("a.b.c.p2"),
				newPkg("a.b.c.p3"),
				newPkg("d.e"),
				newPkg("d.f.a")
		);
		List<JPackage> out = sources.getHierarchyPackages(packages);

		assertEquals(out.size(), 2);
		assertEquals(out.get(0).getName(), "a");
		assertEquals(out.get(0).getInnerPackages().size(), 2);
		assertEquals(out.get(1).getName(), "d");
		assertEquals(out.get(1).getInnerPackages().size(), 2);
	}

	private JavaPackage newPkg(String name) {
		return Factory.newPackage(name, Arrays.asList(newClass()));
	}

	private JavaClass newClass() {
		return Factory.newClass(decompiler, mock(ClassNode.class));
	}

}
