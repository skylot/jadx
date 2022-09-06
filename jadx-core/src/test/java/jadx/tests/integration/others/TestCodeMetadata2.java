package jadx.tests.integration.others;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.api.JavaClass;
import jadx.api.JavaMethod;
import jadx.api.metadata.ICodeMetadata;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.tests.api.IntegrationTest;

import static jadx.api.JadxInternalAccess.convertClassNode;
import static jadx.api.JadxInternalAccess.convertMethodNode;
import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestCodeMetadata2 extends IntegrationTest {

	public static class TestCls {
		@SuppressWarnings("Convert2Lambda")
		public Runnable test(boolean a) {
			if (a) {
				return new Runnable() {
					@Override
					public void run() {
						System.out.println("test");
					}
				};
			}
			System.out.println("another");
			return empty();
		}

		public static Runnable empty() {
			return new Runnable() {
				@Override
				public void run() {
					// empty
				}
			};
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		assertThat(cls).code().containsOne("return empty();");

		MethodNode testMth = getMethod(cls, "test");
		MethodNode emptyMth = getMethod(cls, "empty");

		JavaClass javaClass = convertClassNode(jadxDecompiler, cls);
		JavaMethod emptyJavaMethod = convertMethodNode(jadxDecompiler, emptyMth);
		List<Integer> emptyUsePlaces = javaClass.getUsePlacesFor(javaClass.getCodeInfo(), emptyJavaMethod);
		assertThat(emptyUsePlaces).hasSize(1);
		int callUse = emptyUsePlaces.get(0);

		ICodeMetadata metadata = cls.getCode().getCodeMetadata();
		assertThat(metadata.getNodeAt(callUse)).isSameAs(testMth);
	}
}
