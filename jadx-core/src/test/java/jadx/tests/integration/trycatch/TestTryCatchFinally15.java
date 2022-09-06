package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Negative test case for finally extract (issue 1592).
 * Different registers incorrectly merged into one.
 */
@SuppressWarnings({ "CommentedOutCode", "GrazieInspection" })
public class TestTryCatchFinally15 extends SmaliTest {

	// @formatter:off
	/*
		protected final Parcel test(int i, Parcel parcel) throws RemoteException {
			Parcel obtain = Parcel.obtain();
			try {
				try {
					this.zza.transact(i, parcel, obtain, 0);
					obtain.readException();
					return obtain;
				} catch (RuntimeException e) {
					obtain.recycle();
					throw e;
				}
			} finally {
				parcel.recycle();
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		disableCompilation();
		assertThat(getClassNodeFromSmali())
				.code()
				.doesNotContain("parcel = Parcel.obtain();")
				.containsOne("this.zza.transact(i, parcel, obtain, 0);");
	}
}
