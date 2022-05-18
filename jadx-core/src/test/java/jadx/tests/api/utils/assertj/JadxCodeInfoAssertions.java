package jadx.tests.api.utils.assertj;

import java.util.stream.Collectors;

import org.assertj.core.api.AbstractObjectAssert;

import jadx.api.ICodeInfo;
import jadx.api.metadata.annotations.InsnCodeOffset;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class JadxCodeInfoAssertions extends AbstractObjectAssert<JadxCodeInfoAssertions, ICodeInfo> {
	public JadxCodeInfoAssertions(ICodeInfo cls) {
		super(cls, JadxCodeInfoAssertions.class);
	}

	public JadxCodeAssertions code() {
		isNotNull();
		String codeStr = actual.getCodeStr();
		assertThat(codeStr).isNotBlank();
		return new JadxCodeAssertions(codeStr);
	}

	public JadxCodeInfoAssertions checkCodeOffsets() {
		long dupOffsetCount = actual.getCodeMetadata().getAsMap().values().stream()
				.filter(InsnCodeOffset.class::isInstance)
				.collect(Collectors.groupingBy(o -> ((InsnCodeOffset) o).getOffset(), Collectors.toList()))
				.values().stream()
				.filter(list -> list.size() > 1)
				.count();
		assertThat(dupOffsetCount)
				.describedAs("Found duplicated code offsets")
				.isEqualTo(0);
		return this;
	}
}
