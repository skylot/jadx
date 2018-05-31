package jadx.gui.treemodel;
import jadx.api.ResourceFile;

import jadx.gui.utils.NLS;
import jadx.gui.utils.Utils;
import javax.swing.*;

public class JCertificate extends JNode {

	private static final ImageIcon CERTIFICATE_ICON = Utils.openIcon("certificate_obj");
    private final transient ResourceFile resFile;

	public JCertificate(ResourceFile resFile) {
        this.resFile = resFile;
	}

	@Override
	public JClass getJParent() {
		return null;
	}

	@Override
	public Icon getIcon() {
		return CERTIFICATE_ICON;
	}

	@Override
	public String makeString() {
		return NLS.str("certificate.title");
	}

	@Override
	public String getContent() {
		/*
		if (!loaded && resFile != null && type == JResource.JResType.FILE) {
			loaded = true;
			if (isSupportedForView(resFile.getType())) {
				ResContainer rc = resFile.loadContent();
				if (rc != null) {
					addSubFiles(rc, this, 0);
				}
			}
		}
		return content;*/
		return "test";
	}


}
