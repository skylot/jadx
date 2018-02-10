package jadx.tests.functional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.core.Jadx;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.dex.visitors.JadxVisitor;

import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

public class JadxVisitorsOrderTest {

	private static final Logger LOG = LoggerFactory.getLogger(JadxVisitorsOrderTest.class);

	@Test
	public void testOrder() {
		List<IDexTreeVisitor> passes = Jadx.getPassesList(new JadxArgs());

		List<String> errors = check(passes);
		for (String str : errors) {
			LOG.error(str);
		}
		assertThat(errors, empty());
	}

	private static List<String> check(List<IDexTreeVisitor> passes) {
		List<Class<?>> classList = new ArrayList<>(passes.size());
		for (IDexTreeVisitor pass : passes) {
			classList.add(pass.getClass());
		}
		List<String> errors = new ArrayList<>();

		Set<String> names = new HashSet<>();
		for (int i = 0; i < passes.size(); i++) {
			IDexTreeVisitor pass = passes.get(i);
			JadxVisitor info = pass.getClass().getAnnotation(JadxVisitor.class);
			if (info == null) {
				LOG.warn("No JadxVisitor annotation for visitor: {}", pass.getClass().getName());
				continue;
			}
			String passName = pass.getClass().getSimpleName();
			if (!names.add(passName)) {
				errors.add("Visitor name conflict: " + passName + ", class: " + pass.getClass().getName());
			}
			for (Class<? extends IDexTreeVisitor> cls : info.runBefore()) {
				if (classList.indexOf(cls) < i) {
					errors.add("Pass " + passName + " must be before " + cls.getSimpleName());
				}
			}
			for (Class<? extends IDexTreeVisitor> cls : info.runAfter()) {
				if (classList.indexOf(cls) > i) {
					errors.add("Pass " + passName + " must be after " + cls.getSimpleName());
				}
			}
		}
		return errors;
	}
}
