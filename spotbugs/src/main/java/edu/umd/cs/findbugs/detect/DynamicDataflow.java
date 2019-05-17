package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.ba.*;
import org.apache.bcel.generic.*;

import java.util.IdentityHashMap;
import java.util.Iterator;

public class DynamicDataflow {
    /**
     * Constructor.
     *
     * @param cfg      CFG of the method on which dfa is performed
     * @param analysis
     */
    private CFG cfg;
    private DynamicAnalysis dynamicAnalysis;
    private DepthFirstSearch dfs;
    private MethodGen methodGen;
    private final IdentityHashMap<BasicBlock, DynamicFrame> frameMap;

    public DynamicDataflow(DepthFirstSearch dfs, CFG cfg, MethodGen methodGen) {
        this.cfg = cfg;
        this.dfs = dfs;
        this.frameMap = new IdentityHashMap<>();
        this.methodGen = methodGen;
    }

    public void execute() {
        BlockOrder blockOrder = new ReversePostOrder(cfg, dfs);
        Iterator<BasicBlock> it = blockOrder.blockIterator();
        BasicBlock basicBlock;
        Iterator<Edge> edgeIterator;
        while (it.hasNext()) {
            basicBlock = it.next();
            DynamicFrame fact = createOrGetFrame(basicBlock);
            if (basicBlock == cfg.getEntry()) {
                continue;
            } else {
                edgeIterator = cfg.incomingEdgeIterator(basicBlock);
                while (edgeIterator.hasNext()) {
                    Edge edge = edgeIterator.next();
                    BasicBlock source = edge.getSource();
                    DynamicFrame sourceFact = createOrGetFrame(source);
                    InstructionHandle inh = source.getLastInstruction();
                    fact.mergeFrom(sourceFact);
                    if (inh != null) {
                        Instruction in = inh.getInstruction();
                        if (in instanceof IFNULL || in instanceof IFNONNULL) {
                            caculateDynamicMap(inh, source, edge, fact);
                        }
                    }
                }
            }
//            if (methodGen.getName().equals("cliquePay")) {
//                System.out.println(basicBlock);
//                BasicBlock.InstructionIterator aa = basicBlock.instructionIterator();
//                while (aa.hasNext()) {
//                    System.out.println(aa.next());
//                }
//                System.out.println(fact);
//            }
        }
    }

    private void caculateDynamicMap(InstructionHandle inh, BasicBlock source, Edge edge, DynamicFrame fact) {
        InstructionHandle preh = inh.getPrev();
        if (preh != null && preh.getInstruction() instanceof InvokeInstruction) {
            InvokeInstruction inv = (InvokeInstruction) preh.getInstruction();
            String methodName = inv.getMethodName(methodGen.getConstantPool());
            if (edge.getType() == EdgeTypes.FALL_THROUGH_EDGE && inh.getInstruction() instanceof IFNULL) {
                fact.putDynamic(methodName);
            } else if (edge.getType() == EdgeTypes.IFCMP_EDGE && inh.getInstruction() instanceof IFNONNULL) {
                fact.putDynamic(methodName);
            }
        }
    }

    private DynamicFrame createOrGetFrame(BasicBlock basicBlock) {
        DynamicFrame dynamicFrame;
        if ((dynamicFrame = frameMap.get(basicBlock)) == null) {
            dynamicFrame = DynamicFrame.emptyFrame();
            frameMap.put(basicBlock, dynamicFrame);
        }
        return dynamicFrame;
    }

    public DynamicFrame getFactAtBlock(BasicBlock basicBlock) {
        return frameMap.get(basicBlock);
    }
}
