package jadx.core.xmlgen;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class XmlSecurity {

	private static DocumentBuilderFactory secureDbf = null;

	private XmlSecurity() {}

	public static DocumentBuilderFactory getSecureDbf() throws ParserConfigurationException {
		synchronized (XmlSecurity.class) {
			if (secureDbf == null) {
				secureDbf = DocumentBuilderFactory.newInstance();
				secureDbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
				secureDbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
				secureDbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
				secureDbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
				secureDbf.setFeature("http://apache.org/xml/features/dom/create-entity-ref-nodes", false);
				secureDbf.setXIncludeAware(false);
				secureDbf.setExpandEntityReferences(false);
			}
		}
		return secureDbf;
	}
}
