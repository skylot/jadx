package jadx.core.xmlgen;

import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.deobf.NameMapper;
import jadx.core.utils.Utils;

public class XmlSecurity {
	private static final Logger LOG = LoggerFactory.getLogger(XmlSecurity.class);

	private static final boolean DISABLE_CHECKS = Utils.getEnvVarBool("JADX_DISABLE_XML_SECURITY", false);

	private static final DocumentBuilderFactory DBF_INSTANCE = buildDBF();

	private XmlSecurity() {
	}

	public static DocumentBuilderFactory getDBF() {
		return DBF_INSTANCE;
	}

	public static String verifyAppPackage(String appPackage) {
		if (DISABLE_CHECKS) {
			return appPackage;
		}
		if (NameMapper.isValidFullIdentifier(appPackage)) {
			return appPackage;
		}
		LOG.warn("App package '{}' has invalid format and will be ignored", appPackage);
		return "INVALID_PACKAGE";
	}

	private static DocumentBuilderFactory buildDBF() {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		if (DISABLE_CHECKS) {
			return dbf;
		}
		try {
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
