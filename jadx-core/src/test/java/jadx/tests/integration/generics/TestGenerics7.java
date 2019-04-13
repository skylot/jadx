package jadx.tests.integration.generics;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;

public class TestGenerics7 extends IntegrationTest {

	public static class TestCls {

		public void test() {
			declare(String.class);
		}

		public <T> T declare(Class<T> cls) {
	    	return null;
	    }

		public void declare(Object cls) {
	    }
	}

	@Test
	@NotYetImplemented
	public void test() {
		getClassNode(TestCls.class);
	}
}
