package jadx.tests.integration.android;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestResConstReplace3 extends IntegrationTest {

	@Retention(RetentionPolicy.RUNTIME)
	public @interface UsesAndroidResource {
		int value() default 0;
	}

	@UsesAndroidResource(17039370 /* android.R.string.ok */)
	public static class TestCls {
		public void test(@UsesAndroidResource(17039370 /* android.R.string.ok */) int i) {
		}
	}

	@Test
	public void test() {
		disableCompilation();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("import android.R;")
				.countString(2, "@TestResConstReplace3.UsesAndroidResource(R.string.ok)");
	}
}
