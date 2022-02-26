package jadx.tests.api.extensions.profiles;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
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

public class JadxTestProfilesExtension implements TestTemplateInvocationContextProvider {

	@Override
	public boolean supportsTestTemplate(ExtensionContext context) {
		return isAnnotated(context.getTestMethod(), TestWithProfiles.class);
	}

	@Override
	public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
		Preconditions.condition(IntegrationTest.class.isAssignableFrom(context.getRequiredTestClass()),
				"@TestWithProfiles should be used only in IntegrationTest subclasses");

		Method testMethod = context.getRequiredTestMethod();
		boolean testAnnAdded = AnnotationUtils.findAnnotation(testMethod, Test.class).isPresent();
		Preconditions.condition(!testAnnAdded, "@Test annotation should be removed");

		TestWithProfiles profilesAnn = AnnotationUtils.findAnnotation(testMethod, TestWithProfiles.class).get();
		EnumSet<TestProfile> profilesSet = EnumSet.noneOf(TestProfile.class);
		Collections.addAll(profilesSet, profilesAnn.value());
		if (profilesSet.contains(TestProfile.ALL)) {
			Collections.addAll(profilesSet, TestProfile.values());
		}
		profilesSet.remove(TestProfile.ALL);
		return profilesSet.stream()
				.sorted()
				.map(RunWithProfile::new);
	}

	private static class RunWithProfile implements TestTemplateInvocationContext {
		private final TestProfile testProfile;

		public RunWithProfile(TestProfile testProfile) {
			this.testProfile = testProfile;
		}

		@Override
		public String getDisplayName(int invocationIndex) {
			return testProfile.getDescription();
		}

		@Override
		public List<Extension> getAdditionalExtensions() {
			return Collections.singletonList(beforeTest());
		}

		private BeforeTestExecutionCallback beforeTest() {
			return execContext -> testProfile.accept((IntegrationTest) execContext.getRequiredTestInstance());
		}
	}
}
