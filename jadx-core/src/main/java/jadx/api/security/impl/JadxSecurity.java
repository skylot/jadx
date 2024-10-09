package jadx.api.security.impl;

import java.io.InputStream;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import jadx.api.security.IJadxSecurity;
import jadx.api.security.JadxSecurityFlag;
import jadx.core.deobf.NameMapper;

public class JadxSecurity implements IJadxSecurity {
	private static final Logger LOG = LoggerFactory.getLogger(JadxSecurity.class);

	private final Set<JadxSecurityFlag> flags;

	public JadxSecurity(Set<JadxSecurityFlag> flags) {
		this.flags = flags;
	}

	@Override
	public String verifyAppPackage(String appPackage) {
		if (flags.contains(JadxSecurityFlag.VERIFY_APP_PACKAGE)
				&& !NameMapper.isValidFullIdentifier(appPackage)) {
			LOG.warn("App package '{}' has invalid format and will be ignored", appPackage);
			return "INVALID_PACKAGE";
		}
		return appPackage;
	}

	@Override
	public Document parseXml(InputStream in) {
		DocumentBuilderFactory dbf;
		if (flags.contains(JadxSecurityFlag.SECURE_XML_PARSER)) {
			dbf = SecureDBFHolder.INSTANCE;
		} else {
			dbf = SimpleDBFHolder.INSTANCE;
		}
		try {
			return dbf.newDocumentBuilder().parse(in);
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse xml", e);
		}
	}

	private static final class SimpleDBFHolder {
		private static final DocumentBuilderFactory INSTANCE = DocumentBuilderFactory.newInstance();
	}

	private static final class SecureDBFHolder {
		private static final DocumentBuilderFactory INSTANCE = buildSecureDBF();

		private static DocumentBuilderFactory buildSecureDBF() {
			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
				dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
				dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
				dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
				dbf.setFeature("http://apache.org/xml/features/dom/create-entity-ref-nodes", false);
				dbf.setXIncludeAware(false);
				dbf.setExpandEntityReferences(false);
				return dbf;
			} catch (Exception e) {
				throw new RuntimeException("Fail to build secure XML DocumentBuilderFactory", e);
			}
		}
	}
}
