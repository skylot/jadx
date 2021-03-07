package jadx.tests.integration.others;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import jadx.api.data.ICodeComment;
import jadx.api.data.IJavaNodeRef.RefType;
import jadx.api.data.impl.JadxCodeComment;
import jadx.api.data.impl.JadxCodeData;
import jadx.api.data.impl.JadxNodeRef;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestCodeCommentsMultiline extends IntegrationTest {

	public static class TestCls {
		public int test(boolean z) {
			if (z) {
				System.out.println("z");
				return 1;
			}
			return 3;
		}
	}

	@Test
	public void test() {
		String baseClsId = TestCls.class.getName();
		JadxNodeRef mthRef = new JadxNodeRef(RefType.METHOD, baseClsId, "test(Z)I");
		ICodeComment insnComment = new JadxCodeComment(mthRef, "multi\nline\ncomment", 11);

		JadxCodeData codeData = new JadxCodeData();
		codeData.setComments(Collections.singletonList(insnComment));
		getArgs().setCodeData(codeData);

		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("// multi")
				.containsOne("// line")
				.containsOne("// comment");
	}
}
