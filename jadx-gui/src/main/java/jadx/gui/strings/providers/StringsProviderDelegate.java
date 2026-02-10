package jadx.gui.strings.providers;

import java.util.Optional;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import jadx.api.JavaClass;
import jadx.gui.jobs.Cancelable;
import jadx.gui.jobs.ITaskProgress;
import jadx.gui.strings.StringResult;

public abstract class StringsProviderDelegate implements ITaskProgress {

	private final IStringsProvider provider;
	private final Supplier<Optional<JavaClass>> clsSupplier;

	protected boolean markedAsComplete = false;

	public StringsProviderDelegate(final IStringsProvider provider, final Supplier<Optional<JavaClass>> clsSupplier) {
		this.provider = provider;
		this.clsSupplier = clsSupplier;
	}

	@Nullable
	public StringResult next(final Cancelable cancelable) {
		if (cancelable.isCanceled()) {
			return null;
		}

		return this.provider.next(this.clsSupplier, cancelable);
	}
}
