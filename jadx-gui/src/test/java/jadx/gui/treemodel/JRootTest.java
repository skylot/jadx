package jadx.gui.treemodel;

import jadx.api.Decompiler;
import jadx.api.Factory;
import jadx.api.IJadxArgs;
import jadx.api.JavaClass;
import jadx.api.JavaPackage;
import jadx.core.dex.nodes.ClassNode;
import jadx.gui.JadxWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class JRootTest {

	@Test
	public void testHierarchyPackages() {
		String pkgName = "a.b.c.d.e";

		JadxWrapper wrapper = mock(JadxWrapper.class);
		JRoot root = new JRoot(wrapper);

		JavaClass cls = Factory.newClass(new Decompiler(mock(IJadxArgs.class)), mock(ClassNode.class));
		JavaPackage pkg = Factory.newPackage(pkgName, Arrays.asList(cls));

		List<JavaPackage> packages = new ArrayList<JavaPackage>();
		packages.add(pkg);

		List<JPackage> out = root.getHierarchyPackages(packages);

		assertEquals(out.size(), 1);
		JPackage jpkg = out.get(0);
		assertEquals(jpkg.getName(), pkgName);
		assertEquals(jpkg.getClasses().size(), 1);
	}

}
