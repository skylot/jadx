package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTryWithEmptyCatchTriple extends SmaliTest {
	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				// all catches are empty
				.containsLines(2, "} catch (Error unused) {", "}")
				.containsLines(2, "} catch (Error unused2) {", "}")
				.containsLines(2, "} catch (Error unused3) {", "}");
	}
}
