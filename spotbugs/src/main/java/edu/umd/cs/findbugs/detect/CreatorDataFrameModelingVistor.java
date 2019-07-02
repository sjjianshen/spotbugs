package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.ba.*;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import org.apache.bcel.generic.*;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;

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
        if (obj instanceof INVOKESTATIC) {
            CreatorDataValue creatorDataValue = null;
            try {
                creatorDataValue = getFrame().getStackValue(1);
            } catch (DataflowAnalysisException e) {
            }
            if (creatorDataValue != null && creatorDataValue.isJson()) {
                if ("parseObject".equals(methodName) && className.endsWith("JSON")) {
                    modelInstruction(obj, obj.consumeStack(getCPG()), obj.produceStack(getCPG()), CreatorDataValue.JSON);
                } else if (methodName.endsWith("parseObject") && className.endsWith("JSONObject")){
                    modelInstruction(obj, getNumWordsConsumed(obj), getNumWordsProduced(obj), CreatorDataValue.JSON);
                } else if (methodName.endsWith("parseArray") && className.endsWith("JSONObject")){
                    modelInstruction(obj, getNumWordsConsumed(obj), getNumWordsProduced(obj), CreatorDataValue.JSON);
                } else {
                    doHandleInvoke(obj);
                }
            } else {
                if ("parseObject".equals(methodName) && className.endsWith("JSON")) {
                    modelInstruction(obj, obj.consumeStack(getCPG()), obj.produceStack(getCPG()), CreatorDataValue.JSON_OBJECT);
                } else if (methodName.endsWith("parseObject") && className.endsWith("JSONObject")){
                    modelInstruction(obj, getNumWordsConsumed(obj), getNumWordsProduced(obj), CreatorDataValue.JSON_OBJECT_RAW);
                } else if (methodName.endsWith("parseArray") && className.endsWith("JSONObject")){
                    modelInstruction(obj, getNumWordsConsumed(obj), getNumWordsProduced(obj), CreatorDataValue.JSON_OBJECT_ARRAY);
                } else {
                    doHandleInvoke(obj);
                }
            }
        } else if ("fromJson".equals(methodName) && className.endsWith("Gson")) {
            modelInstruction(obj, getNumWordsConsumed(obj), getNumWordsProduced(obj), CreatorDataValue.JSON_OBJECT);
        } else if (methodName.endsWith("ForObject") && className.endsWith("RestTemplate")) {
            modelInstruction(obj, getNumWordsConsumed(obj), getNumWordsProduced(obj), CreatorDataValue.JSON_OBJECT);
        } else if (methodName.startsWith("get")) {
            CreatorDataFrame creatorDataFrame = getFrame();
            try {
                int consume = obj.consumeStack(cpg);
                CreatorDataValue caller = creatorDataFrame.getStackValue(consume - 1);
                if (caller == CreatorDataValue.JSON_OBJECT_RAW) {
                    if (methodName.equals("getJSONObject")) {
                        modelInstruction(obj, consume, obj.produceStack(cpg), CreatorDataValue.JSON);
                    } else if (methodName.equals("getJSONArray")) {
                        modelInstruction(obj, consume, obj.produceStack(cpg), CreatorDataValue.JSON);
                    } else {
                        String[] nullableGenratorsArr = new String[] {
                                "getTimestamp", "getSqlDate", "getDate", "getString", "getBigInteger", "getBigDecimal",
                                "getDouble", "getFloat", "getLong", "getInteger", "getShort", "getByte", "getBytes",
                                "getBoolean", "get"
                        };

                        List<String> nullGeneratorsSet = Arrays.asList(nullableGenratorsArr);
                        Type[] args = obj.getArgumentTypes(cpg);
                        if (nullGeneratorsSet.contains(methodName)) {
                            modelInstruction(obj, consume, obj.produceStack(cpg), CreatorDataValue.JSON);
                        } else if (methodName.equals("getObject") && args.length == 2) {
                            if ((args[1] instanceof ParameterizedType)) {
                                ParameterizedType pt = ((ParameterizedType)args[1]);
                                pt.getActualTypeArguments()[0].getTypeName();
                                if (isBoxedPrimativeType(pt.getActualTypeArguments()[0])) {
                                    modelInstruction(obj, consume, obj.produceStack(cpg), CreatorDataValue.NOT_JSON);
                                } else {
                                    modelInstruction(obj, consume, obj.produceStack(cpg), CreatorDataValue.JSON);
                                }
                            } else {
                                modelInstruction(obj, consume, obj.produceStack(cpg), CreatorDataValue.JSON);
                            }
                        } else {
                            doHandleInvoke(obj);
                        }
                    }
                } else if (caller.isJsonSource() && consume  <= 2) {
                    modelInstruction(obj, consume, obj.produceStack(cpg), CreatorDataValue.JSON);
                } else {
                    doHandleInvoke(obj);
                }
            } catch (CheckedAnalysisException e) {
                doHandleInvoke(obj);
            }
        } else {
            doHandleInvoke(obj);
        }
    }

    private boolean isBoxedPrimativeType(java.lang.reflect.Type type) {
        if (type == Integer.class || type == Boolean.class || type == Short.class || type == Byte.class ||
                type == Double.class || type == Float.class || type == Long.class) {
            return true;
        }
        return false;
    }

    private void doHandleInvoke(InvokeInstruction obj) {
        XMethod calledMethod = XFactory.createXMethod(obj, getCPG());
        Boolean isJson = AnalysisContext.currentAnalysisContext().getReturnValueJsonPropertyDatabase()
                .getProperty(calledMethod.getMethodDescriptor());
        CreatorDataValue cdv;
        if (isJson == null || isJson == Boolean.FALSE) {
            cdv = CreatorDataValue.NOT_JSON;
        } else {
            cdv = CreatorDataValue.JSON_OBJECT;
        }
        modelInstruction(obj, obj.consumeStack(getCPG()), obj.produceStack(getCPG()), cdv);
    }

    @Override
    public void visitINVOKESTATIC(INVOKESTATIC obj) {
        handleInvoke(obj);
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
//            e.printStackTrace();
            super.visitGETFIELD(obj);
        }
    }

    @Override
    public void visitCHECKCAST(CHECKCAST obj) {
            //do nothing, for checkcast willnot change the variable creator
    }
}
