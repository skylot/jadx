package jadx.tests.integration.types;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTypeResolver18 extends IntegrationTest {

	public static class TestCls<T> {
		private final AtomicReference<T> reference = new AtomicReference<>();

		public void test() {
			T t = this.reference.get();
			if (t instanceof Closeable) {
				try {
					((Closeable) t).close();
				} catch (IOException unused) {
					// ignore
				}
			}
			this.reference.set(null);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("((Closeable) t).close();");
	}
}
