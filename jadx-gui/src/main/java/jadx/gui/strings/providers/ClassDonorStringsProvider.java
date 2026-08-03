package jadx.gui.strings.providers;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import jadx.api.JavaClass;

public abstract class ClassDonorStringsProvider implements IStringsProvider {

	private final List<IStringsProviderDonorHandler> subscribers = new LinkedList<>();

	public void subscribeToDonor(final IStringsProviderDonorHandler handler) {
		this.subscribers.add(handler);
	}

	protected void publishProviderFinishedToFallbackHandlers() {
		publishClassToFallbackHandlers(null);
	}

	protected void publishClassToFallbackHandlers(final JavaClass cls) {
		this.subscribers.forEach((final IStringsProviderDonorHandler handler) -> handler.addClassViaFallback(Optional.ofNullable(cls)));
	}
}
