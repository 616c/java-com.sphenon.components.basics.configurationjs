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
import com.sphenon.basics.debug.*;
import com.sphenon.basics.message.*;
import com.sphenon.basics.notification.*;
import com.sphenon.basics.exception.*;
import com.sphenon.basics.customary.*;
import com.sphenon.basics.configuration.*;
import com.sphenon.basics.system.*;
import com.sphenon.basics.expression.*;
import com.sphenon.basics.expression.returncodes.*;

import java.util.Vector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
 
public class JavaScriptShell {

    protected static boolean configuration_initialised;

    public static JavaScriptEvaluator getJSE(CallContext context) {
        if (configuration_initialised == false) {
            Configuration.initialise(context);
            configuration_initialised = true;
        }
        JavaScriptEvaluator jse = new JavaScriptEvaluator(context);
        jse.setStateful(context, true);
        return jse;
    }

    public static void main(String args[]) {

        Context context = RootContext.getRootContext ();
        Vector<String> restargs = Configuration.checkCommandLineArgs(args);

        JavaScriptEvaluator jse = null;

        try {
            if (restargs != null && restargs.size() != 0) {
                if (jse == null) {
                    jse = getJSE(context);
                }
                for (String jscmd : restargs) {

                    try {
                        // System.out.println("JS> " + jscmd);
                        String result = jse.evaluate(context, jscmd);
                        System.out.println(result);

                    } catch (Throwable t) {
                        Dumper.dump(context, "Error", t);
                        System.exit(8);
                    }
                }
            } else {
                
                System.out.print("JS> ");
                System.out.flush();
                
                String line = null;
                BufferedReader in  = new BufferedReader(new InputStreamReader(System.in));
                
                while ((line = in.readLine()) != null) {
                    if (line.matches("^(exit|quit|bye)[ \n\r\t]*$")) {
                        break;
                    }
                    if (line.matches("^(reset)[ \n\r\t]*$")) {
                        jse = null;
                        continue;
                    }
                    if (jse == null) {
                        jse = getJSE(context);
                    }
                    try {
                        String result = jse.evaluate(context, line);
                        System.out.println(result);
                    } catch (Throwable t) {
                        Dumper.dump(context, "Error", t);
                    }
                    System.out.print("JS> ");
                    System.out.flush();
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(8);
        }
    }
}
