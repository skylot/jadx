package jadx.gui.links;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaMethod;

import static org.assertj.core.api.Assertions.assertThat;

class JadxLinkResolverTest {
	private static final String HASH = "0123456789abcdef";

	@Test
	void resolveAllMethodsFromBuiltLinks() throws Exception {
		JadxArgs args = new JadxArgs();
		args.getInputFiles().add(new File(getClass().getResource("/samples/small.apk").toURI()));
		try (JadxDecompiler decompiler = new JadxDecompiler(args)) {
			decompiler.load();
			JadxLinkResolver resolver = new JadxLinkResolver(decompiler);
			List<JavaClass> classes = decompiler.getClassesWithInners();
			assertThat(classes).isNotEmpty();
			int methodsCount = 0;
			for (JavaClass cls : classes) {
				String clsLink = JadxLink.build(HASH, cls.getClassNode().getClassInfo().getFullName());
				assertThat(resolver.resolve(JadxLink.parse(clsLink))).isEqualTo(cls);
				// raw name with '$' for inner classes also accepted
				String rawClsLink = "jadx://" + HASH.substring(0, 10) + '/' + cls.getClassNode().getClassInfo().makeRawFullName();
				assertThat(resolver.resolve(JadxLink.parse(rawClsLink))).isEqualTo(cls);

				for (JavaMethod mth : cls.getMethods()) {
					String link = JadxLink.build(HASH, mth.getMethodNode().getMethodInfo());
					assertThat(link).startsWith("jadx://0123456789/");
					assertThat(resolver.resolve(JadxLink.parse(link)))
							.as("resolve link: %s", link)
							.isEqualTo(mth);
					methodsCount++;
				}
			}
			assertThat(methodsCount).isPositive();

			JavaClass cls = classes.get(0);
			JavaMethod mth = cls.getMethods().get(0);
			String clsName = cls.getClassNode().getClassInfo().getFullName();
			String mthName = mth.getMethodNode().getMethodInfo().getName();
			// method name without args
			assertThat(resolver.resolve(JadxLink.parse("jadx://0123456789/" + clsName + '.' + mthName))).isNotNull();
			// unknown targets
			assertThat(resolver.resolve(JadxLink.parse("jadx://0123456789/" + clsName + ".noSuchMethod()"))).isNull();
			assertThat(resolver.resolve(JadxLink.parse("jadx://0123456789:no.such.Cls.m()"))).isNull();
		}
	}
}
