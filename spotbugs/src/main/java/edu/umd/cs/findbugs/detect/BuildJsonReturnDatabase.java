/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.*;
import edu.umd.cs.findbugs.ba.npe.IsNullValue;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueFrame;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ARETURN;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.ReferenceType;

import java.util.Iterator;

/**
 * Build database of methods that return values guaranteed to be nonnull
 *
 */
public class BuildJsonReturnDatabase implements Detector, NonReportingDetector, InterproceduralFirstPassDetector {

    public BuildJsonReturnDatabase(BugReporter bugReporter) {
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        if (AnalysisContext.currentAnalysisContext().isApplicationClass(classContext.getJavaClass())) {
            for (Method m : classContext.getMethodsInCallOrder()) {
                considerMethod(classContext, m);
            }
        }
    }

    @Override
    public void report() {

    }

    private void considerMethod(ClassContext classContext, Method method) {
        if ((method.getReturnType() instanceof ReferenceType) && classContext.getMethodGen(method) != null) {
            analyzeMethod(classContext, method);
        }
    }

    private void analyzeMethod(ClassContext classContext, Method method) {
        try {
            CFG cfg = classContext.getCFG(method);

            CreatorDataflow cdv = classContext.getCreatorDataflow(method);
            boolean isJson = false;
            for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
                Location location = i.next();
                InstructionHandle handle = location.getHandle();
                Instruction ins = handle.getInstruction();

                if (!(ins instanceof ARETURN)) {
                    continue;
                }
                CreatorDataFrame frame = cdv.getFactAtLocation(location);
                if (!frame.isValid()) {
                    continue;
                }
                CreatorDataValue value = frame.getTopValue();
                if (value.isJsonSource()) {
                    isJson = true;
                    break;
                }
            }

            if (isJson) {
                XMethod xmethod = XFactory.createXMethod(classContext.getJavaClass(), method);
                AnalysisContext.currentAnalysisContext().getReturnValueJsonPropertyDatabase()
                .setProperty(xmethod.getMethodDescriptor(), isJson);
            }

        } catch (CFGBuilderException e) {
            XMethod xmethod = XFactory.createXMethod(classContext.getJavaClass(), method);

            AnalysisContext.currentAnalysisContext().getLookupFailureCallback()
            .logError("Error analyzing " + xmethod + " for json training", e);
        } catch (DataflowAnalysisException e) {
            XMethod xmethod = XFactory.createXMethod(classContext.getJavaClass(), method);
            AnalysisContext.currentAnalysisContext().getLookupFailureCallback()
            .logError("Error analyzing " + xmethod + " for json training", e);
        }
    }
}
