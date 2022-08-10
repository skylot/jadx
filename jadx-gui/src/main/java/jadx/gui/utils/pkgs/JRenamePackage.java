package jadx.gui.utils.pkgs;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;

import org.apache.commons.lang3.StringUtils;

import jadx.api.JavaNode;
import jadx.api.JavaPackage;
import jadx.api.data.ICodeRename;
import jadx.api.data.impl.JadxCodeRename;
import jadx.api.data.impl.JadxNodeRef;
import jadx.core.deobf.NameMapper;
import jadx.gui.treemodel.JRenameNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.Icons;

import static jadx.core.deobf.NameMapper.VALID_JAVA_IDENTIFIER;

public class JRenamePackage implements JRenameNode {

	private final JavaPackage refPkg;
	private final String rawFullName;
	private final String fullName;
	private final String name;

	public JRenamePackage(JavaPackage refPkg, String rawFullName, String fullName, String name) {
		this.refPkg = refPkg;
		this.rawFullName = rawFullName;
		this.fullName = fullName;
		this.name = name;
	}

	@Override
	public String getTitle() {
		return fullName;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Icon getIcon() {
		return Icons.PACKAGE;
	}

	@Override
	public boolean canRename() {
		return true;
	}

	@Override
	public ICodeRename buildCodeRename(String newName, Set<ICodeRename> renames) {
		return new JadxCodeRename(JadxNodeRef.forPkg(rawFullName), newName);
	}

	@Override
	public boolean isValidName(String newName) {
		return isValidPackageName(newName);
	}

	private static final Pattern PACKAGE_RENAME_PATTERN = Pattern.compile(
			"PKG(\\.PKG)*(\\.)?".replace("PKG", VALID_JAVA_IDENTIFIER.pattern()));

	static boolean isValidPackageName(String newName) {
		if (newName == null || newName.isEmpty() || NameMapper.isReserved(newName)) {
			return false;
		}
		Matcher matcher = PACKAGE_RENAME_PATTERN.matcher(newName);
		if (!matcher.matches()) {
			return false;
		}
		for (String part : StringUtils.split(newName, '.')) {
			if (NameMapper.isReserved(part)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void removeAlias() {
		refPkg.removeAlias();
	}

	@Override
	public void addUpdateNodes(List<JavaNode> toUpdate) {
		refPkg.addUseIn(toUpdate);
	}

	@Override
	public void reload(MainWindow mainWindow) {
		mainWindow.getCacheObject().setPackageHelper(null);
		mainWindow.getTreeRoot().update();
		mainWindow.reloadTree();
	}
}
