package jadx.tests.integration.inner;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Issue: https://github.com/skylot/jadx/issues/336
 */
public class TestInnerClassSyntheticRename extends SmaliTest {

//	private class MyAsync extends AsyncTask<Uri, Uri, List<Uri>> {
//		@Override
//		protected List<Uri> doInBackground(Uri... uris) {
//			Log.i("MyAsync", "doInBackground");
//			return null;
//		}
//
//		@Override
//		protected void onPostExecute(List<Uri> uris) {
//			Log.i("MyAsync", "onPostExecute");
//		}
//	}

	@Test
	public void test() {
		disableCompilation();
		ClassNode cls = getClassNodeFromSmali("inner/TestInnerClassSyntheticRename", "com.github.skylot.testasync.MyAsync");
		String code = cls.getCode().toString();

		assertThat(code, containsOne("protected List<Uri> doInBackground(Uri... uriArr) {"));
		assertThat(code, containsOne("protected void onPostExecute(List<Uri> list) {"));
		assertThat(code, not(containsString("synthetic")));
	}
}
