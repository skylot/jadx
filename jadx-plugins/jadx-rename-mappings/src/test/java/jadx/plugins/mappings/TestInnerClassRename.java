package jadx.plugins.mappings;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;

import static org.assertj.core.api.Assertions.assertThat;

class TestInnerClassRename extends BaseRenameMappingsTest {

	@Test
	public void test() {
		testResDir = "inner-cls-rename";
		jadxArgs.getInputFiles().add(loadResourceFile("base.smali"));
		jadxArgs.getInputFiles().add(loadResourceFile("inner.smali"));
		jadxArgs.setUserRenamesMappingsPath(loadResourceFile("enigma.mapping").toPath());
		try (JadxDecompiler jadx = new JadxDecompiler(jadxArgs)) {
			jadx.load();
			List<JavaClass> classes = jadx.getClasses();
			printClassesCode(classes);
			assertThat(classes).hasSize(1);
			JavaClass baseCls = classes.get(0);
			assertThat(baseCls.getName()).isEqualTo("BaseCls");
			List<JavaClass> innerClasses = baseCls.getInnerClasses();
			assertThat(innerClasses).hasSize(1);
			assertThat(innerClasses.get(0).getName()).isEqualTo("RenamedInner");
			assertThat(baseCls.getCode()).contains("class RenamedInner {");
		}
	}
}
