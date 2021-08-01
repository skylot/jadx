package jadx.tests.integration.android;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

public class TestRFieldRestore extends IntegrationTest {

	public static class TestCls {
		public int test() {
			return 2131230730;
		}
	}

	@Test
	public void test() {
		// unknown R class
		disableCompilation();

		Map<Integer, String> map = new HashMap<>();
		int buttonConstValue = 2131230730;
		map.put(buttonConstValue, "id.Button");
		setResMap(map);

		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		assertThat(code, containsOne("return R.id.Button;"));
		assertThat(code, not(containsString("import R;")));

		// check 'R' class
		ClassNode rCls = cls.root().searchClassByFullAlias("R");
		assertThat(rCls, notNullValue());

		// check inner 'id' class
		List<ClassNode> innerClasses = rCls.getInnerClasses();
		assertThat(innerClasses, hasSize(1));
		ClassNode idCls = innerClasses.get(0);
		assertThat(idCls.getShortName(), is("id"));

		// check 'Button' field
		FieldNode buttonField = idCls.searchFieldByName("Button");
		assertThat(buttonField, notNullValue());
		EncodedValue constVal = buttonField.get(JadxAttrType.CONSTANT_VALUE);
		Integer buttonValue = (Integer) constVal.getValue();
		assertThat(buttonValue, is(buttonConstValue));
	}
}
