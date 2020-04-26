package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTypeResolver14 extends SmaliTest {
	// @formatter:off
	/*
		public Date test() throws Exception {
			Date date = null;
			Long l = null;
			Cursor query = DBUtil.query(false, (CancellationSignal) null);
			try {
				if (query.moveToFirst()) {
					if (!query.isNull(0)) {
						l = Long.valueOf(query.getLong(0));
					}
					date = this.this$0.toDate(l);
				}
				return date;
			} finally {
				query.close();
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		disableCompilation();
		assertThat(getClassNodeFromSmali())
				.code()
				.doesNotContain("? r2");
	}
}
