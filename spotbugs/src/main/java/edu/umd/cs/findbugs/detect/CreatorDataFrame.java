package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.ba.Frame;
import edu.umd.cs.findbugs.ba.npe.IsNullConditionDecision;

public class CreatorDataFrame extends Frame<CreatorDataValue> {
    /**
     * Constructor. This version of the constructor is for subclasses for which
     * it is always safe to call getDefaultValue(), even when the object is not
     * fully initialized.
     *
     * @param numLocals number of local variable slots in the method
     */
    private CreatorDataValueDecision creatorDataValueDecision;

    public CreatorDataValueDecision getDecision() {
        return creatorDataValueDecision;
    }

    public CreatorDataFrame(int numLocals) {
        super(numLocals);
    }

    public void setDecision(CreatorDataValueDecision creatorDataValueDecision) {
        this.creatorDataValueDecision = creatorDataValueDecision;
    }
}
