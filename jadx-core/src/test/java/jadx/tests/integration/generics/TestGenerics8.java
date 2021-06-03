package jadx.tests.integration.generics;

import java.util.Iterator;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestGenerics8 extends IntegrationTest {

	@SuppressWarnings("IllegalType")
	public static class TestCls<I> extends LinkedHashMap<I, Integer> implements Iterable<I> {
		@Override
		public Iterator<I> iterator() {
			return keySet().iterator();
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return keySet().iterator();");
	}
}
