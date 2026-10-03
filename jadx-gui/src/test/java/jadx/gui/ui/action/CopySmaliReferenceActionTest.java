package jadx.gui.ui.action;

import org.junit.jupiter.api.Test;

import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.gui.ui.action.CopySmaliReferenceAction.getSmaliReference;
import static org.assertj.core.api.Assertions.assertThat;

class CopySmaliReferenceActionTest extends IntegrationTest {

	@SuppressWarnings("unused")
	public static class Inner {
		private int count;
		private String[][] names;

		public Inner(long id, Object obj) {
		}

		public void method(int i, String[] arr) {
		}

		public boolean[] noArgs() {
			return null;
		}
	}

	@Test
	public void test() {
		disableCompilation();
		ClassNode outerCls = getClassNode(CopySmaliReferenceActionTest.class);
		ClassNode cls = outerCls.getInnerClasses().stream()
				.filter(c -> c.getClassInfo().getShortName().equals("Inner"))
				.findFirst()
				.orElseThrow();

		String clsRef = "Ljadx/gui/ui/action/CopySmaliReferenceActionTest$Inner;";
		assertThat(getSmaliReference(outerCls.getClassInfo()))
				.isEqualTo("Ljadx/gui/ui/action/CopySmaliReferenceActionTest;");
		assertThat(getSmaliReference(cls.getClassInfo())).isEqualTo(clsRef);

		assertThat(getSmaliReference(cls.searchMethodByShortId("<init>(JLjava/lang/Object;)V").getMethodInfo()))
				.isEqualTo(clsRef + "-><init>(JLjava/lang/Object;)V");
		assertThat(getSmaliReference(cls.searchMethodByShortId("method(I[Ljava/lang/String;)V").getMethodInfo()))
				.isEqualTo(clsRef + "->method(I[Ljava/lang/String;)V");
		assertThat(getSmaliReference(cls.searchMethodByShortId("noArgs()[Z").getMethodInfo()))
				.isEqualTo(clsRef + "->noArgs()[Z");

		assertThat(getSmaliReference(cls.searchFieldByName("count").getFieldInfo()))
				.isEqualTo(clsRef + "->count:I");
		assertThat(getSmaliReference(cls.searchFieldByName("names").getFieldInfo()))
				.isEqualTo(clsRef + "->names:[[Ljava/lang/String;");
	}

	@Test
	public void testRenamed() {
		disableCompilation();
		ClassNode cls = getClassNode(CopySmaliReferenceActionTest.class);
		MethodInfo mth = cls.searchMethodByShortId("testRenamed()V").getMethodInfo();
		cls.getClassInfo().changePkgAndName("a.b", "Renamed");
		mth.setAlias("renamed");

		// original names should be used
		assertThat(getSmaliReference(mth)).isEqualTo("Ljadx/gui/ui/action/CopySmaliReferenceActionTest;->testRenamed()V");
	}
}
