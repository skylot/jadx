package jadx.gui.update.data;

import com.google.gson.annotations.SerializedName;

public class Asset {
	private int id;
	private String name;
	private long size;

	@SerializedName("download_count")
	private int downloadCount;

	@SerializedName("browser_download_url")
	private String downloadUrl;

	@SerializedName("created_at")
	private String createdAt;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public int getDownloadCount() {
		return downloadCount;
	}

	public void setDownloadCount(int downloadCount) {
		this.downloadCount = downloadCount;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public String toString() {
		return name
				+ ", size: " + String.format("%.2fMB", size / 1024. /1024.)
				+ ", downloads count: " + downloadCount
				+ ", url: " + downloadUrl
				+ ", date: " + createdAt;
	}
}
