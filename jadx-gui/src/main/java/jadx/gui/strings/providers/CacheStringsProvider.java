package jadx.gui.strings.providers;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import jadx.api.JavaClass;
import jadx.core.dex.nodes.ClassNode;
import jadx.gui.jobs.Cancelable;
import jadx.gui.strings.SingleStringResult;
import jadx.gui.strings.caching.IStringsInfoCache;

public final class CacheStringsProvider extends ClassDonorStringsProvider {

	private final IStringsInfoCache stringsCache;

	private Iterator<SingleStringResult> resultIterator;

	public CacheStringsProvider(final IStringsInfoCache stringsCache) {
		this.stringsCache = stringsCache;
	}

	@Override
	public @Nullable SingleStringResult next(final Supplier<Optional<JavaClass>> clsSupplier, final Cancelable cancelable) {
		while (true) {
			if (cancelable.isCanceled()) {
				break;
			}

			if (resultIterator == null || !resultIterator.hasNext()) {
				final Optional<JavaClass> nextOptional = clsSupplier.get();
				if (nextOptional.isEmpty()) {
					// No more classes to search
					break;
				}

				final JavaClass next = nextOptional.get();
				final ClassNode clsNode = next.getClassNode();
				if (!stringsCache.contains(clsNode)) {
					// Key has been invalidated or doesn't exist in cache
					publishClassToFallbackHandlers(next);
					continue;
				}
				final List<SingleStringResult> resultsListForClass = stringsCache.getStrings(clsNode);
				if (resultsListForClass == null) {
					// Key has been invalidated
					publishClassToFallbackHandlers(next);
					continue;
				}
				final Iterator<SingleStringResult> newIterator = resultsListForClass.iterator();
				if (!newIterator.hasNext()) {
					continue;
				}
				resultIterator = newIterator;
			}

			return resultIterator.next();
		}

		publishProviderFinishedToFallbackHandlers();
		return null;
	}
}
