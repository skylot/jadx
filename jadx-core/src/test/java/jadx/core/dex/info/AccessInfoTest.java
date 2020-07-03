package jadx.core.dex.info;

import org.junit.jupiter.api.Test;

import jadx.api.plugins.input.data.AccessFlags;
import jadx.core.dex.info.AccessInfo.AFType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertSame;

public class AccessInfoTest {

	@Test
	public void changeVisibility() {
		AccessInfo accessInfo = new AccessInfo(AccessFlags.PROTECTED | AccessFlags.STATIC, AFType.METHOD);
		AccessInfo result = accessInfo.changeVisibility(AccessFlags.PUBLIC);

		assertThat(result.isPublic(), is(true));
		assertThat(result.isPrivate(), is(false));
		assertThat(result.isProtected(), is(false));

		assertThat(result.isStatic(), is(true));
	}

	@Test
	public void changeVisibilityNoOp() {
		AccessInfo accessInfo = new AccessInfo(AccessFlags.PUBLIC, AFType.METHOD);
		AccessInfo result = accessInfo.changeVisibility(AccessFlags.PUBLIC);
		assertSame(accessInfo, result);
	}
}
