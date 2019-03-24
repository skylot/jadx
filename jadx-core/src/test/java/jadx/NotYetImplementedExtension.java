package jadx;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

public class NotYetImplementedExtension implements AfterTestExecutionCallback, TestExecutionExceptionHandler {

	private Set<Method> knownFailedMethods = new HashSet<>();

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
		if (!isNotYetImplemented(context)) {
			throw throwable;
		}
		knownFailedMethods.add(context.getTestMethod().get());
	}

	@Override
	public void afterTestExecution(ExtensionContext context) throws Exception {
		if (!knownFailedMethods.contains(context.getTestMethod().get())
				&& isNotYetImplemented(context)
				&& !context.getExecutionException().isPresent()) {
			throw new AssertionError("Test "
					+ context.getTestClass().get().getName() + '.' + context.getTestMethod().get().getName()
					+ " is marked as @NotYetImplemented, but passes!");
		}
	}

	private static boolean isNotYetImplemented(ExtensionContext context) {
		return context.getTestMethod().get().getAnnotation(NotYetImplemented.class) != null
				|| context.getTestClass().get().getAnnotation(NotYetImplemented.class) != null;
	}
}
