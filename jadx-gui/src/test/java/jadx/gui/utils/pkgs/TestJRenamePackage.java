package jadx.gui.utils.pkgs;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestJRenamePackage {

	@Test
	void isValidName() {
		valid("foo");
		valid("foo.bar");
		valid(".bar");

		invalid("");
		invalid("0foo");
		invalid("foo.");
		invalid("do");
		invalid("foo.if");
		invalid("foo.if.bar");
	}

	private void valid(String name) {
		assertThat(JRenamePackage.isValidPackageName(name))
				.as("expect valid: %s", name)
				.isEqualTo(true);
	}

	private void invalid(String name) {
		assertThat(JRenamePackage.isValidPackageName(name))
				.as("expect invalid: %s", name)
				.isEqualTo(false);
	}
}
