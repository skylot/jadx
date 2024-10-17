package jadx.tests.api.utils.assertj;

import org.assertj.core.api.AbstractObjectAssert;

import jadx.core.dex.nodes.MethodNode;

import static org.assertj.core.api.Assertions.assertThat;

public class JadxMethodNodeAssertions extends AbstractObjectAssert<JadxMethodNodeAssertions, MethodNode> {
	public JadxMethodNodeAssertions(MethodNode mth) {
		super(mth, JadxMethodNodeAssertions.class);
	}

	public JadxCodeAssertions code() {
		isNotNull();
		String codeStr = actual.getCodeStr();
		assertThat(codeStr).isNotBlank();
		return new JadxCodeAssertions(codeStr);
	}
}
