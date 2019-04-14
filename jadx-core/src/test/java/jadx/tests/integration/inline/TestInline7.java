package jadx.tests.integration.inline;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestInline7 extends SmaliTest {

	// @formatter:off
	/*
		public void onViewCreated(View view, @Nullable Bundle bundle) {
			super.onViewCreated(view, bundle);
			view.findViewById(R.id.done_button_early_release_failure).setOnClickListener(new SafeClickListener(this));
			Bundle arguments = getArguments();
			if (arguments != null) {
				((TextView) view.findViewById(R.id.summary_content_early_release_failure)).setText(
					getString(R.string.withdraw_id_capture_failure_content,
						new Object[]{arguments.getString("withdrawAmount"), arguments.getString ("withdrawHoldTime")})
				);
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		disableCompilation();
		ClassNode cls = getClassNodeFromSmaliWithPkg("inline", "TestInline7");
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("Bundle arguments;")));
		assertThat(code, containsOne("Bundle arguments = getArguments();"));
		assertThat(code, containsOne("@Nullable Bundle bundle"));
	}
}
