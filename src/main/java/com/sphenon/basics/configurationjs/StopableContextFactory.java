package com.sphenon.basics.configurationjs;

/****************************************************************************
  Copyright 2001-2018 Sphenon GmbH

  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  License for the specific language governing permissions and limitations
  under the License.
*****************************************************************************/

import com.sphenon.basics.context.CallContext;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;

public class StopableContextFactory extends ContextFactory {

    protected StopableContextFactory(CallContext context) {
    }

    protected Context makeContext () {
        Context js_context = super.makeContext();

        if (this.enable_interrupts) {
            // Make Rhino runtime to call observeInstructionCount
            // each 10000 bytecode instructions
            js_context.setInstructionObserverThreshold(100);
        }

        return js_context;
    }
  
    protected boolean enable_interrupts = false;
    protected boolean interrupted = false;

    public boolean isInterrupted (CallContext context) {
        return interrupted;
    }
    
    public void interrupt (CallContext context) {
        this.interrupted = true;
    }

    public void enableInterrupts (CallContext context) {
        this.enable_interrupts = true;
    }

    public void disableInterrupts (CallContext context) {
        this.enable_interrupts = false;
    }
    
    protected void observeInstructionCount (Context js_context, int instruction_count) {
        if (this.interrupted) {
            throw new Error();
        }
    }
}
