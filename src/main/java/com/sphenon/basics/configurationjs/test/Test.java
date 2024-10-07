package com.sphenon.basics.configurationjs.test;

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
import com.sphenon.basics.message.*;
import com.sphenon.basics.exception.*;
import com.sphenon.basics.expression.*;
import com.sphenon.basics.expression.classes.*;
import com.sphenon.basics.configuration.*;
import com.sphenon.basics.configurationjs.*;

public class Test {

    public static void main(String[] args) {
        System.out.println( "main..." );
        Context context = com.sphenon.basics.context.classes.RootContext.getRootContext ();

        System.out.println( "create my config..." );
        Configuration cfg = Configuration.create(context, "com.sphenon.basics.configurationjs.test");
        
        System.out.println( "property 'hans': " + cfg.get(context, "hans", "DEFAULT-WERT"));
        System.out.println( "property 'willy': " + cfg.get(context, "willy", "DEFAULT-WERT"));
        System.out.println( "property 'werner': " + cfg.get(context, "werner", "DEFAULT-WERT"));

        // System.out.println( "property 'hokuspokus*[JAVASCRIPT]': " + cfg.get(context, "hokuspokus*[JAVASCRIPT]", "DEFAULT-WERT"));
        // System.out.println( "property 'hokuspokus*[JAVASCRIPT/CACHE]': " + cfg.get(context, "hokuspokus*[JAVASCRIPT/CACHE]", "DEFAULT-WERT"));
        System.out.println( "property 'hokuspokus': " + cfg.get(context, "hokuspokus", "DEFAULT-WERT"));
        // System.out.println( "property 'hokuspokus*[JAVASCRIPT]': " + cfg.get(context, "hokuspokus*[JAVASCRIPT]", "DEFAULT-WERT"));
        // System.out.println( "property 'hokuspokus*[JAVASCRIPT/CACHE]': " + cfg.get(context, "hokuspokus*[JAVASCRIPT/CACHE]", "DEFAULT-WERT"));
        System.out.println( "property 'hokuspokus': " + cfg.get(context, "hokuspokus", "DEFAULT-WERT"));

        // System.out.println( "property 'hakaspakas*[JAVASCRIPT]': " + cfg.get(context, "hakaspakas*[JAVASCRIPT]", "DEFAULT-WERT"));
        // System.out.println( "property 'hakaspakas*[JAVASCRIPT/CACHE]': " + cfg.get(context, "hakaspakas*[JAVASCRIPT/CACHE]", "DEFAULT-WERT"));
        System.out.println( "property 'hakaspakas': " + cfg.get(context, "hakaspakas", "DEFAULT-WERT"));
        // System.out.println( "property 'hakaspakas*[JAVASCRIPT]': " + cfg.get(context, "hakaspakas*[JAVASCRIPT]", "DEFAULT-WERT"));
        // System.out.println( "property 'hakaspakas*[JAVASCRIPT/CACHE]': " + cfg.get(context, "hakaspakas*[JAVASCRIPT/CACHE]", "DEFAULT-WERT"));
        System.out.println( "property 'hakaspakas': " + cfg.get(context, "hakaspakas", "DEFAULT-WERT"));

        System.out.println( "property 'blubb': " + cfg.get(context, "blubb", "DEFAULT-WERT"));

        System.out.println( "property 'escape': " + cfg.get(context, "escape", "DEFAULT-WERT"));
        System.out.println( "property 'anders': " + cfg.get(context, "anders", "DEFAULT-WERT"));

        // com.sphenon.basics.expression.test.Test_DynamicStrings.DSTest(context, "javascript:'Hallo'+'Holla'");
        // com.sphenon.basics.expression.test.Test_DynamicStrings.DSTest(context, "js:'Fuu'+'balken'");

        // Scope scope = new Class_Scope(context);
        // scope.set(context, "hans", 4711);
        // com.sphenon.basics.expression.test.Test_DynamicStrings.DSTest(context, "js:'4700 + 11 = '+hans", scope);
        // com.sphenon.basics.expression.test.Test_DynamicStrings.ETest(context, "js:hans - 3896", scope);
        // com.sphenon.basics.expression.test.Test_DynamicStrings.ETest(context, "js:1 == 2", scope);
        // com.sphenon.basics.expression.test.Test_DynamicStrings.ETest(context, "js:[ 3, 4, 5 ]", scope);

        System.out.println( "main done." );
    }
}
