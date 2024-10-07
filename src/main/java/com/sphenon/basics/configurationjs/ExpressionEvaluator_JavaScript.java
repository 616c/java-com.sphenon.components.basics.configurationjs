package com.sphenon.basics.configurationjs;

/****************************************************************************
  Copyright 2001-2024 Sphenon GmbH

  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  License for the specific language governing permissions and limitations
  under the License.
*****************************************************************************/

import com.sphenon.basics.context.*;
import com.sphenon.basics.context.classes.*;
import com.sphenon.basics.message.*;
import com.sphenon.basics.notification.*;
import com.sphenon.basics.exception.*;
import com.sphenon.basics.customary.*;
import com.sphenon.basics.data.*;
import com.sphenon.basics.operations.*;
import com.sphenon.basics.operations.classes.*;
import com.sphenon.basics.operations.factories.*;

import com.sphenon.basics.expression.*;
import com.sphenon.basics.expression.classes.*;
import com.sphenon.basics.expression.returncodes.*;

public class ExpressionEvaluator_JavaScript implements ExpressionEvaluator {

    public ExpressionEvaluator_JavaScript (CallContext context) {
        this.result_attribute = new Class_ActivityAttribute(context, "Result", "Object", "-", "*");
        this.activity_interface = new Class_ActivityInterface(context);
        this.activity_interface.addAttribute(context, this.result_attribute);
    }

    protected Class_ActivityInterface activity_interface;
    protected ActivityAttribute result_attribute;

    public String[] getIds(CallContext context) {
        return new String[] { "js", "javascript" };
    }

    public Object evaluate(CallContext context, String string, Scope scope, DataSink<Execution> execution_sink) throws EvaluationFailure {
        Execution_Basic e = null;
        if (execution_sink != null) {
            e = (Execution_Basic) Factory_Execution.createExecutionInProgress(context, "javascript");
            execution_sink.set(context, e);
        }

        try {
            JavaScriptEvaluator jse = null;
            if (scope != null) {
                jse = (JavaScriptEvaluator) scope.tryGet(context, "JavaScriptEvaluator", "session");
            }            
            if (jse == null) {
                jse = new JavaScriptEvaluator(context);
                jse.setStateful(context, true);
                if (scope != null && scope.containsNameSpace(context, "session")) {
                    scope.set(context, "JavaScriptEvaluator", "session", jse);
                }     
            }

            Scope local_scope = scope;
            if (execution_sink != null) {
                local_scope = new Class_Scope(context, null, scope, "execution_sink", execution_sink);
            }

            Object result = jse.evaluateToObject(context, string, local_scope);

            if (result instanceof Throwable) {
                if (e != null) { e.setFailure(context, (Throwable) result); }
                throw (Throwable) result;
            }

            if (e != null) {
                if (    (result instanceof Execution)
                     && ((Execution) result).getProblemState(context).isRed(context)
                   ) {
                    execution_sink.set(context, (Execution) result);
                } else {
                    e.setSuccess(context);
                }
            }

            return result;

        } catch (Throwable t) {
            if (e != null) { e.setFailure(context, t); }

            EvaluationFailure.createAndThrow(context, t, "Evaluation failure");
            throw (EvaluationFailure) null;
        }
    }

    public ActivityClass parse(CallContext context, ExpressionSource expression_source) throws EvaluationFailure {
        return new ActivityClass_ExpressionEvaluator(context, this, expression_source, this.activity_interface, this.result_attribute);
    }
}
