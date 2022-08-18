package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.api.JadxInternalAccess;
import jadx.api.JavaClass;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestAnonymousClass20 extends IntegrationTest {

	@SuppressWarnings({ "unused", "checkstyle:TypeName", "Convert2Lambda", "Anonymous2MethodRef" })
	public static class Test$Cls {
		public Runnable test() {
			return new Runnable() {
				@Override
				public void run() {
					new Test$Cls();
				}
			};
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(Test$Cls.class);
		assertThat(cls.get(AType.ANONYMOUS_CLASS)).isNull();

		JavaClass javaClass = JadxInternalAccess.convertClassNode(jadxDecompiler, cls);
		assertThat(javaClass.getTopParentClass()).isEqualTo(javaClass);

		assertThat(cls)
				.code()
				.containsOne("new TestAnonymousClass20$Test$Cls();");
	}
}
