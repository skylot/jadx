package jadx.tests.integration.invoke;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestCastInOverloadedInvoke extends IntegrationTest {

	public static class TestCls {

		public void test() {
			call(new ArrayList<>());
			call((List<String>) new ArrayList<String>());
		}

		public void test2(Object obj) {
			if (obj instanceof String) {
				call((String) obj);
			}
		}

		public void call(String str) {
		}

		public void call(List<String> list) {
		}

		public void call(ArrayList<String> list) {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

// 		TODO: implement more checks for casts in overloaded methods
//		assertThat(code, containsOne("call(new ArrayList<>());"));
		assertThat(code, containsOne("call((ArrayList<String>) new ArrayList());"));

// 		TODO: fix generics in constructors
//		assertThat(code, containsOne("call((List<String>) new ArrayList<String>());"));
		assertThat(code, containsOne("call((List<String>) new ArrayList());"));

		assertThat(code, containsOne("call((String) obj);"));
	}
}
