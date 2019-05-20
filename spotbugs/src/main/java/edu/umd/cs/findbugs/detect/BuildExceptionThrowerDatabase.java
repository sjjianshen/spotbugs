package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.InterproceduralFirstPassDetector;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.ba.*;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.engine.bcel.ExceptionThrowerPropertyDatabase;
import edu.umd.cs.findbugs.classfile.engine.bcel.ExceptionThrowerSet;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

public class BuildExceptionThrowerDatabase implements Detector, NonReportingDetector, InterproceduralFirstPassDetector {
    public BuildExceptionThrowerDatabase(BugReporter bugReporter) {
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        if (AnalysisContext.currentAnalysisContext().isApplicationClass(classContext.getJavaClass())
                && !classContext.getXClass().isInterface() && !classContext.getXClass().isAbstract()) {
            for (Method m : classContext.getMethodsInCallOrder()) {
                if (m.isStatic() || m.isPublic()) {
                    if (m.getArgumentTypes().length > 0) {
                        considerKaboomMethod(classContext, m);
                    } else {
                        considerThrowerMethod(classContext, m);
                    }
                }
            }
        }
//        ExceptionThrowerPropertyDatabase db = Global.getAnalysisCache().getDatabase(ExceptionThrowerPropertyDatabase.class);
    }

    private void considerKaboomMethod(ClassContext classContext, Method m) {
        ExceptionThrowerSet throwerSet = ExceptionThrowerSet.newConditionalThrowerType();
        try {
            CFG cfg = classContext.getCFG(m);
            BasicBlock block = cfg.getEntry();
            if (!analyzeKaboomMethod(m, block, cfg, throwerSet)) {
                throwerSet.clear();
            } else {
                MethodDescriptor methodDescriptor = new MethodDescriptor(
                        classContext.getClassDescriptor().getClassName(), m.getName(), m.getSignature(), true);
                Global.getAnalysisCache().getDatabase(ExceptionThrowerPropertyDatabase.class)
                        .setProperty(methodDescriptor, throwerSet);
            }
        } catch (CFGBuilderException e) {
//            e.printStackTrace();
        }
    }

    private boolean analyzeKaboomMethod(Method m, BasicBlock start, CFG cfg, ExceptionThrowerSet throwerSet) {
        Stack<Iterator<Edge>> vector = new Stack<>();
        Set<BasicBlock> visited = new HashSet();
        Iterator<Edge> it = cfg.outgoingEdgeIterator(start);
        visited.add(start);
        InstructionHandle inh;
        BasicBlock target;
        BasicBlock source;
        Instruction ins;
        vector.push(it);
        while (!vector.empty()) {
            it = vector.pop();
            while (it.hasNext()) {
                Edge edge = it.next();
                if ((target = edge.getTarget()) == cfg.getExit()) {
                    if (edge.getType() != EdgeTypes.UNHANDLED_EXCEPTION_EDGE &&
                            ((inh = start.getLastInstruction()) == null || !(inh.getInstruction() instanceof ATHROW))) {
                        return false;
                    }
                }
                source = edge.getSource();
                if ((inh = source.getLastInstruction()) != null) {
                    ins = inh.getInstruction();
                    if (ins instanceof IFNONNULL || ins instanceof IFNULL) {
                        InstructionHandle prevh = inh.getPrev();
                        if (prevh.getInstruction() instanceof ALOAD) {
                            ALOAD aload = (ALOAD)prevh.getInstruction();
                            if (aload.getIndex() < m.getArgumentTypes().length) {
                                if ((ins.getOpcode() == Const.IFNULL && edge.getType() == EdgeTypes.FALL_THROUGH_EDGE) ||
                                        (ins.getOpcode() == Const.IFNONNULL && edge.getType() == EdgeTypes.IFCMP_EDGE)) {
                                    continue;
                                } else {
                                    throwerSet.setParam(aload.getIndex());
                                }
                            }
                        }
                    }
                }
                if (!visited.contains(target)) {
                    vector.push(it);
                    it = cfg.outgoingEdgeIterator(target);
                    visited.add(target);
                }
            }
        }
        return true;
    }

    private void considerThrowerMethod(ClassContext classContext, Method method) {
        try {
            CFG cfg = classContext.getCFG(method);
            BasicBlock basicBlock = cfg.getExit();
            Iterator<Edge> it = cfg.incomingEdgeIterator(basicBlock);
            int unhandledExceptionCount = 0, incomingCount = 0, athrowCount = 0;
            while (it.hasNext()) {
                incomingCount++;
                Edge inCommingEdge = it.next();
                if (inCommingEdge.getType() == EdgeTypes.UNHANDLED_EXCEPTION_EDGE) {
                    unhandledExceptionCount++;
                } else {
                    BasicBlock source = inCommingEdge.getSource();
                    InstructionHandle inh = source.getLastInstruction();
                    if (inh != null && inh.getInstruction() instanceof ATHROW) {
                        athrowCount++;
                    }
                }
            }
            if (athrowCount + unhandledExceptionCount == incomingCount) {
                MethodDescriptor methodDescriptor = new MethodDescriptor(
                        classContext.getClassDescriptor().getClassName(), method.getName(), method.getSignature(), true);
                Global.getAnalysisCache().getDatabase(ExceptionThrowerPropertyDatabase.class)
                        .setProperty(methodDescriptor, ExceptionThrowerSet.ALWAYS);
            }
        } catch (CFGBuilderException e) {
//            e.printStackTrace();
        }
    }

    @Override
    public void report() {

    }
}
