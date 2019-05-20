package edu.umd.cs.findbugs.classfile.engine.bcel;

import java.util.BitSet;

public class ExceptionThrowerSet {
    public static final ExceptionThrowerSet ALWAYS = new ExceptionThrowerSet(ThrowerType.ALWAYS);
    private ThrowerType state;

    private BitSet bitSet = new BitSet(32);

    public ExceptionThrowerSet(ThrowerType state) {
        this.state = state;
    }

    public static ExceptionThrowerSet newConditionalThrowerType() {
        return new ExceptionThrowerSet(ThrowerType.CONDITIONAL);
    }

    public void setParam(int index) {
        bitSet.set(index);
    }

    public void clear() {
        bitSet.clear();
    }

    public boolean isIndexSet(int index) {
        return bitSet.get(index);
    }

    public static enum ThrowerType {
        ALWAYS, CONDITIONAL;
    }

    public boolean isConditional() {
        return this.state == ThrowerType.CONDITIONAL;
    }
}
