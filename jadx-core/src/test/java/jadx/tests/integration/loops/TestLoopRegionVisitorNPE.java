package jadx.tests.integration.loops;

import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestLoopRegionVisitorNPE extends IntegrationTest {

	public static class TestClsSyncForEach {
		public void test(List<String> list) {
			for (String s : list) {
				synchronized (this) {
					voidCall();
					process(s);
				}
			}
		}

		private void voidCall() {
		}

		private void process(String s) {
		}
	}

	public static class TestClsSyncWhile {
		public void test(List<String> list) {
			Iterator<String> it = list.iterator();
			while (it.hasNext()) {
				synchronized (this) {
					voidCall();
					String s = it.next();
					process(s);
				}
			}
		}

		private void voidCall() {
		}

		private void process(String s) {
		}
	}

	public static class TestClsLoopWithSyncAndTryCatch {
		public int test(int[] arr) {
			int sum = 0;
			for (int i = 0; i < arr.length; i++) {
				synchronized (this) {
					try {
						voidCall();
						sum += arr[i];
					} catch (Exception e) {
						// ignore
					}
				}
			}
			return sum;
		}

		private void voidCall() {
		}
	}

	public static class TestClsTryCatchInLoopHeader {
		public void test(List<String> list) {
			Iterator<String> it = list.iterator();
			while (true) {
				try {
					if (!it.hasNext()) {
						break;
					}
					String s = it.next();
					synchronized (this) {
						process(s);
					}
				} catch (Exception e) {
					// continue
				}
			}
		}

		private void process(String s) {
		}
	}

	public static class TestClsArrayForEachWithTryCatch {
		public void test(String[] arr) {
			for (String s : arr) {
				try {
					synchronized (this) {
						voidCall();
						process(s);
					}
				} catch (Exception e) {
					// ignore
				}
			}
		}

		private void voidCall() {
		}

		private void process(String s) {
		}
	}

	public static class TestClsIndexedLoopWithSync {
		public void test(String[] items) {
			int i = 0;
			while (i < items.length) {
				synchronized (this) {
					voidCall();
					String item = items[i];
					process(item);
					i++;
				}
			}
		}

		private void voidCall() {
		}

		private void process(String s) {
		}
	}

	@Test
	public void testSyncForEach() {
		assertThat(getClassNode(TestClsSyncForEach.class))
				.code()
				.containsOne("for (String s : list) {")
				.containsOne("synchronized (this) {");
	}

	@Test
	public void testSyncWhile() {
		assertThat(getClassNode(TestClsSyncWhile.class))
				.code()
				.containsOne("synchronized (this) {");
	}

	@Test
	public void testLoopWithSyncAndTryCatch() {
		assertThat(getClassNode(TestClsLoopWithSyncAndTryCatch.class))
				.code()
				.containsOne("synchronized (this) {")
				.containsOne("try {");
	}

	@Test
	public void testTryCatchInLoopHeader() {
		assertThat(getClassNode(TestClsTryCatchInLoopHeader.class))
				.code()
				.contains("synchronized (this) {")
				.contains("try {");
	}

	@Test
	public void testArrayForEachWithTryCatch() {
		assertThat(getClassNode(TestClsArrayForEachWithTryCatch.class))
				.code()
				.containsOne("for (String s : arr) {")
				.containsOne("synchronized (this) {");
	}

	@Test
	public void testIndexedLoopWithSync() {
		assertThat(getClassNode(TestClsIndexedLoopWithSync.class))
				.code()
				.containsOne("synchronized (this) {");
	}
}
