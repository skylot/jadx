package jadx.core.plugins.versions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VerifyRequiredVersionTest {
	@Test
	public void test() {
		isCompatible("1.5.0, r2000", "1.5.1", true);
		isCompatible("1.5.1, r3000", "1.5.1", true);
		isCompatible("1.5.1, r3000", "1.6.0", true);
		isCompatible("1.5.1, r3000", "1.5.0", false);

		isCompatible("1.5.1, r3000", "r3001.417bb7a", true);
		isCompatible("1.5.1, r3000", "r4000", true);
		isCompatible("1.5.1, r3000", "r3000", true);
		isCompatible("1.5.1, r3000", "r2000", false);
	}

	private static void isCompatible(String requiredVersion, String jadxVersion, boolean result) {
		assertThat(new VerifyRequiredVersion(jadxVersion).isCompatible(requiredVersion))
				.as("Expect plugin with required version %s is%s compatible with jadx %s",
						requiredVersion, result ? "" : " not", jadxVersion)
				.isEqualTo(result);
	}
}
