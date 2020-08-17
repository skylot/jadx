package jadx.core.dex.nodes;

import java.util.Collections;
import java.util.List;

import jadx.api.plugins.input.data.IFieldData;
import jadx.core.dex.attributes.annotations.AnnotationsList;
import jadx.core.dex.attributes.nodes.LineAttrNode;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.AccessInfo.AFType;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.args.ArgType;

public class FieldNode extends LineAttrNode implements ICodeNode {

	private final ClassNode parentClass;
	private final FieldInfo fieldInfo;
	private AccessInfo accFlags;

	private ArgType type;

	private List<MethodNode> useIn = Collections.emptyList();

	public static FieldNode build(ClassNode cls, IFieldData fieldData) {
		FieldInfo fieldInfo = FieldInfo.fromData(cls.root(), fieldData);
		FieldNode fieldNode = new FieldNode(cls, fieldInfo, fieldData.getAccessFlags());
		AnnotationsList.attach(fieldNode, fieldData.getAnnotations());
		return fieldNode;
	}

	public FieldNode(ClassNode cls, FieldInfo fieldInfo, int accessFlags) {
		this.parentClass = cls;
		this.fieldInfo = fieldInfo;
		this.type = fieldInfo.getType();
		this.accFlags = new AccessInfo(accessFlags, AFType.FIELD);
	}

	public void updateType(ArgType type) {
		this.type = type;
	}

	public FieldInfo getFieldInfo() {
		return fieldInfo;
	}

	@Override
	public AccessInfo getAccessFlags() {
		return accFlags;
	}

	@Override
	public void setAccessFlags(AccessInfo accFlags) {
		this.accFlags = accFlags;
	}

	public boolean isStatic() {
		return accFlags.isStatic();
	}

	public String getName() {
		return fieldInfo.getName();
	}

	public String getAlias() {
		return fieldInfo.getAlias();
	}

	public ArgType getType() {
		return type;
	}

	public ClassNode getParentClass() {
		return parentClass;
	}

	public List<MethodNode> getUseIn() {
		return useIn;
	}

	public void setUseIn(List<MethodNode> useIn) {
		this.useIn = useIn;
	}

	@Override
	public String typeName() {
		return "field";
	}

	@Override
	public String getInputFileName() {
		return parentClass.getInputFileName();
	}

	@Override
	public RootNode root() {
		return parentClass.root();
	}

	@Override
	public int hashCode() {
		return fieldInfo.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		FieldNode other = (FieldNode) obj;
		return fieldInfo.equals(other.fieldInfo);
	}

	@Override
	public String toString() {
		return fieldInfo.getDeclClass() + "." + fieldInfo.getName() + " :" + type;
	}
}
