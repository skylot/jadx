package jadx.tests.integration.generics;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

public class TestImportGenericMap extends IntegrationTest {

	@Test
	public void test() {
		ClassNode cls = getClassNode(SuperClass.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString(
				"import " + SuperClass.ToImport.class.getName().replace('$', '.') + ';'));
		assertThat(code, not(containsString(
				"import " + SuperClass.NotToImport.class.getName().replace('$', '.') + ';')));
	}
}

final class SuperClass<O extends SuperClass.ToImport> {

    interface ToImport {
    }

    interface NotToImport {
    }

    static final class Class1<C extends NotToImport> {
    }

    public <C extends NotToImport> SuperClass(Class1<C> zzf) {
    }

}