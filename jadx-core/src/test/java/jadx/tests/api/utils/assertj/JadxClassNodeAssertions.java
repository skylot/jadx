package jadx.tests.api.utils.assertj;

import org.assertj.core.api.AbstractAssert;

import jadx.api.ICodeInfo;
import jadx.core.dex.nodes.ClassNode;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class JadxClassNodeAssertions extends AbstractAssert<JadxClassNodeAssertions, ClassNode> {
	public JadxClassNodeAssertions(ClassNode cls) {
		super(cls, JadxClassNodeAssertions.class);
	}

	public JadxCodeAssertions code() {
		isNotNull();
		ICodeInfo code = actual.getCode();
		assertThat(code).isNotNull();
		String codeStr = code.getCodeStr();
		assertThat(codeStr).isNotBlank();
		return new JadxCodeAssertions(codeStr);
	}
}
