package jadx.tests.api.extensions.inputs;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.util.AnnotationUtils;
import org.junit.platform.commons.util.Preconditions;

import jadx.tests.api.IntegrationTest;

import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;

public class JadxInputPluginsExtension implements TestTemplateInvocationContextProvider {

	@Override
	public boolean supportsTestTemplate(ExtensionContext context) {
		return isAnnotated(context.getTestMethod(), TestWithInputPlugins.class);
	}

	@Override
	public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
		Preconditions.condition(IntegrationTest.class.isAssignableFrom(context.getRequiredTestClass()),
				"@TestWithInputPlugins should be used only in IntegrationTest subclasses");

		Method testMethod = context.getRequiredTestMethod();
		boolean testAnnAdded = AnnotationUtils.findAnnotation(testMethod, Test.class).isPresent();
		Preconditions.condition(!testAnnAdded, "@Test annotation should be removed");

		TestWithInputPlugins inputPluginAnn = AnnotationUtils.findAnnotation(testMethod, TestWithInputPlugins.class).get();
		return Stream.of(inputPluginAnn.value())
				.sorted()
				.map(RunWithInputPlugin::new);
	}

	private static class RunWithInputPlugin implements TestTemplateInvocationContext {
		private final InputPlugin plugin;

		public RunWithInputPlugin(InputPlugin plugin) {
			this.plugin = plugin;
		}

		@Override
		public String getDisplayName(int invocationIndex) {
			return plugin.name().toLowerCase(Locale.ROOT) + " input";
		}

		@Override
		public List<Extension> getAdditionalExtensions() {
			return Collections.singletonList(beforeTest());
		}

		private BeforeTestExecutionCallback beforeTest() {
			return execContext -> plugin.accept((IntegrationTest) execContext.getRequiredTestInstance());
		}
	}
}
