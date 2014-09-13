package jadx.tests.smali;

import jadx.api.SmaliTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static jadx.tests.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestConstructor extends SmaliTest {

	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmali("TestConstructor");
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, containsOne("new SomeObject(arg3);"));
		assertThat(code, not(containsString("= someObject")));
	}
}
