package jadx.gui.treemodel;

import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.core.codegen.CodeWriter;
import jadx.gui.utils.OverlayIcon;
import jadx.gui.utils.Utils;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

public class JResource extends JNode implements Comparable<JResource> {
	private static final long serialVersionUID = -201018424302612434L;

	private static final ImageIcon ROOT_ICON = Utils.openIcon("cf_obj");
	private static final ImageIcon FOLDER_ICON = Utils.openIcon("folder");
	private static final ImageIcon FILE_ICON = Utils.openIcon("file_obj");
	private static final ImageIcon MANIFEST_ICON = Utils.openIcon("template_obj");
	private static final ImageIcon JAVA_ICON = Utils.openIcon("java_ovr");
	private static final ImageIcon ERROR_ICON = Utils.openIcon("error_co");

	public enum JResType {
		ROOT,
		DIR,
		FILE
	}

	private final String name;
	private final List<JResource> files = new ArrayList<JResource>(1);
	private final JResType type;
	private final ResourceFile resFile;

	private boolean loaded;
	private String content;
	private Map<Integer, Integer> lineMapping;

	public JResource(ResourceFile resFile, String name, JResType type) {
		this.resFile = resFile;
		this.name = name;
		this.type = type;
	}

	public final void update() {
		removeAllChildren();
		for (JResource res : files) {
			res.update();
			add(res);
		}
	}

	public String getName() {
		return name;
	}

	public List<JResource> getFiles() {
		return files;
	}

	public String getContent() {
		if (!loaded && resFile != null && type == JResType.FILE) {
			loaded = true;
			if (isSupportedForView(resFile.getType())) {
				CodeWriter cw = resFile.getContent();
				if (cw != null) {
					lineMapping = cw.getLineMapping();
					content = cw.toString();
				}
			}
		}
		return content;
	}

	@Override
	public Integer getSourceLine(int line) {
		if (lineMapping == null) {
			return null;
		}
		return lineMapping.get(line);
	}

	@Override
	public String getSyntaxName() {
		switch (resFile.getType()) {
			case CODE:
				return super.getSyntaxName();
			case MANIFEST:
			case XML:
				return SyntaxConstants.SYNTAX_STYLE_XML;
		}
		String syntax = getSyntaxByExtension(resFile.getName());
		if (syntax != null) {
			return syntax;
		}
		return super.getSyntaxName();
	}

	private String getSyntaxByExtension(String name) {
		int dot = name.lastIndexOf('.');
		if (dot == -1) {
			return null;
		}
		String ext = name.substring(dot + 1);
		if (ext.equals("js")) {
			return SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT;
		}
		if (ext.equals("css")) {
			return SyntaxConstants.SYNTAX_STYLE_CSS;
		}
		if (ext.equals("html")) {
			return SyntaxConstants.SYNTAX_STYLE_HTML;
		}
		return null;
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

	private boolean isSupportedForView(ResourceType type) {
		switch (type) {
			case CODE:
			case FONT:
			case IMG:
			case LIB:
				return false;

			case MANIFEST:
			case XML:
			case ARSC:
			case UNKNOWN:
				return true;
		}
		return true;
	}

	@Override
	public JClass getJParent() {
		return null;
	}

	@Override
	public int compareTo(JResource o) {
		return name.compareTo(o.name);
	}

	@Override
	public String makeString() {
		return name;
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
