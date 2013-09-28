package jadx.api;

import jadx.core.Jadx;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.DepthTraverser;
import jadx.core.dex.visitors.IDexTreeVisitor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

public abstract class InternalJadxTest {

	protected boolean outputCFG = false;
	protected String outDir = "test-out-tmp";

	public ClassNode getClassNode(Class<?> clazz) {
		try {
			File temp = getJarForClass(clazz);
			Decompiler d = new Decompiler();
			try {
				d.loadFile(temp);
				assertEquals(d.getClasses().size(), 1);
			} catch (Exception e) {
				fail(e.getMessage());
			} finally {
				temp.delete();
			}
			List<ClassNode> classes = d.getRoot().getClasses(false);
			ClassNode cls = classes.get(0);

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
				DepthTraverser.visit(visitor, cls);
			}
			return cls;
		} catch (Exception e) {
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
		File classFile = getClassFile(cls);
		String shortClsFileName = cls.getName().replace('.', '/') + ".class";

		File temp = File.createTempFile("jadx-tmp-", System.nanoTime() + ".jar");
		JarOutputStream jo = new JarOutputStream(new FileOutputStream(temp));
		add(classFile, shortClsFileName, jo);
		jo.close();
		temp.deleteOnExit();
		return temp;
	}

	private File getClassFile(Class<?> cls) {
		String path = cutPackage(cls) + ".class";
		URL resource = cls.getResource(path);
		if (resource == null) {
			fail("Class file not found: " + path);
		}
		if (!"file".equalsIgnoreCase(resource.getProtocol())) {
			fail("Class is not stored in a file.");
		}
		return new File(resource.getPath());
	}

	private String cutPackage(Class<?> cls) {
		String longName = cls.getName();
		String pkg = cls.getPackage().getName();
		return longName.substring(pkg.length() + 1, longName.length());
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
				if (count == -1)
					break;
				target.write(buffer, 0, count);
			}
			target.closeEntry();
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}
}
