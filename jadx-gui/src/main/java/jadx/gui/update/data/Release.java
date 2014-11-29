package jadx.gui.update.data;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Release {
	private int id;
	private String name;

	@SerializedName("prerelease")
	private boolean preRelease;

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

	public boolean isPreRelease() {
		return preRelease;
	}

	public void setPreRelease(boolean preRelease) {
		this.preRelease = preRelease;
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
			sb.append("\n ");
			sb.append(asset);
		}
		return sb.toString();
	}
}
