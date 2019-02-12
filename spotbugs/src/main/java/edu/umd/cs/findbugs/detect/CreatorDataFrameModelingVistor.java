package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.ba.*;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.util.ClassName;
import org.apache.bcel.generic.*;

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
        return CreatorDataValue.NOT_JSON;
    }

    public void handleInvoke(InvokeInstruction obj) {
        String methodName = obj.getMethodName(cpg);
        String className = obj.getClassName(cpg);
        if ("fromJson".equals(methodName) && className.endsWith("Gson")) {
            modelInstruction(obj, getNumWordsConsumed(obj), getNumWordsProduced(obj), CreatorDataValue.JSON_SOURCE);
            return;
        }
        if (methodName.startsWith("get")) {
            CreatorDataFrame creatorDataFrame = getFrame();
            try {
                CreatorDataValue creatorDataValue = creatorDataFrame.getTopValue();
                int consume = obj.consumeStack(cpg);
                if (creatorDataValue.isJsonSource() && consume == 1) {
                    XClass xClass = Global.getAnalysisCache().getClassAnalysis(XClass.class,
                            DescriptorFactory.createClassDescriptor(ClassName.toSlashedClassName(className)));
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
        }
        doHandleInvoke(obj);
    }

    private void doHandleInvoke(InvokeInstruction obj) {
        XMethod calledMethod = XFactory.createXMethod(obj, getCPG());
        Boolean isJson = AnalysisContext.currentAnalysisContext().getReturnValueJsonPropertyDatabase()
                .getProperty(calledMethod.getMethodDescriptor());
        CreatorDataValue cdv;
        if (isJson == null || isJson == Boolean.FALSE) {
            cdv = CreatorDataValue.NOT_JSON;
        } else {
            cdv = CreatorDataValue.JSON_SOURCE;
        }
        modelInstruction(obj, obj.consumeStack(getCPG()), obj.produceStack(getCPG()), cdv);
    }

    @Override
    public void visitINVOKESTATIC(INVOKESTATIC obj) {
        doHandleInvoke(obj);
    }

    @Override
    public void visitINVOKESPECIAL(INVOKESPECIAL obj) {
        handleInvoke(obj);
    }

    @Override
    public void visitINVOKEINTERFACE(INVOKEINTERFACE obj) {
        handleInvoke(obj);
    }

    @Override
    public void visitINVOKEVIRTUAL(INVOKEVIRTUAL obj) {
        handleInvoke(obj);
    }

    @Override
    public void visitGETFIELD(GETFIELD obj) {
        CreatorDataFrame creatorDataFrame = getFrame();
        try {
            CreatorDataValue creatorDataValue = creatorDataFrame.getTopValue();
            if (creatorDataValue.isJsonSource()) {
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

    @Override
    public void visitCHECKCAST(CHECKCAST obj) {
            //do nothing, for checkcast willnot change the variable creator
    }
}
