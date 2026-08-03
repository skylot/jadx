package jadx.gui.strings.providers;

import java.util.Optional;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import jadx.api.JavaClass;
import jadx.gui.jobs.Cancelable;
import jadx.gui.strings.StringResult;

public interface IStringsProvider {

	/**
	 * Return next result or null if search complete
	 */
	@Nullable
	public StringResult next(final Supplier<Optional<JavaClass>> clsSupplier, final Cancelable cancelable);
}
