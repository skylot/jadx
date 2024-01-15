package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.RaungTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestJavaJSR extends RaungTest {

	@Test
	public void test() {
		assertThat(getClassNodeFromRaung())
				.code()
				.containsLines(2,
						"InputStream in = url.openStream();",
						"try {",
						indent() + "return call(in);",
						"} finally {",
						indent() + "in.close();",
						"}");
	}
}
