package jadx.tests.api.utils.assertj;

import org.assertj.core.api.Assertions;

import jadx.api.ICodeInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;

public class JadxAssertions extends Assertions {

	public static JadxClassNodeAssertions assertThat(ClassNode cls) {
		Assertions.assertThat(cls).isNotNull();
		return new JadxClassNodeAssertions(cls);
	}

	public static JadxMethodNodeAssertions assertThat(MethodNode mth) {
		Assertions.assertThat(mth).isNotNull();
		return new JadxMethodNodeAssertions(mth);
	}

	public static JadxCodeInfoAssertions assertThat(ICodeInfo codeInfo) {
		Assertions.assertThat(codeInfo).isNotNull();
		return new JadxCodeInfoAssertions(codeInfo);
	}

	public static JadxCodeAssertions assertThat(String code) {
		return new JadxCodeAssertions(code);
	}
}
