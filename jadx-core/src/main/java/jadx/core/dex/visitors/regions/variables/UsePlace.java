package jadx.core.dex.visitors.regions.variables;

import java.util.Objects;

import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IRegion;

public class UsePlace {
	public final IRegion region;
	public final IBlock block;

	public UsePlace(IRegion region, IBlock block) {
		this.region = region;
		this.block = block;
	}

	public IRegion getRegion() {
		return region;
	}

	public IBlock getBlock() {
		return block;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		UsePlace usePlace = (UsePlace) o;
		return Objects.equals(region, usePlace.region)
				&& Objects.equals(block, usePlace.block);
	}

	@Override
	public int hashCode() {
		return Objects.hash(region, block);
	}

	@Override
	public String toString() {
		return "UsePlace{region=" + region + ", block=" + block + '}';
	}
}
