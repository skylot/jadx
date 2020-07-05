package jadx.tests.integration.others;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestIssue13a extends IntegrationTest {

	public static class TestCls {
		private static final String TAG = "Parcel";
		private static final Map<ClassLoader, Map<String, Parcelable.Creator<?>>> M_CREATORS = new HashMap<>();

		@SuppressWarnings({ "unchecked", "ConstantConditions", "Java8MapApi", "rawtypes" })
		public final <T extends Parcelable> T test(ClassLoader loader) {
			String name = readString();
			if (name == null) {
				return null;
			}
			Parcelable.Creator<T> creator;
			synchronized (M_CREATORS) {
				Map<String, Parcelable.Creator<?>> map = M_CREATORS.get(loader);
				if (map == null) {
					map = new HashMap<>();
					M_CREATORS.put(loader, map);
				}
				creator = (Parcelable.Creator<T>) map.get(name);
				if (creator == null) {
					try {
						Class<?> c = loader == null ? Class.forName(name) : Class.forName(name, true, loader);
						Field f = c.getField("CREATOR");
						creator = (Parcelable.Creator) f.get(null);
					} catch (IllegalAccessException e) {
						Log.e(TAG, '1' + name + ", e: " + e);
						throw new RuntimeException('2' + name);
					} catch (ClassNotFoundException e) {
						Log.e(TAG, '3' + name + ", e: " + e);
						throw new RuntimeException('4' + name);
					} catch (ClassCastException e) {
						throw new RuntimeException('5' + name);
					} catch (NoSuchFieldException e) {
						throw new RuntimeException('6' + name);
					}
					if (creator == null) {
						throw new RuntimeException('7' + name);
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
		disableCompilation();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		for (int i = 1; i <= 7; i++) {
			assertThat(code, containsOne("'" + i + '\''));
		}

		// TODO: add additional checks
		assertThat(code, not(containsString("Throwable")));
	}
}
