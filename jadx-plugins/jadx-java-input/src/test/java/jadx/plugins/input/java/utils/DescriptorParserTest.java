package jadx.plugins.input.java.utils;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import jadx.plugins.input.java.data.JavaMethodRef;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class DescriptorParserTest {

	@Test
	public void testPrimitives() {
		check("()V", "V");
		check("(I)D", "D", "I");
	}

	@Test
	public void testObjects() {
		check("(Ljava/lang/String;Ljava/lang/Object;)V", "V", "Ljava/lang/String;", "Ljava/lang/Object;");
	}

	@SuppressWarnings("CatchMayIgnoreException")
	private void check(String desc, String retType, String... argTypes) {
		JavaMethodRef mthRef = new JavaMethodRef();
		try {
			DescriptorParser.fillMethodProto(desc, mthRef);
		} catch (Exception e) {
			fail("Parse failed for: " + desc, e);
		}

		assertThat(mthRef.getReturnType()).isEqualTo(retType);
		assertThat(mthRef.getArgTypes()).isEqualTo(Arrays.asList(argTypes));
	}
}
