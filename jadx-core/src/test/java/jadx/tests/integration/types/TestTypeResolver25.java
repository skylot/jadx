package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTypeResolver25 extends SmaliTest {

	@Test
	public void testSmali() {
		// TODO: type inference error not yet resolved
		// Check that no stack overflow in type inference for now
		allowWarnInCode();
		disableCompilation();
		assertThat(getClassNodeFromSmali())
				.code()
				.oneOf(c -> c.containsOne("t = obj;"),
						c -> c.containsOne("t = (T) obj;"));
	}
}
