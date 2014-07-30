package jadx.tests.internal.others;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import java.lang.reflect.Field;
import java.util.HashMap;

import org.junit.Test;

import static jadx.tests.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestIssue13 extends InternalJadxTest {

	public static class TestCls {
		private static final String TAG = "Parcel";
		private static HashMap<ClassLoader, HashMap<String, Parcelable.Creator>> mCreators;

		public final <T extends Parcelable> T test(ClassLoader loader) {
			String name = readString();
			if (name == null) {
				return null;
			}
			Parcelable.Creator<T> creator;
			synchronized (mCreators) {
				HashMap<String, Parcelable.Creator> map = mCreators.get(loader);
				if (map == null) {
					map = new HashMap<String, Parcelable.Creator>();
					mCreators.put(loader, map);
				}
				creator = map.get(name);
				if (creator == null) {
					try {
						Class c = loader == null ?
								Class.forName(name) : Class.forName(name, true, loader);
						Field f = c.getField("CREATOR");
						creator = (Parcelable.Creator) f.get(null);
					} catch (IllegalAccessException e) {
						Log.e(TAG, "1" + name + ", e: " + e);
						throw new RuntimeException("2" + name);
					} catch (ClassNotFoundException e) {
						Log.e(TAG, "3" + name + ", e: " + e);
						throw new RuntimeException("4" + name);
					} catch (ClassCastException e) {
						throw new RuntimeException("5" + name);
					} catch (NoSuchFieldException e) {
						throw new RuntimeException("6" + name);
					}
					if (creator == null) {
						throw new RuntimeException("7" + name);
					}
					map.put(name, creator);
				}
			}
			if (creator instanceof Parcelable.ClassLoaderCreator<?>) {
				return ((Parcelable.ClassLoaderCreator<T>) creator).createFromParcel(this, loader);
			}
			return creator.createFromParcel(this);
		}

		private String readString() {
			return "";
		}

		private class Parcelable {
			public class Creator<T> {
				public T createFromParcel(TestCls testCls) {
					return null;
				}
			}

			public class ClassLoaderCreator<T> extends Creator<T> {
				public T createFromParcel(TestCls testCls, ClassLoader loader) {
					return null;
				}
			}
		}

		private static class Log {
			public static void e(String tag, String s) {

			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		for (int i = 1; i <= 7; i++) {
			assertThat(code, containsOne("\"" + i + "\""));
		}

		// TODO: add additional checks
//		assertThat(code, not(containsString("Throwable")));
	}
}
