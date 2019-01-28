package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.ba.AbstractFrameModelingVisitor;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.INVOKEVIRTUAL;

public class CreatorDataFrameModelingVistor extends AbstractFrameModelingVisitor<CreatorDataValue, CreatorDataFrame> {
    /**
     * Constructor.
     *
     * @param cpg the ConstantPoolGen of the method to be analyzed
     */
    public CreatorDataFrameModelingVistor(ConstantPoolGen cpg) {
        super(cpg);
    }

    @Override
    public CreatorDataValue getDefaultValue() {
        return CreatorDataValue.UK;
    }

    @Override
    public void visitINVOKEVIRTUAL(INVOKEVIRTUAL obj) {
        String methodName = obj.getMethodName(cpg);
        String className = obj.getClassName(cpg);
        if ("fromJson".equals(methodName) && "Gson".equals(className)) {
            modelInstruction(obj, getNumWordsConsumed(obj), getNumWordsProduced(obj), CreatorDataValue.JSON);
        } else if (methodName.startsWith("get")) {
            CreatorDataFrame creatorDataFrame = getFrame();
            try {
                CreatorDataValue creatorDataValue = creatorDataFrame.getTopValue();
                int consume = obj.consumeStack(cpg);
                if (creatorDataValue == CreatorDataValue.JSON && consume == 1) {
                    XClass xClass = Global.getAnalysisCache().getClassAnalysis(XClass.class, DescriptorFactory.createClassDescriptor(className));
                    String trimedName = methodName.substring("get".length());
                    for (XField xField : xClass.getXFields()) {
                        if (trimedName.equalsIgnoreCase(xField.getName())) {
                            int produce = obj.produceStack(cpg);
                            modelInstruction(obj, consume, produce, CreatorDataValue.JSON);
                            return;
                        }
                    }
                }
            } catch (CheckedAnalysisException e) {
                e.printStackTrace();
            }
            super.visitINVOKEVIRTUAL(obj);
        } else {
            super.visitINVOKEVIRTUAL(obj);
        }
    }

    @Override
    public void visitGETFIELD(GETFIELD obj) {
        CreatorDataFrame creatorDataFrame = getFrame();
        try {

            CreatorDataValue creatorDataValue = creatorDataFrame.getTopValue();
            if (creatorDataValue == CreatorDataValue.JSON) {
                int consume = obj.consumeStack(cpg);
                int produce = obj.produceStack(cpg);
                modelInstruction(obj, consume, produce, CreatorDataValue.JSON);
            } else {
                super.visitGETFIELD(obj);
            }
        } catch (DataflowAnalysisException e) {
            e.printStackTrace();
            super.visitGETFIELD(obj);
        }
    }
}
