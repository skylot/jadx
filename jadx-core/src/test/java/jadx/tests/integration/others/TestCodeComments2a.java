package jadx.tests.integration.others;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

import jadx.api.data.ICodeComment;
import jadx.api.data.IJavaNodeRef.RefType;
import jadx.api.data.impl.JadxCodeComment;
import jadx.api.data.impl.JadxCodeData;
import jadx.api.data.impl.JadxNodeRef;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestCodeComments2a extends IntegrationTest {

	public static class TestCls {
		private int f;

		public int test(boolean z) {
			if (z) {
				System.out.println("z");
				return new Random().nextInt();
			}
			return f;
		}
	}

	@Test
	public void test() {
		String baseClsId = TestCls.class.getName();
		JadxNodeRef mthRef = new JadxNodeRef(RefType.METHOD, baseClsId, "test(Z)I");
		ICodeComment insnComment = new JadxCodeComment(mthRef, "return comment", 18);
		ICodeComment insnComment2 = new JadxCodeComment(mthRef, "another return comment", 19);

		JadxCodeData codeData = new JadxCodeData();
		codeData.setComments(Arrays.asList(insnComment, insnComment2));
		getArgs().setCodeData(codeData);

		assertThat(getClassNode(TestCls.class))
				.decompile()
				.checkCodeOffsets()
				.code()
				.containsOne("// " + insnComment.getComment())
				.containsOne("// " + insnComment2.getComment());
	}
}
