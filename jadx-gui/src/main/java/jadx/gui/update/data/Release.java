package jadx.gui.update.data;

import java.util.List;

public class Release {
	private int id;
	private String name;
	private boolean prerelease;
	private List<Asset> assets;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public boolean isPrerelease() {
		return prerelease;
	}

	public void setPrerelease(boolean prerelease) {
		this.prerelease = prerelease;
	}

	public List<Asset> getAssets() {
		return assets;
	}

	public void setAssets(List<Asset> assets) {
		this.assets = assets;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		for (Asset asset : getAssets()) {
			sb.append('\n');
			sb.append("  ").append(asset.getName())
					.append(", asset id: ").append(asset.getId())
					.append(", size: ").append(asset.getSize())
					.append(", dc: ").append(asset.getDownload_count());
		}
		return sb.toString();
	}
}
