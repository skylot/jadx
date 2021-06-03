package jadx.core.dex.attributes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jadx.core.dex.attributes.annotations.AnnotationsList;
import jadx.core.dex.attributes.annotations.MethodParameters;
import jadx.core.dex.attributes.fldinit.FieldInitAttr;
import jadx.core.dex.attributes.nodes.ClassTypeVarsAttr;
import jadx.core.dex.attributes.nodes.DeclareVariablesAttr;
import jadx.core.dex.attributes.nodes.EdgeInsnAttr;
import jadx.core.dex.attributes.nodes.EnumClassAttr;
import jadx.core.dex.attributes.nodes.EnumMapAttr;
import jadx.core.dex.attributes.nodes.FieldReplaceAttr;
import jadx.core.dex.attributes.nodes.ForceReturnAttr;
import jadx.core.dex.attributes.nodes.GenericInfoAttr;
import jadx.core.dex.attributes.nodes.IgnoreEdgeAttr;
import jadx.core.dex.attributes.nodes.JadxError;
import jadx.core.dex.attributes.nodes.JumpInfo;
import jadx.core.dex.attributes.nodes.LocalVarsDebugInfoAttr;
import jadx.core.dex.attributes.nodes.LoopInfo;
import jadx.core.dex.attributes.nodes.LoopLabelAttr;
import jadx.core.dex.attributes.nodes.MethodInlineAttr;
import jadx.core.dex.attributes.nodes.MethodOverrideAttr;
import jadx.core.dex.attributes.nodes.MethodTypeVarsAttr;
import jadx.core.dex.attributes.nodes.PhiListAttr;
import jadx.core.dex.attributes.nodes.RegDebugInfoAttr;
import jadx.core.dex.attributes.nodes.RenameReasonAttr;
import jadx.core.dex.attributes.nodes.SkipMethodArgsAttr;
import jadx.core.dex.attributes.nodes.SourceFileAttr;
import jadx.core.dex.nodes.IMethodDetails;
import jadx.core.dex.trycatch.CatchAttr;
import jadx.core.dex.trycatch.ExcHandlerAttr;
import jadx.core.dex.trycatch.SplitterBlockAttr;

/**
 * Attribute types enumeration,
 * uses generic type for omit cast after 'AttributeStorage.get' method
 *
 * @param <T> attribute class implementation
 */
@SuppressWarnings("InstantiationOfUtilityClass")
public class AType<T extends IAttribute> {

	// class, method, field, insn
	public static final AType<AttrList<String>> CODE_COMMENTS = new AType<>();

	// class, method, field
	public static final AType<AnnotationsList> ANNOTATION_LIST = new AType<>();
	public static final AType<RenameReasonAttr> RENAME_REASON = new AType<>();

	// class, method
	public static final AType<AttrList<JadxError>> JADX_ERROR = new AType<>(); // code failed to decompile completely
	public static final AType<AttrList<String>> JADX_WARN = new AType<>(); // mark code as inconsistent (code can be viewed)
	public static final AType<AttrList<String>> COMMENTS = new AType<>(); // any additional info about decompilation

	// class
	public static final AType<SourceFileAttr> SOURCE_FILE = new AType<>();
	public static final AType<EnumClassAttr> ENUM_CLASS = new AType<>();
	public static final AType<EnumMapAttr> ENUM_MAP = new AType<>();
	public static final AType<ClassTypeVarsAttr> CLASS_TYPE_VARS = new AType<>();

	// field
	public static final AType<FieldInitAttr> FIELD_INIT = new AType<>();
	public static final AType<FieldReplaceAttr> FIELD_REPLACE = new AType<>();

	// method
	public static final AType<LocalVarsDebugInfoAttr> LOCAL_VARS_DEBUG_INFO = new AType<>();
	public static final AType<MethodInlineAttr> METHOD_INLINE = new AType<>();
	public static final AType<MethodParameters> ANNOTATION_MTH_PARAMETERS = new AType<>();
	public static final AType<SkipMethodArgsAttr> SKIP_MTH_ARGS = new AType<>();
	public static final AType<MethodOverrideAttr> METHOD_OVERRIDE = new AType<>();
	public static final AType<MethodTypeVarsAttr> METHOD_TYPE_VARS = new AType<>();

	// region
	public static final AType<DeclareVariablesAttr> DECLARE_VARIABLES = new AType<>();

	// block
	public static final AType<PhiListAttr> PHI_LIST = new AType<>();
	public static final AType<IgnoreEdgeAttr> IGNORE_EDGE = new AType<>();
	public static final AType<ForceReturnAttr> FORCE_RETURN = new AType<>();
	public static final AType<CatchAttr> CATCH_BLOCK = new AType<>();
	public static final AType<SplitterBlockAttr> SPLITTER_BLOCK = new AType<>();
	public static final AType<AttrList<LoopInfo>> LOOP = new AType<>();
	public static final AType<AttrList<EdgeInsnAttr>> EDGE_INSN = new AType<>();

	// block or insn
	public static final AType<ExcHandlerAttr> EXC_HANDLER = new AType<>();

	// instruction
	public static final AType<LoopLabelAttr> LOOP_LABEL = new AType<>();
	public static final AType<AttrList<JumpInfo>> JUMP = new AType<>();
	public static final AType<IMethodDetails> METHOD_DETAILS = new AType<>();
	public static final AType<GenericInfoAttr> GENERIC_INFO = new AType<>();

	// register
	public static final AType<RegDebugInfoAttr> REG_DEBUG_INFO = new AType<>();

	public static final Set<AType<?>> SKIP_ON_UNLOAD = new HashSet<>(Arrays.asList(
			SOURCE_FILE,
			ANNOTATION_LIST,
			ANNOTATION_MTH_PARAMETERS,
			FIELD_INIT,
			FIELD_REPLACE,
			METHOD_INLINE,
			METHOD_OVERRIDE,
			SKIP_MTH_ARGS));
}
