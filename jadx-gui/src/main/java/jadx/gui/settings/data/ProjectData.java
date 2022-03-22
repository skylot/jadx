package jadx.gui.settings.data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jadx.api.data.impl.JadxCodeData;

public class ProjectData {

	private int projectVersion = 1;
	private List<Path> files;
	private List<String[]> treeExpansions = new ArrayList<>();
	private JadxCodeData codeData = new JadxCodeData();
	private List<TabViewState> openTabs = Collections.emptyList();
	private int activeTab = -1;

	public List<Path> getFiles() {
		return files;
	}

	public void setFiles(List<Path> files) {
		this.files = files;
	}

	public List<String[]> getTreeExpansions() {
		return treeExpansions;
	}

	public void setTreeExpansions(List<String[]> treeExpansions) {
		this.treeExpansions = treeExpansions;
	}

	public JadxCodeData getCodeData() {
		return codeData;
	}

	public void setCodeData(JadxCodeData codeData) {
		this.codeData = codeData;
	}

	public int getProjectVersion() {
		return projectVersion;
	}

	public void setProjectVersion(int projectVersion) {
		this.projectVersion = projectVersion;
	}

	public List<TabViewState> getOpenTabs() {
		return openTabs;
	}

	/**
	 *
	 * @param openTabs
	 * @return <code>true></code> if a change was saved
	 */
	public boolean setOpenTabs(List<TabViewState> openTabs) {
		if (this.openTabs.equals(openTabs)) {
			return false;
		}
		this.openTabs = openTabs;
		return true;
	}

	public int getActiveTab() {
		return activeTab;
	}

	/**
	 *
	 * @param activeTab
	 * @return <code>true></code> if a change was saved
	 */
	public boolean setActiveTab(int activeTab) {
		if (this.activeTab == activeTab) {
			return false;
		}
		this.activeTab = activeTab;
		return true;
	}
}
