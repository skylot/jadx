package jadx.tests.integration.others;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import jadx.api.JadxInternalAccess;
import jadx.api.JavaClass;
import jadx.api.JavaMethod;
import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeAnnotation.AnnType;
import jadx.api.metadata.ICodeMetadata;
import jadx.api.metadata.ICodeNodeRef;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestCodeMetadata extends IntegrationTest {

	public static class TestCls {
		public static class A {
			public String str;
		}

		public String test() {
			A a = new A();
			a.str = call();
			return a.str;
		}

		public static String call() {
			return "str";
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		assertThat(cls).code().containsOne("return a.str;");

		MethodNode testMth = getMethod(cls, "test");
		MethodNode callMth = getMethod(cls, "call");

		int callDefPos = callMth.getDefPosition();
		assertThat(callDefPos).isNotZero();

		JavaClass javaClass = JadxInternalAccess.convertClassNode(jadxDecompiler, cls);
		JavaMethod callJavaMethod = JadxInternalAccess.convertMethodNode(jadxDecompiler, callMth);
		List<Integer> callUsePlaces = javaClass.getUsePlacesFor(javaClass.getCodeInfo(), callJavaMethod);
		assertThat(callUsePlaces).hasSize(1);
		int callUse = callUsePlaces.get(0);

		ICodeMetadata metadata = cls.getCode().getCodeMetadata();
		System.out.println(metadata);
		ICodeNodeRef callDef = metadata.getNodeAt(callUse);
		assertThat(callDef).isSameAs(testMth);

		AtomicInteger endPos = new AtomicInteger();
		ICodeAnnotation testEnd = metadata.searchUp(callDefPos, (pos, ann) -> {
			if (ann.getAnnType() == AnnType.END) {
				endPos.set(pos);
				return ann;
			}
			return null;
		});
		assertThat(testEnd).isNotNull();
		int testEndPos = endPos.get();

		ICodeAnnotation closest = metadata.getClosestUp(testEndPos);
		assertThat(closest).isInstanceOf(FieldNode.class); // field reference from 'return a.str;'

		ICodeNodeRef nodeBelow = metadata.getNodeBelow(testEndPos);
		assertThat(nodeBelow).isSameAs(callMth);
	}
}
