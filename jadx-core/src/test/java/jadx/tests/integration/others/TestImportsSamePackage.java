package jadx.tests.integration.others;

import static jadx.core.utils.files.FileUtils.addFileToJar;
import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

public class TestImportsSamePackage extends IntegrationTest {

	@Test
	public void test() {
		args.setUseImports(false);
		ClassNode cls = getClassNode(TestImportsSamePackage1.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("others.TestImportsSamePackage2")));
	}

	@Override
	public File getJarForClass(Class<?> cls) throws IOException {
		String path = cls.getPackage().getName().replace('.', '/');
		File temp = createTempFile(".jar");
		try (JarOutputStream jo = new JarOutputStream(new FileOutputStream(temp))) {
			File file = new File(new URI(cls.getProtectionDomain().getCodeSource().getLocation().toURI().toString()
					+ '/' + cls.getName().replace('.', '/') + ".class"));
			addFileToJar(jo, file, path + '/' + file.getName());
		} catch (URISyntaxException e) {
			fail(e);
		}
		return temp;
	}
}

class TestImportsSamePackage1 {
	TestImportsSamePackage2 package2;
}

class TestImportsSamePackage2 {
}