package jadx.tests.integration.generics;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.integration.generics.TestImportGenericMap.SuperClass.NotToImport;
import static jadx.tests.integration.generics.TestImportGenericMap.SuperClass.ToImport;

public class TestImportGenericMap extends IntegrationTest {

	public static class SuperClass<O extends SuperClass.ToImport> {

		interface ToImport {
		}

		interface NotToImport {
		}

		static final class Class1<C extends NotToImport> {
		}

		public <C extends NotToImport> SuperClass(Class1<C> zzf) {
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(SuperClass.class))
				.code()
				.contains("import " + ToImport.class.getName().replace("$ToImport", ".ToImport") + ';')
				.doesNotContain("import " + NotToImport.class.getName().replace("NotToImport", ".NotToImport") + ';');
	}
}
