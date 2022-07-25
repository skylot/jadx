package jadx.core.utils;

import org.junit.jupiter.api.Test;

import static jadx.core.utils.BetterName.calcRating;
import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestBetterName {

	@Test
	public void test() {
		expectFirst("color_main", "t0");
		expectFirst("done", "oOo0oO0o");
	}

	private void expectFirst(String first, String second) {
		String best = BetterName.compareAndGet(first, second);
		assertThat(best)
				.as(() -> String.format("'%s'=%d, '%s'=%d", first, calcRating(first), second, calcRating(second)))
				.isEqualTo(first);
	}
}
