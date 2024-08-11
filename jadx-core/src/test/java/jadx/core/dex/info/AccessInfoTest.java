package jadx.core.dex.info;

import org.junit.jupiter.api.Test;

import jadx.api.plugins.input.data.AccessFlags;
import jadx.core.dex.info.AccessInfo.AFType;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

public class AccessInfoTest {

	@Test
	public void changeVisibility() {
		AccessInfo accessInfo = new AccessInfo(AccessFlags.PROTECTED | AccessFlags.STATIC, AFType.METHOD);
		AccessInfo result = accessInfo.changeVisibility(AccessFlags.PUBLIC);

		assertThat(result.isPublic()).isTrue();
		assertThat(result.isPrivate()).isFalse();
		assertThat(result.isProtected()).isFalse();

		assertThat(result.isStatic()).isTrue();
	}

	@Test
	public void changeVisibilityNoOp() {
		AccessInfo accessInfo = new AccessInfo(AccessFlags.PUBLIC, AFType.METHOD);
		AccessInfo result = accessInfo.changeVisibility(AccessFlags.PUBLIC);
		assertThat(result).isSameAs(accessInfo);
	}
}
