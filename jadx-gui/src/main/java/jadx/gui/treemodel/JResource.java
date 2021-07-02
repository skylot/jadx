package jadx.gui.treemodel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeInfo;
import jadx.api.ICodeWriter;
import jadx.api.ResourceFile;
import jadx.api.ResourceFileContent;
import jadx.api.ResourceType;
import jadx.api.ResourcesLoader;
import jadx.api.impl.SimpleCodeInfo;
import jadx.core.utils.Utils;
import jadx.core.xmlgen.ResContainer;
import jadx.gui.ui.ContentPanel;
import jadx.gui.ui.ImagePanel;
import jadx.gui.ui.TabbedPane;
import jadx.gui.ui.codearea.CodeContentPanel;
import jadx.gui.utils.NLS;
import jadx.gui.utils.OverlayIcon;
import jadx.gui.utils.UiUtils;

public class JResource extends JLoadableNode implements Comparable<JResource> {
	private static final long serialVersionUID = -201018424302612434L;

	private static final ImageIcon ROOT_ICON = UiUtils.openIcon("cf_obj");
	private static final ImageIcon FOLDER_ICON = UiUtils.openIcon("folder");
	private static final ImageIcon FILE_ICON = UiUtils.openIcon("file_obj");
	private static final ImageIcon MANIFEST_ICON = UiUtils.openIcon("template_obj");
	private static final ImageIcon JAVA_ICON = UiUtils.openIcon("java_ovr");
	private static final ImageIcon ERROR_ICON = UiUtils.openIcon("error_co");

	public enum JResType {
		ROOT,
		DIR,
		FILE
	}

	private final transient String name;
	private final transient String shortName;
	private final transient List<JResource> files = new ArrayList<>(1);
	private final transient JResType type;
	private final transient ResourceFile resFile;

	private transient boolean loaded;
	private transient ICodeInfo content;

	public JResource(ResourceFile resFile, String name, JResType type) {
		this(resFile, name, name, type);
	}

	public JResource(ResourceFile resFile, String name, String shortName, JResType type) {
		this.resFile = resFile;
		this.name = name;
		this.shortName = shortName;
		this.type = type;
		this.loaded = false;
	}

	public final void update() {
		if (files.isEmpty()) {
			if (type == JResType.DIR || type == JResType.ROOT
					|| resFile.getType() == ResourceType.ARSC) {
				// fake leaf to force show expand button
				// real sub nodes will load on expand in loadNode() method
				add(new TextNode(NLS.str("tree.loading")));
			}
		} else {
			removeAllChildren();

			Comparator<JResource> typeComparator = Comparator.comparingInt(r -> r.type.ordinal());
			Comparator<JResource> nameComparator = Comparator.comparing(JResource::getName, String.CASE_INSENSITIVE_ORDER);

			files.sort(typeComparator.thenComparing(nameComparator));

			for (JResource res : files) {
				res.update();
				add(res);
			}
		}
	}

	@Override
	public void loadNode() {
		getContent();
		update();
	}

	@Override
	public String getName() {
		return name;
	}

	public List<JResource> getFiles() {
		return files;
	}

	@Override
	public @Nullable ICodeInfo getCodeInfo() {
		getContent();
		return content;
	}

	@Override
	public @Nullable ContentPanel getContentPanel(TabbedPane tabbedPane) {
		if (resFile == null) {
			return null;
		}
		if (resFile.getType() == ResourceType.IMG) {
			return new ImagePanel(tabbedPane, this);
		}
		return new CodeContentPanel(tabbedPane, this);
	}

	@Override
	public synchronized String getContent() {
		if (loaded) {
			if (content == null) {
				return null;
			}
			return content.getCodeStr();
		}
		if (resFile == null || type != JResType.FILE) {
			return null;
		}
		if (!isSupportedForView(resFile.getType())) {
			return null;
		}
		ResContainer rc = resFile.loadContent();
		if (rc == null) {
			loaded = true;
			return null;
		}
		if (rc.getDataType() == ResContainer.DataType.RES_TABLE) {
			content = loadCurrentSingleRes(rc);
			for (ResContainer subFile : rc.getSubFiles()) {
				loadSubNodes(this, subFile, 1);
			}
		} else {
			// single node
			content = loadCurrentSingleRes(rc);
		}
		loaded = true;
		return content.getCodeStr();
	}

	private ICodeInfo loadCurrentSingleRes(ResContainer rc) {
		switch (rc.getDataType()) {
			case TEXT:
			case RES_TABLE:
				return rc.getText();

			case RES_LINK:
				try {
					return ResourcesLoader.decodeStream(rc.getResLink(), (size, is) -> {
						if (size > 10 * 1024 * 1024L) {
							return new SimpleCodeInfo("File too large for view");
						}
						return ResourcesLoader.loadToCodeWriter(is);
					});
				} catch (Exception e) {
					return new SimpleCodeInfo("Failed to load resource file:" + ICodeWriter.NL + Utils.getStackTrace(e));
				}

			case DECODED_DATA:
			default:
				return new SimpleCodeInfo("Unexpected resource type: " + rc);
		}
	}

	private void loadSubNodes(JResource root, ResContainer rc, int depth) {
		String resName = rc.getName();
		String[] path = resName.split("/");
		String resShortName = path.length == 0 ? resName : path[path.length - 1];
		ICodeInfo code = rc.getText();
		ResourceFileContent fileContent = new ResourceFileContent(resShortName, ResourceType.XML, code);
		addPath(path, root, new JResource(fileContent, resName, resShortName, JResType.FILE));

		for (ResContainer subFile : rc.getSubFiles()) {
			loadSubNodes(root, subFile, depth + 1);
		}
	}

	private static void addPath(String[] path, JResource root, JResource jResource) {
		if (path.length == 1) {
			root.getFiles().add(jResource);
			return;
		}
		JResource currentRoot = root;
		int last = path.length - 1;
		for (int i = 0; i <= last; i++) {
			String f = path[i];
			if (i == last) {
				currentRoot.getFiles().add(jResource);
			} else {
				currentRoot = getResDir(currentRoot, f);
			}
		}
	}

	private static JResource getResDir(JResource root, String dirName) {
		for (JResource file : root.getFiles()) {
			if (file.getName().equals(dirName)) {
				return file;
			}
		}
		JResource resDir = new JResource(null, dirName, JResType.DIR);
		root.getFiles().add(resDir);
		return resDir;
	}

	@Override
	public String getSyntaxName() {
		if (resFile == null) {
			return null;
		}
		switch (resFile.getType()) {
			case CODE:
				return super.getSyntaxName();

			case MANIFEST:
			case XML:
				return SyntaxConstants.SYNTAX_STYLE_XML;

			default:
				String syntax = getSyntaxByExtension(resFile.getDeobfName());
				if (syntax != null) {
					return syntax;
				}
				return super.getSyntaxName();
		}
	}

	private static final Map<String, String> EXTENSION_TO_FILE_SYNTAX = jadx.core.utils.Utils.newConstStringMap(
			"java", SyntaxConstants.SYNTAX_STYLE_JAVA,
			"js", SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT,
			"ts", SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT,
			"json", SyntaxConstants.SYNTAX_STYLE_JSON,
			"css", SyntaxConstants.SYNTAX_STYLE_CSS,
			"less", SyntaxConstants.SYNTAX_STYLE_LESS,
			"html", SyntaxConstants.SYNTAX_STYLE_HTML,
			"xml", SyntaxConstants.SYNTAX_STYLE_XML,
			"yaml", SyntaxConstants.SYNTAX_STYLE_YAML,
			"properties", SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE,
			"ini", SyntaxConstants.SYNTAX_STYLE_INI,
			"sql", SyntaxConstants.SYNTAX_STYLE_SQL,
			"arsc", SyntaxConstants.SYNTAX_STYLE_XML);

	private String getSyntaxByExtension(String name) {
		int dot = name.lastIndexOf('.');
		if (dot == -1) {
			return null;
		}
		String ext = name.substring(dot + 1);
		return EXTENSION_TO_FILE_SYNTAX.get(ext);
	}

	@Override
	public Icon getIcon() {
		switch (type) {
			case ROOT:
				return ROOT_ICON;
			case DIR:
				return FOLDER_ICON;

			case FILE:
				ResourceType resType = resFile.getType();
				if (resType == ResourceType.MANIFEST) {
					return MANIFEST_ICON;
				}
				if (resType == ResourceType.CODE) {
					return new OverlayIcon(FILE_ICON, ERROR_ICON, JAVA_ICON);
				}
				if (!isSupportedForView(resType)) {
					return new OverlayIcon(FILE_ICON, ERROR_ICON);
				}
				return FILE_ICON;
		}
		return FILE_ICON;
	}

	public static boolean isSupportedForView(ResourceType type) {
		switch (type) {
			case CODE:
			case FONT:
			case LIB:
			case MEDIA:
				return false;

			case MANIFEST:
			case XML:
			case ARSC:
			case IMG:
			case UNKNOWN:
				return true;
		}
		return true;
	}

	public ResourceFile getResFile() {
		return resFile;
	}

	@Override
	public JClass getJParent() {
		return null;
	}

	@Override
	public int compareTo(@NotNull JResource o) {
		return name.compareTo(o.name);
	}

	@Override
	public String makeString() {
		return shortName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		return name.equals(((JResource) o).name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
