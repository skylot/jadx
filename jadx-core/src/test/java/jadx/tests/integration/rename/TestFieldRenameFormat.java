package jadx.tests.integration.rename;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.annotations.SerializedName;

import jadx.api.data.ICodeRename;
import jadx.api.data.IJavaNodeRef.RefType;
import jadx.api.data.impl.JadxCodeData;
import jadx.api.data.impl.JadxCodeRename;
import jadx.api.data.impl.JadxNodeRef;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestFieldRenameFormat extends IntegrationTest {

	@SuppressWarnings({ "unused", "NonSerializableClassWithSerialVersionUID" })
	public static class TestCls {
		private static final long serialVersionUID = -2619335455376089892L;
		@SerializedName("id")
		private int b;
		@SerializedName("title")
		private String c;
		@SerializedName("images")
		private List<String> d;
		@SerializedName("authors")
		private List<String> e;
		@SerializedName("description")
		private String f;
	}

	@Test
	public void test() {
		noDebugInfo();

		String baseClsId = TestCls.class.getName();
		List<ICodeRename> renames = Arrays.asList(
				fieldRename(baseClsId, "b:I", "id"),
				fieldRename(baseClsId, "c:Ljava/lang/String;", "title"),
				fieldRename(baseClsId, "e:Ljava/util/List;", "authors"));

		JadxCodeData codeData = new JadxCodeData();
		codeData.setRenames(renames);
		getArgs().setCodeData(codeData);
		getArgs().setDeobfuscationOn(false);

		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("private int id;")
				.containsOne("private List<String> authors;")
				.containsLines(1,
						"",
						"/* renamed from: c */",
						"@SerializedName(\"title\")",
						"private String title;",
						"");
	}

	private static JadxCodeRename fieldRename(String baseClsId, String shortId, String id) {
		return new JadxCodeRename(new JadxNodeRef(RefType.FIELD, baseClsId, shortId), id);
	}
}
