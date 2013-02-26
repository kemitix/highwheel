package org.pitest.highwheel.bytecodeparser;

import static org.pitest.highwheel.bytecodeparser.NameUtil.getElementNameForType;
import static org.pitest.highwheel.bytecodeparser.NameUtil.getOutermostClassName;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.pitest.highwheel.cycles.AccessVisitor;
import org.pitest.highwheel.model.AccessPoint;
import org.pitest.highwheel.model.AccessType;
import org.pitest.highwheel.model.ElementName;

class DependencyMethodVisitor extends MethodVisitor {

  private final AccessPoint   parent;
  private final AccessVisitor typeReceiver;

  public DependencyMethodVisitor(final AccessPoint owner,
      final AccessVisitor typeReceiver) {
    super(Opcodes.ASM4, null);
    this.typeReceiver = typeReceiver;
    this.parent = owner;
  }

  @Override
  public void visitMethodInsn(final int opcode, final String owner,
      final String name, final String desc) {
    this.typeReceiver
        .apply(this.parent, AccessPoint.create(
            getOutermostClassName(owner), name),
            AccessType.USES);
  }

  @Override
  public void visitFieldInsn(final int opcode, final String owner,
      final String name, final String desc) {
    this.typeReceiver
        .apply(this.parent, AccessPoint.create(
            getOutermostClassName(owner), name),
            AccessType.USES);
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String desc,
      final boolean visible) {
    this.typeReceiver.apply(this.parent, AccessPoint.create(ElementName
        .fromString(org.objectweb.asm.Type.getType(desc).getClassName())),
        AccessType.ANNOTATED);
    return null;
  }

  @Override
  public AnnotationVisitor visitParameterAnnotation(final int parameter,
      final String desc, final boolean visible) {
    this.typeReceiver.apply(this.parent, AccessPoint.create(ElementName
        .fromString(org.objectweb.asm.Type.getType(desc).getClassName())),
        AccessType.ANNOTATED);
    return null;
  }

  @Override
  public void visitLdcInsn(final Object cst) {
    if (cst instanceof Type) {
      ElementName element = getElementNameForType((Type) cst);
      this.typeReceiver
      .apply(this.parent, AccessPoint.create(element),
          AccessType.USES);
    }
  }
}
