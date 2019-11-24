package jadx.tests.api.utils.assertj;

import org.assertj.core.api.Assertions;

import jadx.api.ICodeInfo;
import jadx.core.dex.nodes.ClassNode;

public class JadxAssertions extends Assertions {

	public static JadxClassNodeAssertions assertThat(ClassNode actual) {
		return new JadxClassNodeAssertions(actual);
	}

	public static JadxCodeAssertions assertThat(ICodeInfo actual) {
		return new JadxCodeAssertions(actual.getCodeStr());
	}
}
