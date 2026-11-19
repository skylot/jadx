package jadx.tests.integration.generics;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.MethodBridgeAttr;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestOverrideCollisionRename extends IntegrationTest {

	public static class TestCls {
		public interface Mutable<T> {
			T getValue();

			void setValue(T value);
		}

		public static class MutableBoolean implements Mutable<Boolean> {
			private boolean value;

			@Override
			public Boolean getValue() {
				return value;
			}

			@Override
			public void setValue(Boolean value) {
				this.value = value;
			}
		}

		public static class MutableShort extends Number implements Mutable<Number> {
			private static final long serialVersionUID = 1L;
			private short value;

			@Override
			public Short getValue() {
				return value;
			}

			@Override
			public void setValue(Number value) {
				this.value = value.shortValue();
			}

			@Override
			public int intValue() {
				return value;
			}

			@Override
			public long longValue() {
				return value;
			}

			@Override
			public float floatValue() {
				return value;
			}

			@Override
			public double doubleValue() {
				return value;
			}
		}

		public static class Box<E> {
			private E item;

			public Box(E item) {
				this.item = item;
			}

			public E getItem() {
				return item;
			}
		}

		public static class ZBoxImpl implements Mutable<Box<String>> {
			@SuppressWarnings("rawtypes")
			private Box value;

			@SuppressWarnings("rawtypes")
			@Override
			public Box getValue() {
				return value;
			}

			@Override
			public void setValue(Box<String> value) {
				this.value = value;
			}
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.D8_J11, TestProfile.JAVA11 })
	public void testInline() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("T getValue();")
				.containsOne("public Boolean getValue() {")
				.doesNotContain("getValue2");
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.D8_J11, TestProfile.JAVA11 })
	public void testNoInline() {
		disableCompilation();
		allowWarnInCode();
		args.setInlineMethods(false);
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("T getValue();")
				.doesNotContain("getValue2");
	}

	public static class DiamondTestCls {
		public interface DiamondA {
			int op();
		}

		public interface DiamondB {
			int op();
		}

		public static class ImplA implements DiamondA {
			@Override
			public int op() {
				return 1;
			}
		}

		public static class ImplB implements DiamondB {
			@Override
			public int op() {
				return 2;
			}
		}

		public static class ImplAB implements DiamondA, DiamondB {
			@Override
			public int op() {
				return 3;
			}
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.D8_J11, TestProfile.JAVA11 })
	public void testDiamondMergeRename() {
		noDebugInfo();
		enableDeobfuscation();
		args.setDeobfuscationMinLength(100);

		ClassNode cls = getClassNode(DiamondTestCls.class);
		assertThat(cls)
				.code()
				.countString(3, "@Override")
				.doesNotContain("int op()");
	}

	public static class DontRenamePropagationCls {
		public interface CustomRun {
			void run();
		}

		public static class CustomOnly implements CustomRun {
			@Override
			public void run() {
			}
		}

		public static class ZBothRun implements CustomRun, Runnable {
			@Override
			public void run() {
			}
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.D8_J11, TestProfile.JAVA11 })
	public void testDontRenamePropagation() {
		noDebugInfo();
		enableDeobfuscation();
		args.setDeobfuscationMinLength(100);

		ClassNode cls = getClassNode(DontRenamePropagationCls.class);
		assertThat(cls)
				.code()
				.countString(3, "void run()")
				.countString(2, "@Override");
	}

	public static class BridgeAttrHierarchyCls {
		public interface ValueConsumer<T> {
			void accept(T value);
		}

		public static class StringConsumer implements ValueConsumer<String> {
			private String last;

			@Override
			public void accept(String value) {
				this.last = value;
			}

			public String getLast() {
				return last;
			}
		}

		public static class SubStringConsumer extends StringConsumer implements ValueConsumer<String> {
			@Override
			public void accept(String value) {
				super.accept(value);
			}
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.D8_J11, TestProfile.JAVA11 })
	public void testBridgeAttrOnAlreadyRenamedSubclass() {
		ClassNode cls = getClassNode(BridgeAttrHierarchyCls.class);
		ClassNode subCls = searchCls(cls.getInnerClasses(), "SubStringConsumer");
		MethodNode concreteAccept = subCls.searchMethodByShortId("accept(Ljava/lang/String;)V");
		MethodNode bridgeAccept = subCls.searchMethodByShortId("accept(Ljava/lang/Object;)V");
		assertThat(concreteAccept).isNotNull();
		assertThat(bridgeAccept).isNotNull();
		assertThat(bridgeAccept.getAccessFlags().isBridge()).isTrue();
		MethodBridgeAttr bridgeAttr = concreteAccept.get(AType.BRIDGED_BY);
		assertThat(bridgeAttr).isNotNull();
		assertThat(bridgeAttr.getBridgeMth()).isEqualTo(bridgeAccept);
		assertThat(cls)
				.code()
				.countString(2, "accept2(String value)")
				.countString(2, "public /* bridge */ /* synthetic */ void accept(String str)");
	}
}
