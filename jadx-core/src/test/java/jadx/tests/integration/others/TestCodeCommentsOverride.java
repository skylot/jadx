package jadx.tests.integration.others;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import jadx.api.data.ICodeComment;
import jadx.api.data.IJavaNodeRef.RefType;
import jadx.api.data.impl.JadxCodeComment;
import jadx.api.data.impl.JadxCodeData;
import jadx.api.data.impl.JadxNodeRef;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestCodeCommentsOverride extends IntegrationTest {

	public static class TestCls {
		public interface I {
			void mth();
		}

		public static class A implements I {
			@Override
			public void mth() {
				System.out.println("mth");
			}
		}
	}

	@Test
	public void test() {
		String baseClsId = TestCls.class.getName();
		JadxNodeRef iMthRef = new JadxNodeRef(RefType.METHOD, baseClsId + "$I", "mth()V");
		ICodeComment iMthComment = new JadxCodeComment(iMthRef, "interface mth comment");

		JadxNodeRef mthRef = new JadxNodeRef(RefType.METHOD, baseClsId + "$A", "mth()V");
		ICodeComment mthComment = new JadxCodeComment(mthRef, "mth comment");

		JadxCodeData codeData = new JadxCodeData();
		codeData.setComments(Arrays.asList(iMthComment, mthComment));
		getArgs().setCodeData(codeData);

		ClassNode cls = getClassNode(TestCls.class);
		assertThat(cls)
				.decompile()
				.checkCodeOffsets()
				.code()
				.containsOne("@Override")
				.containsOne("// " + iMthComment.getComment())
				.containsOne("// " + mthComment.getComment());

		assertThat(cls)
				.reloadCode(this)
				.containsOne("@Override")
				.containsOne("// " + iMthComment.getComment())
				.containsOne("// " + mthComment.getComment());
	}
}
