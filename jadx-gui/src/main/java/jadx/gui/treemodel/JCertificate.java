package jadx.gui.treemodel;
import jadx.api.ResourceFile;
import jadx.core.utils.files.ZipSecurity;
import jadx.gui.utils.CertificateManager;
import jadx.gui.utils.NLS;
import jadx.gui.utils.Utils;
import javax.swing.*;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JCertificate extends JNode {

	private static final ImageIcon CERTIFICATE_ICON = Utils.openIcon("certificate_obj");
    private final transient ResourceFile rf;

	public JCertificate(ResourceFile resFile) {
        this.rf = resFile;
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

		try {
			ResourceFile.ZipRef zipRef = rf.getZipRef();
			if (zipRef == null) {
				File file = new File(rf.getName());
				try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
					return CertificateManager.decode(inputStream);
				}
			} else {
				try (ZipFile zipFile = new ZipFile(zipRef.getZipFile())) {
					ZipEntry entry = zipFile.getEntry(zipRef.getEntryName());
					if (entry == null) {
						throw new IOException("Zip entry not found: " + zipRef);
					}
					if (!ZipSecurity.isValidZipEntry(entry)) {
						return null;
					}
					try (InputStream inputStream = new BufferedInputStream(zipFile.getInputStream(entry))) {
						return  CertificateManager.decode(inputStream);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		//	throw new JadxException("Error decode: " + rf.getName(), e);
		}

		return null;
	}


}
