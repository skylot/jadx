package jadx.gui.strings.providers;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import jadx.api.JavaClass;
import jadx.gui.jobs.ITaskProgress;

public class ListStringsProviderDelegate extends StringsProviderDelegate {

	public static ListStringsProviderDelegate createListDelegateForProvider(final IStringsProvider provider,
			final List<JavaClass> classes) {
		final ListSupplier supplier = new ListSupplier(classes);
		return new ListStringsProviderDelegate(provider, supplier);
	}

	private static class ListSupplier implements Supplier<Optional<JavaClass>>, ITaskProgress {

		private final List<JavaClass> classes;
		private final int classesSize;

		private int index = 0;
		private int progress = 0;
		private int maxProgress = 0;

		public ListSupplier(final List<JavaClass> classes) {
			this.classes = classes;
			this.classesSize = classes.size();
			this.maxProgress = this.classesSize;
		}

		@Override
		public Optional<JavaClass> get() {
			if (this.index >= this.classesSize) {
				return Optional.empty();
			}

			final JavaClass next = this.classes.get(this.index);
			this.index++;
			return Optional.of(next);
		}

		@Override
		public int progress() {
			return this.progress + this.index;
		}

		@Override
		public int total() {
			return this.maxProgress;
		}
	}

	private final ListSupplier supplier;

	private ListStringsProviderDelegate(final IStringsProvider stringsProvider, final ListSupplier supplier) {
		super(stringsProvider, supplier);

		this.supplier = supplier;

		if (stringsProvider instanceof ClassDonorStringsProvider) {
			// If we donate a class to another delegate, apply a reduction to the task progress current and
			// maximum
			final ClassDonorStringsProvider donor = (ClassDonorStringsProvider) stringsProvider;
			donor.subscribeToDonor(cls -> {
				supplier.progress--;
				supplier.maxProgress--;
			});
		}
	}

	@Override
	public int progress() {
		return supplier.progress();
	}

	@Override
	public int total() {
		return supplier.total();
	}
}
