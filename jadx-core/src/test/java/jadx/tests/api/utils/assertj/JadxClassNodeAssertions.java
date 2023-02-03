package jadx.tests.api.utils.assertj;

import java.util.Map;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;

import jadx.api.ICodeInfo;
import jadx.api.metadata.ICodeAnnotation;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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

	public JadxCodeAssertions disasmCode() {
		isNotNull();
		String disasmCode = actual.getDisassembledCode();
		assertThat(disasmCode).isNotNull().isNotBlank();
		return new JadxCodeAssertions(disasmCode);
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

	/**
	 * Force running auto check on decompiled code.
	 * Useful for smali tests.
	 */
	public JadxClassNodeAssertions runDecompiledAutoCheck(IntegrationTest testInstance) {
		isNotNull();
		testInstance.runDecompiledAutoCheck(actual);
		return this;
	}

	public JadxClassNodeAssertions checkCodeAnnotationFor(String refStr, ICodeAnnotation node) {
		checkCodeAnnotationFor(refStr, 0, node);
		return this;
	}

	public JadxClassNodeAssertions checkCodeAnnotationFor(String refStr, int refOffset, ICodeAnnotation node) {
		ICodeInfo code = actual.getCode();
		int codePos = code.getCodeStr().indexOf(refStr);
		assertThat(codePos).describedAs("String '%s' not found", refStr).isNotEqualTo(-1);
		int refPos = codePos + refOffset;
		for (Map.Entry<Integer, ICodeAnnotation> entry : code.getCodeMetadata().getAsMap().entrySet()) {
			if (entry.getKey() == refPos) {
				Assertions.assertThat(entry.getValue()).isEqualTo(node);
				return this;
			}
		}
		fail("Annotation for reference string: '%s' at position %d not found", refStr, refPos);
		return this;
	}
}
