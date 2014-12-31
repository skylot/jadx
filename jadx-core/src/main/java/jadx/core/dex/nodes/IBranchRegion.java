package jadx.core.dex.nodes;

import java.util.List;

public interface IBranchRegion extends IRegion {

	/**
	 * Return list of branches in this region.
	 * NOTE: Contains 'null' elements for indicate empty branches.
	 */
	List<IContainer> getBranches();

}
