package jadx.tests.integration.types;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTypeResolver13 extends IntegrationTest {

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	public static class TestCls {
		private static final Set<?> CONST = new HashSet<>();
		private Map<Set<?>, List<?>> map = new HashMap<>();

		@SuppressWarnings("unchecked")
		public <T> List<T> test(Set<T> type) {
			List<?> obj = this.map.get(type == null ? CONST : type);
			if (obj != null) {
				return (List<T>) obj;
			}
			return null;
		}
	}

	@NotYetImplemented("additional cast for generic types")
	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("public <T> List<T> test(Set<T> type) {")
				.containsOne("return (List<T>) obj;");
	}
}
