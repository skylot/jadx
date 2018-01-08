package jadx.samples;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class RunTests {

	public static void main(String[] args) {
		ClassLoader clsLoader = ClassLoader.getSystemClassLoader();

		List<String> clsList = getClasses(clsLoader, "jadx.samples");
		if (clsList.isEmpty()) {
			System.err.println("No tests found");
			System.exit(1);
		}

		int timeout = 2 * clsList.size();
		System.err.println("Set timeout to " + timeout + " seconds");
		new Timer().schedule(new TerminateTask(), timeout * 1000);

		Collections.sort(clsList);
		int passed = 0;
		for (String cls : clsList) {
			if (runTest(cls)) {
				passed++;
			}
		}
		int failed = clsList.size() - passed;
		System.err.println("---");
		System.err.println("Total " + clsList.size()
				+ ", Passed: " + passed
				+ ", Failed: " + failed);

		System.exit(failed);
	}

	private static boolean runTest(String clsName) {
		try {
			boolean pass = false;
			String msg = null;
			Throwable exc = null;

			Class<?> cls = Class.forName(clsName);
			if (cls.getSuperclass() == AbstractTest.class) {
				Method mth = cls.getMethod("testRun");
				try {
					AbstractTest test = (AbstractTest) cls.getConstructor().newInstance();
					pass = (Boolean) mth.invoke(test);
				} catch (InvocationTargetException e) {
					pass = false;
					exc = e.getCause();
				} catch (Throwable e) {
					pass = false;
					exc = e;
				}
			} else {
				msg = "not extends AbstractTest";
			}
			System.err.println(">> "
					+ (pass ? "PASS" : "FAIL") + "\t"
					+ clsName
					+ (msg == null ? "" : "\t - " + msg));
			if (exc != null) {
				exc.printStackTrace();
			}
			return pass;
		} catch (ClassNotFoundException e) {
			System.err.println("Class '" + clsName + "' not found");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private static class TerminateTask extends TimerTask {
		@Override
		public void run() {
			System.err.println("Test timed out");
			System.exit(1);
		}
	}

	private static class TestFilter implements FilenameFilter {
		@Override
		public boolean accept(File dir, String name) {
			return name.startsWith("Test") && name.endsWith(".class") && !name.contains("$");
		}
	}

	private static List<String> getClasses(ClassLoader clsLoader, String packageName) {
		List<String> clsList = new ArrayList<>();
		URL resource = clsLoader.getResource(packageName.replace('.', '/'));
		if (resource != null) {
			File path = new File(resource.getFile());
			if (path.exists() && path.isDirectory()) {
				System.out.println("Test classes path: " + path.getAbsolutePath());
				String[] files = path.list(new TestFilter());
				for (String file : files) {
					String clsName = packageName + '.' + file.replace(".class", "");
					clsList.add(clsName);
				}
			}
		}
		return clsList;
	}
}
