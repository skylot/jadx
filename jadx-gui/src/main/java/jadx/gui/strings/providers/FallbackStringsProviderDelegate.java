package jadx.gui.strings.providers;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

import jadx.api.JavaClass;

public class FallbackStringsProviderDelegate extends StringsProviderDelegate {

	public static FallbackStringsProviderDelegate createFallbackForProvider(final ClassDonorStringsProvider sourceProvider,
			final IStringsProvider fallbackHandlerProvider) {
		final FallbackSupplier supplier = new FallbackSupplier(sourceProvider);
		final FallbackStringsProviderDelegate delegate =
				new FallbackStringsProviderDelegate(sourceProvider, fallbackHandlerProvider, supplier);
		supplier.delegate = delegate;
		return delegate;
	}

	private static class FallbackSupplier implements Supplier<Optional<JavaClass>> {
		private final BlockingQueue<Optional<JavaClass>> pendingQueue;

		private FallbackStringsProviderDelegate delegate;

		public FallbackSupplier(final ClassDonorStringsProvider sourceProvider) {
			this.pendingQueue = new LinkedBlockingQueue<>();
			sourceProvider.subscribeToDonor(this::addClassToQueue);
		}

		@Override
		public Optional<JavaClass> get() {
			Optional<JavaClass> next;
			try {
				next = pendingQueue.take();
			} catch (final InterruptedException e) {
				next = Optional.empty();
			}

			if (next.isEmpty()) {
				delegate.markProgressAsDone();
			} else {
				delegate.incrementProgress();
			}
			return next;
		}

		private void addClassToQueue(final Optional<JavaClass> cls) {
			this.pendingQueue.add(cls);
		}
	}

	private int index = 0;
	private int total = 1;

	private FallbackStringsProviderDelegate(final ClassDonorStringsProvider sourceProvider, final IStringsProvider fallbackHandler,
			final FallbackSupplier supplier) {
		super(fallbackHandler, supplier);
	}

	@Override
	public int progress() {
		return this.index;
	}

	@Override
	public int total() {
		return this.total;
	}

	private void markProgressAsDone() {
		this.index += 1;
	}

	private void incrementProgress() {
		this.index += 1;
		this.total += 1;
	}
}
