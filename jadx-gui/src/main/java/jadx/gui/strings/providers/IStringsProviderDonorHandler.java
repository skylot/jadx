package jadx.gui.strings.providers;

import java.util.Optional;

import jadx.api.JavaClass;

@FunctionalInterface
public interface IStringsProviderDonorHandler {
	public void addClassViaFallback(final Optional<JavaClass> unprocessedCls);
}
