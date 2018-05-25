package jadx.gui.treemodel;
import jadx.gui.utils.NLS;
import jadx.gui.utils.Utils;
import javax.swing.*;

public class JCertificate extends JNode {

	private static final ImageIcon CERTIFICATE_ICON = Utils.openIcon("certificate_obj");

	public JCertificate() {

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


}
