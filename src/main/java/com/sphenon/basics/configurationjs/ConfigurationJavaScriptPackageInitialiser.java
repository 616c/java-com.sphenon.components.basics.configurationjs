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

import org.mozilla.javascript.ContextFactory;

import com.sphenon.basics.context.*;
import com.sphenon.basics.context.classes.*;
import com.sphenon.basics.configuration.*;
import com.sphenon.basics.message.*;
import com.sphenon.basics.variatives.*;
import com.sphenon.basics.services.*;
import com.sphenon.basics.graph.*;
import com.sphenon.basics.expression.*;

public class ConfigurationJavaScriptPackageInitialiser {

    static protected boolean initialised = false;

    static {
        initialise(RootContext.getRootContext());
    }

    static public synchronized void initialise (CallContext context) {
        
        if (initialised == false) {
            initialised = true;
            
            // call our own factory
            ContextFactory.initGlobal(new StopableContextFactory(context));

            Configuration.setConfigurationJavaScriptEvaluator(context, new com.sphenon.basics.configurationjs.classes.Class_ConfigurationJavaScriptEvaluator(context));

            Configuration.loadDefaultProperties(context, com.sphenon.basics.configurationjs.ConfigurationJavaScriptPackageInitialiser.class);

            ExpressionEvaluatorRegistry.registerDynamicStringEvaluator(context, new DynamicStringProcessor_JavaScript(context));
            ExpressionEvaluatorRegistry.registerExpressionEvaluator(context, new ExpressionEvaluator_JavaScript(context));

            ExpressionPackageInitialiser.loadDynamicStringProcessors(context, getConfiguration(context));
            ExpressionPackageInitialiser.loadExpressionEvaluators(context, getConfiguration(context));

            ServiceRegistry.registerConsumer(context, JavaScriptEvaluator.getConsumer(context));

            if (getConfiguration(context).get(context, "SaveJavaScriptCacheOnExit", false)) {
                JavaScriptEvaluator.saveCacheOnExit(context);
            }
        }
    }

    static protected Configuration config;
    static public Configuration getConfiguration (CallContext context) {
        if (config == null) {
            config = Configuration.create(RootContext.getInitialisationContext(), "com.sphenon.basics.configurationjs");
        }
        return config;
    }
}
