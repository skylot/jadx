package jadx.core.codegen.json.cls;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class JsonClass extends JsonNode {
	@SerializedName("package")
	private String pkg;
	private String type; // class, interface, enum
	@SerializedName("extends")
	private String superClass;
	@SerializedName("implements")
	private List<String> interfaces;
	private String dex;

	private List<JsonField> fields;
	private List<JsonMethod> methods;
	private List<JsonClass> innerClasses;

	private List<String> imports;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSuperClass() {
		return superClass;
	}

	public void setSuperClass(String superClass) {
		this.superClass = superClass;
	}

	public List<String> getInterfaces() {
		return interfaces;
	}

	public void setInterfaces(List<String> interfaces) {
		this.interfaces = interfaces;
	}

	public List<JsonField> getFields() {
		return fields;
	}

	public void setFields(List<JsonField> fields) {
		this.fields = fields;
	}

	public List<JsonMethod> getMethods() {
		return methods;
	}

	public void setMethods(List<JsonMethod> methods) {
		this.methods = methods;
	}

	public List<JsonClass> getInnerClasses() {
		return innerClasses;
	}

	public void setInnerClasses(List<JsonClass> innerClasses) {
		this.innerClasses = innerClasses;
	}

	public String getPkg() {
		return pkg;
	}

	public void setPkg(String pkg) {
		this.pkg = pkg;
	}

	public String getDex() {
		return dex;
	}

	public void setDex(String dex) {
		this.dex = dex;
	}

	public List<String> getImports() {
		return imports;
	}

	public void setImports(List<String> imports) {
		this.imports = imports;
	}
}
