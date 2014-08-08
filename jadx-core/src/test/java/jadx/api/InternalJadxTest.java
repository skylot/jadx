package jadx.api;

import jadx.core.Jadx;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.DepthTraversal;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.utils.files.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarOutputStream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class InternalJadxTest extends TestUtils {

	protected boolean outputCFG = false;
	protected boolean isFallback = false;
	protected boolean deleteTmpJar = true;

	protected String outDir = "test-out-tmp";

	public ClassNode getClassNode(Class<?> clazz) {
		JadxDecompiler d = new JadxDecompiler();
		try {
			d.loadFile(getJarForClass(clazz));
		} catch (Exception e) {
			fail(e.getMessage());
		}
		String clsName = clazz.getName();
		ClassNode cls = d.getRoot().searchClassByName(clsName);
		assertNotNull("Class not found: " + clsName, cls);
		assertEquals(cls.getFullName(), clazz.getName());

		cls.load();
		for (IDexTreeVisitor visitor : getPasses()) {
			DepthTraversal.visit(visitor, cls);
		}
		// don't unload class

		checkCode(cls);
		return cls;
	}

	private static void checkCode(ClassNode cls) {
		assertTrue("Inconsistent cls: " + cls,
				!cls.contains(AFlag.INCONSISTENT_CODE) && !cls.contains(AType.JADX_ERROR));
		for (MethodNode mthNode : cls.getMethods()) {
			assertTrue("Inconsistent method: " + mthNode,
					!mthNode.contains(AFlag.INCONSISTENT_CODE) && !mthNode.contains(AType.JADX_ERROR));
		}
		assertThat(cls.getCode().toString(), not(containsString("inconsistent")));
	}

	protected List<IDexTreeVisitor> getPasses() {
		return Jadx.getPassesList(new DefaultJadxArgs() {
			@Override
			public boolean isCFGOutput() {
				return outputCFG;
			}

			@Override
			public boolean isRawCFGOutput() {
				return outputCFG;
			}

			@Override
			public boolean isFallbackMode() {
				return isFallback;
			}
		}, new File(outDir));
	}

	protected MethodNode getMethod(ClassNode cls, String method) {
		for (MethodNode mth : cls.getMethods()) {
			if (mth.getName().equals(method)) {
				return mth;
			}
		}
		fail("Method not found " + method + " in class " + cls);
		return null;
	}

	public File getJarForClass(Class<?> cls) throws IOException {
		String path = cls.getPackage().getName().replace('.', '/');
		List<File> list = getClassFilesWithInners(cls);

		File temp = File.createTempFile("jadx-tmp-", System.nanoTime() + ".jar");
		JarOutputStream jo = new JarOutputStream(new FileOutputStream(temp));
		for (File file : list) {
			FileUtils.addFileToJar(jo, file, path + "/" + file.getName());
		}
		jo.close();
		if (deleteTmpJar) {
			temp.deleteOnExit();
		} else {
			System.out.println("Temporary jar file path: " + temp.getAbsolutePath());
		}
		return temp;
	}

	private List<File> getClassFilesWithInners(Class<?> cls) {
		List<File> list = new ArrayList<File>();
		String pkgName = cls.getPackage().getName();
		URL pkgResource = ClassLoader.getSystemClassLoader().getResource(pkgName.replace('.', '/'));
		if (pkgResource != null) {
			try {
				String clsName = cls.getName();
				File directory = new File(pkgResource.toURI());
				String[] files = directory.list();
				for (String file : files) {
					String fullName = pkgName + "." + file;
					if (fullName.startsWith(clsName)) {
						list.add(new File(directory, file));
					}
				}
			} catch (URISyntaxException e) {
				fail(e.getMessage());
			}
		}
		return list;
	}


	// Use only for debug purpose
	@Deprecated
	protected void setOutputCFG() {
		this.outputCFG = true;
	}

	// Use only for debug purpose
	@Deprecated
	protected void setFallback() {
		this.isFallback = true;
	}

	// Use only for debug purpose
	@Deprecated
	protected void notDeleteTmpJar() {
		this.deleteTmpJar = false;
	}
}
