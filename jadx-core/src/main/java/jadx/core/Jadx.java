package jadx.core;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.CommentsLevel;
import jadx.api.JadxArgs;
import jadx.core.deobf.DeobfuscatorVisitor;
import jadx.core.deobf.SaveDeobfMapping;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.visitors.AnonymousClassVisitor;
import jadx.core.dex.visitors.AttachCommentsVisitor;
import jadx.core.dex.visitors.AttachMethodDetails;
import jadx.core.dex.visitors.AttachTryCatchVisitor;
import jadx.core.dex.visitors.CheckCode;
import jadx.core.dex.visitors.ClassModifier;
import jadx.core.dex.visitors.ConstInlineVisitor;
import jadx.core.dex.visitors.ConstructorVisitor;
import jadx.core.dex.visitors.DeboxingVisitor;
import jadx.core.dex.visitors.DotGraphVisitor;
import jadx.core.dex.visitors.EnumVisitor;
import jadx.core.dex.visitors.ExtractFieldInit;
import jadx.core.dex.visitors.FallbackModeVisitor;
import jadx.core.dex.visitors.FixAccessModifiers;
import jadx.core.dex.visitors.GenericTypesVisitor;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.dex.visitors.InitCodeVariables;
import jadx.core.dex.visitors.InlineMethods;
import jadx.core.dex.visitors.MarkMethodsForInline;
import jadx.core.dex.visitors.MethodInvokeVisitor;
import jadx.core.dex.visitors.MethodVisitor;
import jadx.core.dex.visitors.ModVisitor;
import jadx.core.dex.visitors.MoveInlineVisitor;
import jadx.core.dex.visitors.OverrideMethodVisitor;
import jadx.core.dex.visitors.PrepareForCodeGen;
import jadx.core.dex.visitors.ProcessAnonymous;
import jadx.core.dex.visitors.ProcessInstructionsVisitor;
import jadx.core.dex.visitors.ProcessMethodsForInline;
import jadx.core.dex.visitors.ReSugarCode;
import jadx.core.dex.visitors.ShadowFieldVisitor;
import jadx.core.dex.visitors.SignatureProcessor;
import jadx.core.dex.visitors.SimplifyVisitor;
import jadx.core.dex.visitors.blocks.BlockProcessor;
import jadx.core.dex.visitors.blocks.BlockSplitter;
import jadx.core.dex.visitors.debuginfo.DebugInfoApplyVisitor;
import jadx.core.dex.visitors.debuginfo.DebugInfoAttachVisitor;
import jadx.core.dex.visitors.finaly.MarkFinallyVisitor;
import jadx.core.dex.visitors.kotlin.ProcessKotlinInternals;
import jadx.core.dex.visitors.regions.CheckRegions;
import jadx.core.dex.visitors.regions.CleanRegions;
import jadx.core.dex.visitors.regions.IfRegionVisitor;
import jadx.core.dex.visitors.regions.LoopRegionVisitor;
import jadx.core.dex.visitors.regions.RegionMakerVisitor;
import jadx.core.dex.visitors.regions.ReturnVisitor;
import jadx.core.dex.visitors.regions.variables.ProcessVariables;
import jadx.core.dex.visitors.rename.CodeRenameVisitor;
import jadx.core.dex.visitors.rename.RenameVisitor;
import jadx.core.dex.visitors.shrink.CodeShrinkVisitor;
import jadx.core.dex.visitors.ssa.SSATransform;
import jadx.core.dex.visitors.typeinference.FinishTypeInference;
import jadx.core.dex.visitors.typeinference.TypeInferenceVisitor;
import jadx.core.dex.visitors.usage.UsageInfoVisitor;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class Jadx {
	private static final Logger LOG = LoggerFactory.getLogger(Jadx.class);

	private Jadx() {
	}

	public static List<IDexTreeVisitor> getPassesList(JadxArgs args) {
		switch (args.getDecompilationMode()) {
			case AUTO:
			case RESTRUCTURE:
				return getRegionsModePasses(args);
			case SIMPLE:
				return getSimpleModePasses(args);
			case FALLBACK:
				return getFallbackPassesList();
			default:
				throw new JadxRuntimeException("Unknown decompilation mode: " + args.getDecompilationMode());
		}
	}

	public static List<IDexTreeVisitor> getPreDecompilePassesList() {
		List<IDexTreeVisitor> passes = new ArrayList<>();
		passes.add(new SignatureProcessor());
		passes.add(new OverrideMethodVisitor());

		// rename and deobfuscation
		passes.add(new DeobfuscatorVisitor());
		passes.add(new RenameVisitor());
		passes.add(new SaveDeobfMapping());

		passes.add(new UsageInfoVisitor());
		passes.add(new ProcessAnonymous());
		passes.add(new ProcessMethodsForInline());
		return passes;
	}

	public static List<IDexTreeVisitor> getRegionsModePasses(JadxArgs args) {
		List<IDexTreeVisitor> passes = new ArrayList<>();
		// instructions IR
		passes.add(new CheckCode());
		if (args.isDebugInfo()) {
			passes.add(new DebugInfoAttachVisitor());
		}
		passes.add(new AttachTryCatchVisitor());
		if (args.getCommentsLevel() != CommentsLevel.NONE) {
			passes.add(new AttachCommentsVisitor());
		}
		passes.add(new AttachMethodDetails());
		passes.add(new ProcessInstructionsVisitor());

		// blocks IR
		passes.add(new BlockSplitter());
		passes.add(new BlockProcessor());
		if (args.isRawCFGOutput()) {
			passes.add(DotGraphVisitor.dumpRaw());
		}

		passes.add(new SSATransform());
		passes.add(new MoveInlineVisitor());
		passes.add(new ConstructorVisitor());
		passes.add(new InitCodeVariables());
		if (args.isExtractFinally()) {
			passes.add(new MarkFinallyVisitor());
		}
		passes.add(new ConstInlineVisitor());
		passes.add(new TypeInferenceVisitor());
		if (args.isDebugInfo()) {
			passes.add(new DebugInfoApplyVisitor());
		}
		passes.add(new FinishTypeInference());
		if (args.getUseKotlinMethodsForVarNames() != JadxArgs.UseKotlinMethodsForVarNames.DISABLE) {
			passes.add(new ProcessKotlinInternals());
		}
		passes.add(new CodeRenameVisitor());
		if (args.isInlineMethods()) {
			passes.add(new InlineMethods());
		}
		passes.add(new GenericTypesVisitor());
		passes.add(new ShadowFieldVisitor());
		passes.add(new DeboxingVisitor());
		passes.add(new AnonymousClassVisitor());
		passes.add(new ModVisitor());
		passes.add(new CodeShrinkVisitor());
		passes.add(new ReSugarCode());
		if (args.isCfgOutput()) {
			passes.add(DotGraphVisitor.dump());
		}

		// regions IR
		passes.add(new RegionMakerVisitor());
		passes.add(new IfRegionVisitor());
		passes.add(new ReturnVisitor());
		passes.add(new CleanRegions());

		passes.add(new CodeShrinkVisitor());
		passes.add(new MethodInvokeVisitor());
		passes.add(new SimplifyVisitor());
		passes.add(new CheckRegions());

		passes.add(new EnumVisitor());
		passes.add(new ExtractFieldInit());
		passes.add(new FixAccessModifiers());
		passes.add(new ClassModifier());
		passes.add(new LoopRegionVisitor());

		if (args.isInlineMethods()) {
			passes.add(new MarkMethodsForInline());
		}
		passes.add(new ProcessVariables());
		passes.add(new PrepareForCodeGen());
		if (args.isCfgOutput()) {
			passes.add(DotGraphVisitor.dumpRegions());
		}
		return passes;
	}

	public static List<IDexTreeVisitor> getSimpleModePasses(JadxArgs args) {
		List<IDexTreeVisitor> passes = new ArrayList<>();
		if (args.isDebugInfo()) {
			passes.add(new DebugInfoAttachVisitor());
		}
		passes.add(new AttachTryCatchVisitor());
		if (args.getCommentsLevel() != CommentsLevel.NONE) {
			passes.add(new AttachCommentsVisitor());
		}
		passes.add(new AttachMethodDetails());
		passes.add(new ProcessInstructionsVisitor());

		passes.add(new BlockSplitter());
		if (args.isRawCFGOutput()) {
			passes.add(DotGraphVisitor.dumpRaw());
		}
		passes.add(new MethodVisitor(mth -> mth.add(AFlag.DISABLE_BLOCKS_LOCK)));
		passes.add(new BlockProcessor());
		passes.add(new SSATransform());
		passes.add(new MoveInlineVisitor());
		passes.add(new ConstructorVisitor());
		passes.add(new InitCodeVariables());
		passes.add(new ConstInlineVisitor());
		passes.add(new TypeInferenceVisitor());
		if (args.isDebugInfo()) {
			passes.add(new DebugInfoApplyVisitor());
		}
		passes.add(new FinishTypeInference());
		passes.add(new CodeRenameVisitor());
		passes.add(new DeboxingVisitor());
		passes.add(new ModVisitor());
		passes.add(new CodeShrinkVisitor());
		passes.add(new ReSugarCode());
		passes.add(new CodeShrinkVisitor());
		passes.add(new SimplifyVisitor());
		passes.add(new MethodVisitor(mth -> mth.remove(AFlag.DONT_GENERATE)));
		if (args.isCfgOutput()) {
			passes.add(DotGraphVisitor.dump());
		}
		return passes;
	}

	public static List<IDexTreeVisitor> getFallbackPassesList() {
		List<IDexTreeVisitor> passes = new ArrayList<>();
		passes.add(new AttachTryCatchVisitor());
		passes.add(new AttachCommentsVisitor());
		passes.add(new ProcessInstructionsVisitor());
		passes.add(new FallbackModeVisitor());
		return passes;
	}

	public static final String VERSION_DEV = "dev";

	private static String version;

	public static String getVersion() {
		if (version == null) {
			version = searchJadxVersion();
		}
		return version;
	}

	public static boolean isDevVersion() {
		return getVersion().equals(VERSION_DEV);
	}

	private static String searchJadxVersion() {
		try {
			ClassLoader classLoader = Jadx.class.getClassLoader();
			if (classLoader != null) {
				Enumeration<URL> resources = classLoader.getResources("META-INF/MANIFEST.MF");
				while (resources.hasMoreElements()) {
					try (InputStream is = resources.nextElement().openStream()) {
						Manifest manifest = new Manifest(is);
						String ver = manifest.getMainAttributes().getValue("jadx-version");
						if (ver != null) {
							return ver;
						}
					}
				}
			}
		} catch (Exception e) {
			LOG.error("Can't get manifest file", e);
		}
		return VERSION_DEV;
	}
}
