package jadx.tests.integration.generics;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestMethodOverride extends SmaliTest {

	@Test
	public void test() {
		disableCompilation();

		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("String createFromParcel(Parcel parcel) {")
				.containsOne("String[] newArray(int i) {")
				.countString(2, "@Override");
	}
}
