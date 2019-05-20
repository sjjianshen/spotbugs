package edu.umd.cs.findbugs.classfile.engine.bcel;

import edu.umd.cs.findbugs.ba.interproc.MethodPropertyDatabase;
import edu.umd.cs.findbugs.ba.interproc.PropertyDatabaseFormatException;

public class ExceptionThrowerPropertyDatabase extends MethodPropertyDatabase<ExceptionThrowerSet> {
    @Override
    protected ExceptionThrowerSet decodeProperty(String propStr) throws PropertyDatabaseFormatException {
        return null;
    }

    @Override
    protected String encodeProperty(ExceptionThrowerSet property) {
        return null;
    }
}
