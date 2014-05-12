package jadx.api;

import jadx.core.Jadx;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.DepthTraversal;
import jadx.core.dex.visitors.IDexTreeVisitor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public abstract class InternalJadxTest extends TestUtils {

	protected boolean outputCFG = false;
	protected boolean deleteTmpJar = true;

	protected String outDir = "test-out-tmp";

	public ClassNode getClassNode(Class<?> clazz) {
		try {
			File temp = getJarForClass(clazz);
			Decompiler d = new Decompiler();
			try {
				d.loadFile(temp);
			} catch (Exception e) {
				fail(e.getMessage());
			}
			List<ClassNode> classes = d.getRoot().getClasses(false);
			String clsName = clazz.getName();
			ClassNode cls = null;
			for (ClassNode aClass : classes) {
				if (aClass.getFullName().equals(clsName)) {
					cls = aClass;
				}
			}
			assertNotNull("Class not found: " + clsName, cls);

			assertEquals(cls.getFullName(), clazz.getName());

			cls.load();
			List<IDexTreeVisitor> passes = Jadx.getPassesList(new DefaultJadxArgs() {
				@Override
				public boolean isCFGOutput() {
					return outputCFG;
				}

				@Override
				public boolean isRawCFGOutput() {
					return outputCFG;
				}
			}, new File(outDir));
			for (IDexTreeVisitor visitor : passes) {
				DepthTraversal.visit(visitor, cls);
			}
			assertThat(cls.getCode().toString(), not(containsString("inconsistent")));
			return cls;
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
			return null;
		}
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
			add(file, path + "/" + file.getName(), jo);
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

	private void add(File source, String entryName, JarOutputStream target) throws IOException {
		BufferedInputStream in = null;
		try {
			JarEntry entry = new JarEntry(entryName);
			entry.setTime(source.lastModified());
			target.putNextEntry(entry);
			in = new BufferedInputStream(new FileInputStream(source));

			byte[] buffer = new byte[1024];
			while (true) {
				int count = in.read(buffer);
				if (count == -1) {
					break;
				}
				target.write(buffer, 0, count);
			}
			target.closeEntry();
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	// Use only for debug purpose
	@Deprecated
	protected void setOutputCFG() {
		this.outputCFG = true;
	}

	// Use only for debug purpose
	@Deprecated
	protected void notDeleteTmpJar() {
		this.deleteTmpJar = false;
	}
}
