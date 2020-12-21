package jadx.tests.api.utils.assertj;

import org.assertj.core.api.AbstractObjectAssert;

import jadx.api.ICodeInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class JadxClassNodeAssertions extends AbstractObjectAssert<JadxClassNodeAssertions, ClassNode> {
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

	public JadxCodeAssertions reloadCode(IntegrationTest testInstance) {
		isNotNull();
		ICodeInfo code = actual.reloadCode();
		assertThat(code).isNotNull();
		String codeStr = code.getCodeStr();
		assertThat(codeStr).isNotBlank();

		JadxCodeAssertions codeAssertions = new JadxCodeAssertions(codeStr);
		codeAssertions.print();
		testInstance.runChecks(actual);
		return codeAssertions;
	}
}
