package jadx.core.xmlgen;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jadx.core.codegen.CodeWriter;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;

/*
 * modifies android:name attributes and xml tags which are old class names
 * but were changed during deobfuscation
 */
public class XmlDeobf {
	private static final Map<String, String> deobfMap = new HashMap<>();
	
	private XmlDeobf() {}
	
	public static void deobfXmlDocument(RootNode rootNode, ResContainer resContainer) {
		CodeWriter codeWriter = resContainer.getContent();
		Document document;
		try {
			InputStream sourceInputStream = new ByteArrayInputStream(codeWriter.toString()
					.getBytes());
			document = XmlSecurity.getSecureDbf()
					.newDocumentBuilder()
					.parse(sourceInputStream);
		}
		catch(Exception e) {
			return;
		}
		
		Element rootElement = document.getDocumentElement();
		String packageName = rootElement.hasAttribute("package") ?
				rootElement.getAttribute("package") : null;
		
		updateDocumentElement(rootNode, rootElement, packageName);
		String documentData = documentToString(document);
		if(documentData != null) {
			codeWriter.updateContent(documentData);
		}
		
		for(ResContainer subFile : resContainer.getSubFiles()) {
			deobfXmlDocument(rootNode, subFile);
		}
	}
	
	private static String documentToString(Document document) {
		try {
			TransformerFactory transFactory = TransformerFactory.newInstance();
			Transformer transformer = transFactory.newTransformer();
			StringWriter buffer = new StringWriter();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.transform(new DOMSource(document), new StreamResult(buffer));
			return buffer.toString();
		}
		catch(Exception e) {
			return null;
		}
	}
	
	private static void updateDocumentElement(RootNode rootNode,
			Element e, String packageName) {

		if(e.hasAttribute("android:name")) {
			String oldClassName = e.getAttribute("android:name");
			if(packageName != null && oldClassName.startsWith(".")) {
				oldClassName = packageName + oldClassName;
			}
			String newClassName = getNewClassName(rootNode, oldClassName);
			if(newClassName != null) {
				e.setAttribute("android:name", newClassName);
			}
		}

		NodeList list = e.getChildNodes();
		for(int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if(node == null || node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element element = (Element) node;
			updateDocumentElement(rootNode, element, packageName);
		}
	}
	
	private static String getNewClassName(RootNode rootNode, String old) {
		if(deobfMap.isEmpty()) {
			for(ClassNode classNode : rootNode.getClasses(true)) {
				if(classNode.getAlias() != null) {
					String oldName = classNode.getClassInfo().getFullName();
					String newName = classNode.getAlias().getFullName();
					if(!oldName.equals(newName)) {
						deobfMap.put(oldName, newName);
					}
				}
			}
		}
		return deobfMap.get(old);
	}
}
