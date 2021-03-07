package jadx.tests.api.utils.assertj;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;

import jadx.api.ICodeInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class JadxClassNodeAssertions extends AbstractObjectAssert<JadxClassNodeAssertions, ClassNode> {
	public JadxClassNodeAssertions(ClassNode cls) {
		super(cls, JadxClassNodeAssertions.class);
	}

	public JadxCodeInfoAssertions decompile() {
		isNotNull();
		ICodeInfo codeInfo = actual.getCode();
		Assertions.assertThat(codeInfo).isNotNull();
		return new JadxCodeInfoAssertions(codeInfo);
	}

	public JadxCodeAssertions code() {
		isNotNull();
		ICodeInfo code = actual.getCode();
		Assertions.assertThat(code).isNotNull();
		String codeStr = code.getCodeStr();
		assertThat(codeStr).isNotBlank();
		return new JadxCodeAssertions(codeStr);
	}

	public JadxCodeAssertions reloadCode(IntegrationTest testInstance) {
		isNotNull();
		ICodeInfo code = actual.reloadCode();
		Assertions.assertThat(code).isNotNull();
		String codeStr = code.getCodeStr();
		Assertions.assertThat(codeStr).isNotBlank();

		JadxCodeAssertions codeAssertions = new JadxCodeAssertions(codeStr);
		codeAssertions.print();
		testInstance.runChecks(actual);
		return codeAssertions;
	}
}
