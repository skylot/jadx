package jadx.gui.treemodel;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import jadx.api.Factory;
import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaPackage;
import jadx.core.dex.nodes.ClassNode;
import jadx.gui.JadxWrapper;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
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
		decompiler = new JadxDecompiler(new JadxArgs());
	}

	@Test
	public void testHierarchyPackages() {
		String pkgName = "a.b.c.d.e";

		List<JavaPackage> packages = Collections.singletonList(newPkg(pkgName));
		List<JPackage> out = sources.getHierarchyPackages(packages);

		assertThat(out, hasSize(1));
		JPackage jPkg = out.get(0);
		assertThat(jPkg.getName(), is(pkgName));
		assertThat(jPkg.getClasses(), hasSize(1));
	}

	@Test
	public void testHierarchyPackages2() {
		List<JavaPackage> packages = asList(
				newPkg("a.b"),
				newPkg("a.c"),
				newPkg("a.d")
		);
		List<JPackage> out = sources.getHierarchyPackages(packages);

		assertThat(out, hasSize(1));
		JPackage jPkg = out.get(0);
		assertThat(jPkg.getName(), is("a"));
		assertThat(jPkg.getClasses(), hasSize(0));
		assertThat(jPkg.getInnerPackages(), hasSize(3));
	}

	@Test
	public void testHierarchyPackages3() {
		List<JavaPackage> packages = asList(
				newPkg("a.b.p1"),
				newPkg("a.b.p2"),
				newPkg("a.b.p3")
		);
		List<JPackage> out = sources.getHierarchyPackages(packages);

		assertThat(out, hasSize(1));
		JPackage jPkg = out.get(0);
		assertThat(jPkg.getName(), is("a.b"));
		assertThat(jPkg.getClasses(), hasSize(0));
		assertThat(jPkg.getInnerPackages(), hasSize(3));
	}

	@Test
	public void testHierarchyPackages4() {
		List<JavaPackage> packages = asList(
				newPkg("a.p1"),
				newPkg("a.b.c.p2"),
				newPkg("a.b.c.p3"),
				newPkg("d.e"),
				newPkg("d.f.a")
		);
		List<JPackage> out = sources.getHierarchyPackages(packages);

		assertThat(out, hasSize(2));
		assertThat(out.get(0).getName(), is("a"));
		assertThat(out.get(0).getInnerPackages(), hasSize(2));
		assertThat(out.get(1).getName(), is("d"));
		assertThat(out.get(1).getInnerPackages(), hasSize(2));
	}

	private JavaPackage newPkg(String name) {
		return Factory.newPackage(name, Collections.singletonList(newClass()));
	}

	private JavaClass newClass() {
		return Factory.newClass(decompiler, mock(ClassNode.class));
	}
}
