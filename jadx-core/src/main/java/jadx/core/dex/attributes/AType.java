package jadx.core.dex.attributes;

import jadx.core.dex.attributes.annotations.AnnotationsList;
import jadx.core.dex.attributes.annotations.MethodParameters;
import jadx.core.dex.attributes.nodes.DeclareVariablesAttr;
import jadx.core.dex.attributes.nodes.EdgeInsnAttr;
import jadx.core.dex.attributes.nodes.EnumClassAttr;
import jadx.core.dex.attributes.nodes.EnumMapAttr;
import jadx.core.dex.attributes.nodes.FieldReplaceAttr;
import jadx.core.dex.attributes.nodes.ForceReturnAttr;
import jadx.core.dex.attributes.nodes.IgnoreEdgeAttr;
import jadx.core.dex.attributes.nodes.JadxErrorAttr;
import jadx.core.dex.attributes.nodes.JumpInfo;
import jadx.core.dex.attributes.nodes.LoopInfo;
import jadx.core.dex.attributes.nodes.LoopLabelAttr;
import jadx.core.dex.attributes.nodes.MethodInlineAttr;
import jadx.core.dex.attributes.nodes.PhiListAttr;
import jadx.core.dex.attributes.nodes.SourceFileAttr;
import jadx.core.dex.nodes.parser.FieldInitAttr;
import jadx.core.dex.trycatch.CatchAttr;
import jadx.core.dex.trycatch.ExcHandlerAttr;
import jadx.core.dex.trycatch.SplitterBlockAttr;

/**
 * Attribute types enumeration,
 * uses generic type for omit cast after 'AttributeStorage.get' method
 *
 * @param <T> attribute class implementation
 */
public class AType<T extends IAttribute> {

	public static final AType<AttrList<JumpInfo>> JUMP = new AType<>();
	public static final AType<AttrList<LoopInfo>> LOOP = new AType<>();
	public static final AType<AttrList<EdgeInsnAttr>> EDGE_INSN = new AType<>();

	public static final AType<ExcHandlerAttr> EXC_HANDLER = new AType<>();
	public static final AType<CatchAttr> CATCH_BLOCK = new AType<>();
	public static final AType<SplitterBlockAttr> SPLITTER_BLOCK = new AType<>();
	public static final AType<ForceReturnAttr> FORCE_RETURN = new AType<>();
	public static final AType<FieldInitAttr> FIELD_INIT = new AType<>();
	public static final AType<FieldReplaceAttr> FIELD_REPLACE = new AType<>();
	public static final AType<JadxErrorAttr> JADX_ERROR = new AType<>();
	public static final AType<MethodInlineAttr> METHOD_INLINE = new AType<>();
	public static final AType<EnumClassAttr> ENUM_CLASS = new AType<>();
	public static final AType<EnumMapAttr> ENUM_MAP = new AType<>();
	public static final AType<AnnotationsList> ANNOTATION_LIST = new AType<>();
	public static final AType<MethodParameters> ANNOTATION_MTH_PARAMETERS = new AType<>();
	public static final AType<PhiListAttr> PHI_LIST = new AType<>();
	public static final AType<SourceFileAttr> SOURCE_FILE = new AType<>();
	public static final AType<DeclareVariablesAttr> DECLARE_VARIABLES = new AType<>();
	public static final AType<LoopLabelAttr> LOOP_LABEL = new AType<>();
	public static final AType<IgnoreEdgeAttr> IGNORE_EDGE = new AType<>();
}
