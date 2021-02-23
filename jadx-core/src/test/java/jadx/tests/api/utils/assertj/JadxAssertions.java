package jadx.tests.api.utils.assertj;

import org.assertj.core.api.Assertions;

import jadx.api.ICodeInfo;
import jadx.core.dex.nodes.ClassNode;

public class JadxAssertions extends Assertions {

	public static JadxClassNodeAssertions assertThat(ClassNode cls) {
		Assertions.assertThat(cls).isNotNull();
		return new JadxClassNodeAssertions(cls);
	}

	public static JadxCodeInfoAssertions assertThat(ICodeInfo codeInfo) {
		Assertions.assertThat(codeInfo).isNotNull();
		return new JadxCodeInfoAssertions(codeInfo);
	}

	public static JadxCodeAssertions assertThat(String code) {
		Assertions.assertThat(code).isNotNull();
		return new JadxCodeAssertions(code);
	}
}
