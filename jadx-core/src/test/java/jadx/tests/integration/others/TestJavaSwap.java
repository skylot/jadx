package jadx.tests.integration.others;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;

import jadx.core.utils.files.FileUtils;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestJavaSwap extends IntegrationTest {

	@SuppressWarnings("StringBufferReplaceableByString")
	public static class TestCls {
		private Iterable<String> field;

		@Override
		public String toString() {
			String string = String.valueOf(this.field);
			return new StringBuilder(8 + String.valueOf(string).length())
					.append("concat(").append(string).append(")")
					.toString();
		}
	}

	@Test
	public void testJava() {
		useJavaInput();
		assertThat(getClassNode(TestCls.class))
				.code();
	}

	@Test
	public void test() throws IOException {
		// TODO: find up-to-date assembler/disassembler in java
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		cw.visit(Opcodes.V1_8, 0, "TestCls", null, "java/lang/Object", new String[] {});
		cw.visitField(Opcodes.ACC_PRIVATE, "field", "Ljava/lang/Iterable;", null, null).visitEnd();
		MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, new String[] {});
		mv.visitCode();
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(Opcodes.GETFIELD, "TestCls", "field", "Ljava/lang/Iterable;");
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;", false);
		mv.visitVarInsn(Opcodes.ASTORE, 1);
		mv.visitIntInsn(Opcodes.BIPUSH, 8);
		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;", false);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false);
		mv.visitInsn(Opcodes.IADD);
		mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
		mv.visitInsn(Opcodes.DUP_X1);
		mv.visitInsn(Opcodes.SWAP);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(I)V", false);
		mv.visitLdcInsn("concat(");
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
				"append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
				"append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
		mv.visitLdcInsn(")");
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
				"append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
		mv.visitInsn(Opcodes.ARETURN);
		mv.visitMaxs(0, 0); // auto calculated
		mv.visitEnd();
		cw.visitEnd();
		byte[] clsBytes = cw.toByteArray();

		StringWriter results = new StringWriter();
		CheckClassAdapter.verify(new ClassReader(clsBytes), false, new PrintWriter(results));
		assertThat(results.toString()).isEmpty();

		Path clsFile = FileUtils.createTempFile(".class");
		Files.write(clsFile, clsBytes);
		List<File> files = Collections.singletonList(clsFile.toFile());

		useJavaInput();
		assertThat(getClassNodeFromFiles(files, "TestCls"))
				.code();
	}
}
