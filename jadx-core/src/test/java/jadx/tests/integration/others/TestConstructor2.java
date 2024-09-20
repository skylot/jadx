package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Constructor called on object instance is from Object not instance type
 */
public class TestConstructor2 extends SmaliTest {

	@Test
	public void test() {
		assertThat(getClassNodeFromSmaliFiles())
				.code()
				.containsOne("A a = new A();")
				.doesNotContain("return");
	}
}
