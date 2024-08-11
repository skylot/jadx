package jadx.tests.integration.android;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.tests.api.IntegrationTest;

import static jadx.api.plugins.input.data.attributes.JadxAttrType.CONSTANT_VALUE;
import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(cls).code()
				.containsOne("return R.id.Button;")
				.doesNotContain("import R;");

		// check 'R' class
		ClassNode rCls = cls.root().searchClassByFullAlias("R");
		assertThat(rCls).isNotNull();

		// check inner 'id' class
		List<ClassNode> innerClasses = rCls.getInnerClasses();
		assertThat(innerClasses).hasSize(1);
		ClassNode idCls = innerClasses.get(0);
		assertThat(idCls.getShortName()).isEqualTo("id");

		// check 'Button' field
		FieldNode buttonField = idCls.searchFieldByName("Button");
		assertThat(buttonField).isNotNull();
		EncodedValue constVal = buttonField.get(CONSTANT_VALUE);
		Integer buttonValue = (Integer) constVal.getValue();
		assertThat(buttonValue).isEqualTo(buttonConstValue);
	}
}
