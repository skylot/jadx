package jadx.tests.integration.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTypeResolver26 extends SmaliTest {

	@Test
	public void test() {
		allowWarnInCode();
		disableCompilation();

		ClassNode cn = getClassNodeFromSmali();

		System.out.println(cn.getCode().toString());
		List<FieldNode> fields = (ArrayList<FieldNode>) cn.getFields();
		assertThat(fields.size()).isEqualTo(10);

		Map<String, String> mappings = new HashMap<>();
		mappings.put("L", "bool");
		mappings.put("L1", "java.lang.Boolean");
		mappings.put("M", "byte");
		mappings.put("N", "short");
		mappings.put("O", "char");
		mappings.put("P", "float");
		mappings.put("Q", "int");
		mappings.put("R", "long");
		mappings.put("S", "double");
		mappings.put("T", "boolean");

		for (FieldNode field : fields) {
			String name = field.getName();
			String type = field.getType().toString();
			assertThat(mappings.containsKey(name));
			assertThat(mappings.get(name)).isEqualTo(type);
		}
	}
}
