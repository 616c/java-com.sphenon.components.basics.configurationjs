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
import com.sphenon.basics.services.*;
import com.sphenon.basics.graph.*;

import com.sphenon.basics.many.Tuple;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import java.lang.reflect.Method;

import java.io.File;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;

import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
 
public class JavaScriptEvaluator implements ManagedResource {

    static final public Class _class = JavaScriptEvaluator.class;

    static protected long notification_level;
    static public    long adjustNotificationLevel(long new_level) { long old_level = notification_level; notification_level = new_level; return old_level; }
    static public    long getNotificationLevel() { return notification_level; }

    static protected Configuration config;

    static protected long runtimestep_level;
    static public    long adjustRuntimeStepLevel(long new_level) { long old_level = runtimestep_level; runtimestep_level = new_level; return old_level; }
    static public    long getRuntimeStepLevel() { return runtimestep_level; }
    static { runtimestep_level = RuntimeStepLocationContext.getLevel(_class); };

    static {
        config = Configuration.create(RootContext.getInitialisationContext(), "com.sphenon.basics.configurationjs");
        source_lines_regexp = new RegularExpression(  "^ *@sourceLines\\("
                                                    +   "(?:" 
                                                    +     "(?:\"([^\"]+)\")"
                                                    +     "|(?:'([^']+)')"
                                                    +     "|(?:([A-Za-z0-9_]+))"
                                                    +   ")"
                                                    + "\\) *"
                                                    + "(?s:;(.*))?");
        volatile_regexp  = new RegularExpression("^ *@volatile\\((?:(?:\"([^\"]+)\")|(?:'([^']+)')|(?:null)),(?:(?:\"([^\"]+)\")|(?:'([^']+)')|(?:null))\\) *");
        unicode_on  = new RegularExpression("^ *(?:unicode|\u24CA) *(?::? *(?:true|\u2714))?"); // Ⓤ ✔
        unicode_off = new RegularExpression("^ *(?:unicode|\u24CA) *:? *(?:false|\u2718)"); // Ⓤ ✘
        notification_level = NotificationLocationContext.getLevel(_class);
    }

    static protected org.mozilla.javascript.Scriptable jsscope_shared = null;

    static protected boolean create_script_cache = true;
    static protected Boolean load_script_cache;

    protected boolean doLoadScriptCache(CallContext context) {
        if (load_script_cache == null) {
            load_script_cache = config.get(context, "LoadJavaScriptCache", false);
        }
        return load_script_cache;
    }

    public JavaScriptEvaluator (CallContext call_context) {
    }

    protected String expression;

    public String getExpression (CallContext context) {
        return this.expression;
    }

    public void setExpression (CallContext context, String expression) {
        this.expression = expression;
    }

    protected java.util.Hashtable parameters;

    public java.util.Hashtable getParameters (CallContext context) {
        return this.parameters;
    }

    public void setParameters (CallContext context, java.util.Hashtable parameters) {
        this.parameters = parameters;
    }

    protected boolean stateful;

    public boolean getStateful (CallContext context) {
        return this.stateful;
    }

    public void setStateful (CallContext context, boolean stateful) {
        this.stateful = stateful;
    }

    static protected RegularExpression source_lines_regexp;
    static protected RegularExpression volatile_regexp;
    static protected RegularExpression unicode_on;
    static protected RegularExpression unicode_off;

    static protected class ScriptEntry {
        public org.mozilla.javascript.Script script;
        public int count;
        public Set<Tuple<Tuple<String>>> signatures;
    }
    static protected Map<String,ScriptEntry> scripts;

    static protected ResourceAccessor resource_accessor;
    static protected Consumer<ResourceAccessor> consumer;
    static public Consumer<ResourceAccessor> getConsumer(CallContext context) {
        if (consumer == null) {
            consumer = new Consumer<ResourceAccessor>() {
                public void notifyNewService(CallContext context, ResourceAccessor new_ra) {
                    resource_accessor = new_ra;
                }
                public Class<ResourceAccessor> getServiceClass(CallContext context) {
                    return ResourceAccessor.class;
                }
            };
        }
        return consumer;
    }

    protected Object doEvaluation(CallContext context, String expression, org.mozilla.javascript.Context jsctx, org.mozilla.javascript.Scriptable jsscope) {
        return this.doEvaluation(context, expression, jsctx, jsscope, null);
    }

    protected Object doEvaluation(CallContext context, String expression, org.mozilla.javascript.Context jsctx, org.mozilla.javascript.Scriptable jsscope, String info) {
        CallContext previous = RootContext.setFallbackCallContext (context);

        String[] match;
        while (expression != null && (match = source_lines_regexp.tryGetMatches(context, expression)) != null && match.length > 0) {
            Object result = null;
            
            Vector<String> lines = null;
            String stream_variable = match[2];
            String fnn = null;
            if (stream_variable == null || stream_variable.isEmpty()) {
                String fn = (match[0] == null || match[0].isEmpty()) ? match[1] : match[0];
                fnn = fn.replaceFirst(".*/","");
                if ((notification_level & Notifier.DIAGNOSTICS) != 0) { NotificationContext.sendTrace(context, Notifier.DIAGNOSTICS, "JS sourcing: '%(file)'", "file", fn); }
                lines = (resource_accessor != null ? resource_accessor.read(context, fn) : FileUtilities.readFile(context, fn));
            } else {
                Object o = jsscope.get(stream_variable, jsscope);
                if (o instanceof org.mozilla.javascript.NativeJavaObject) {
                    o = ((org.mozilla.javascript.NativeJavaObject) o).unwrap();
                } else if (o instanceof org.mozilla.javascript.ScriptableObject) {
                    o = ((org.mozilla.javascript.ScriptableObject) o).getDefaultValue(null);
                }
                if (o == null) {
                    CustomaryContext.create((Context)context).throwPreConditionViolation(context, "Variable '%(name)' is not defined");
                    throw (ExceptionPreConditionViolation) null; // compiler insists
                }
                InputStream is = null;
                try {
                    is = (InputStream) o;
                } catch (ClassCastException cce) {
                    CustomaryContext.create((Context)context).throwPreConditionViolation(context, "Variable '%(name)' does not refer to an 'InputStream', but to a '%(class)' instead", "class", o.getClass().getName());
                    throw (ExceptionPreConditionViolation) null; // compiler insists
                }
                fnn = "<stream>";
                lines = FileUtilities.readStream(context, is);
            }
            boolean stateful_save = this.stateful;
            this.stateful = true;

            int l=0;
            boolean unicode = false;
            for (String line : lines) {
                if (unicode_on.matches(context, line)) {
                    unicode = true;
                    continue;
                }
                if (unicode_off.matches(context, line)) {
                    unicode = false;
                    continue;
                }
                if ((notification_level & Notifier.DIAGNOSTICS) != 0) { NotificationContext.sendTrace(context, Notifier.DIAGNOSTICS, "JS line:  '%(line)'", "line", line); }
                if (unicode) {
                    line = DynamicString.process(context, line, "unicode");
                }
                result = doEvaluation(context, line, jsctx, jsscope, "line " + l + " in " + fnn);
                if (result instanceof ExitCode) {
                    break;
                }
                l++;
            }

            this.stateful = stateful_save;

            if (match.length >= 4 && match[3] != null && match[3].isEmpty() == false) {
                expression = match[3];
            } else {
                return result;
            }
        } 

        if ((match = volatile_regexp.tryGetMatches(context, expression)) != null && match.length > 0) {
            Object result = null;
            
            String include = (match[0] != null ? match[0] : match[1] != null ? match[1] : null);
            String exclude = (match[2] != null ? match[2] : match[3] != null ? match[3] : null);

            this.volatile_include = include == null ? null : new RegularExpression(context, include);
            this.volatile_exclude = exclude == null ? null : new RegularExpression(context, exclude);

            return new Boolean(true);
        } else {
            RuntimeStep runtime_step = null;
            if (info == null) { info = ""; } else { info = ", " + info; }
            if ((runtimestep_level & RuntimeStepLevel.OBSERVATION_CHECKPOINT) != 0) { runtime_step = RuntimeStep.create((Context) context, RuntimeStepLevel.OBSERVATION_CHECKPOINT, _class, "Evaluating JavaScript expression '%(expression)'%(info)", "expression", expression, "info", info); }
            try {
                RootContext.setFallbackCallContext(context);

                ScriptEntry se = null;
                if (scripts == null) {
                    if (shutdown_in_progress == false) {
                        scripts = new HashMap<String,ScriptEntry>();
                    }
                } else {
                    se = scripts.get(expression);
                }
                if (se == null) {
                    se = new ScriptEntry();
                    se.script = jsctx.compileString(expression, "[configuration (no file)]", 0, null);
                    if (shutdown_in_progress == false) {
                        scripts.put(expression, se);
                    } else {
                        System.err.println("NOTICE: access to script cache during shutdown disabled (expression: '" + expression + "')");
                    }
                }
                se.count++;
                Object o = se.script.exec(jsctx, jsscope);

                if (create_script_cache) {
                    Tuple<Tuple<String>> sig = signature == null ? new Tuple<Tuple<String>>(context) : new Tuple<Tuple<String>>(context, signature, Tuple.class);
                    if (se.signatures == null) {
                        se.signatures = new HashSet<Tuple<Tuple<String>>>();
                    }
                    if (se.signatures.contains(sig) == false) {
                        se.signatures.add(sig);
                    }
                }

                if (o == null) {
                    return null;
                }
                // class does not exist in my js.jar
                // if (o instanceof org.mozilla.javascript.FlattenedObject) {
                //     o = ((org.mozilla.javascript.FlattenedObject) o).getObject();
                // }
                if (o instanceof org.mozilla.javascript.NativeJavaObject) {
                    o = ((org.mozilla.javascript.NativeJavaObject) o).unwrap();
                } else if (o instanceof org.mozilla.javascript.NativeArray) {
                    org.mozilla.javascript.NativeArray na = (org.mozilla.javascript.NativeArray) o;
                    Vector v = new Vector();
                    for (int oi=0; oi<na.getLength(); oi++) {
                        v.add(na.get(oi, na));
                    }
                    o = v;
                } else if (o instanceof org.mozilla.javascript.ConsString) {
                    o = ((org.mozilla.javascript.ConsString) o).toString();
                } else if (o instanceof org.mozilla.javascript.ScriptableObject) {
                    o = ((org.mozilla.javascript.ScriptableObject) o).getDefaultValue(null);
                }

                if (runtime_step != null) { runtime_step.setCompleted(context, "JavaScript expression successfully evaluated"); runtime_step = null; }

                return o;
            } catch (org.mozilla.javascript.JavaScriptException jse) {
                if (runtime_step != null) { runtime_step.setFailed(context, jse, "JavaScript expression evaluation failed"); runtime_step = null; }
                CustomaryContext.create(Context.create(context)).throwConfigurationError(context, jse, "Evaluation of JavaScript code '%(code)' failed", "code", expression);
                throw (ExceptionConfigurationError) null; // compiler insists
            } catch (org.mozilla.javascript.EvaluatorException ee) {
                if (runtime_step != null) { runtime_step.setFailed(context, ee, "JavaScript expression evaluation failed"); runtime_step = null; }
                CustomaryContext.create(Context.create(context)).throwConfigurationError(context, ee, "Evaluation of JavaScript code '%(code)' failed", "code", expression);
                throw (ExceptionConfigurationError) null; // compiler insists
            } catch (org.mozilla.javascript.EcmaError ee) {
                if (runtime_step != null) { runtime_step.setFailed(context, ee, "JavaScript expression evaluation failed"); runtime_step = null; }
                CustomaryContext.create(Context.create(context)).throwConfigurationError(context, ee, "Evaluation of JavaScript code '%(code)' failed", "code", expression);
                throw (ExceptionConfigurationError) null; // compiler insists
            } catch (Throwable t) {
                if (runtime_step != null) { runtime_step.setFailed(context, t, "JavaScript expression evaluation failed"); runtime_step = null; }
                CustomaryContext.create(Context.create(context)).throwConfigurationError(context, t, "Evaluation of JavaScript code '%(code)' failed", "code", expression);
                throw (ExceptionConfigurationError) null; // compiler insists
            } finally {
                RootContext.setFallbackCallContext (previous);
            }
        }
    }

    public String evaluate (CallContext context) {
        return evaluate(context, this.expression, this.parameters);
    }

    public Long evaluateToLong (CallContext context) {
        return new Long(evaluate(context, this.expression, this.parameters));
    }

    public String evaluate (CallContext context, String expression) {
        return evaluate(context, expression, (Hashtable) null);
    }

    public String evaluate (CallContext call_context, String expression, Object... arguments) {
        Hashtable parameters = new Hashtable();
        for (int a=0; a<arguments.length; a+=2) {
            if (arguments[a] != null && arguments[a+1] != null) {
                parameters.put(arguments[a], arguments[a+1]);
            }
        }
        return evaluate (call_context, expression, parameters);
    }

    public Object evaluateToObject (CallContext call_context, String expression, Object... arguments) {
        Hashtable parameters = new Hashtable();
        for (int a=0; a<arguments.length; a+=2) {
            if (arguments[a] != null && arguments[a+1] != null) {
                parameters.put(arguments[a], arguments[a+1]);
            }
        }
        return evaluateToObject (call_context, expression, parameters);
    }

    public Object evaluateToObject (CallContext call_context, String expression, java.util.Hashtable parameters) {
        return evaluateToTargetClass (call_context, expression, parameters, null, null);
    }

    public String evaluate (CallContext call_context, String expression, java.util.Hashtable parameters) {
        return evaluateToTargetClass (call_context, expression, parameters, null, String.class);
    }

    public Object evaluateToObject (CallContext call_context, String expression, Scope scope) {
        return evaluateToTargetClass (call_context, expression, null, scope, null);
    }

    public String evaluate (CallContext call_context, String expression, Scope scope) {
        return evaluateToTargetClass (call_context, expression, null, scope, String.class);
    }

    protected Vector<Tuple<String>> signature;

    protected class JSScopeAccessInterceptor implements Interceptor {
        protected org.mozilla.javascript.Context    jsctx;
        protected org.mozilla.javascript.Scriptable jsscope;
        protected Scope scope;

        public JSScopeAccessInterceptor(CallContext context, Scope scope, org.mozilla.javascript.Context jsctx, org.mozilla.javascript.Scriptable jsscope) {
            this.scope     = scope;
            this.jsctx     = jsctx;
            this.jsscope   = jsscope;
        }

        public void attachScope (CallContext context, Scope scope) {
            this.scope   = scope;
        }

        public boolean matches(Object target, Method method, Object[] arguments){
            String mn = method.getName();
            return (   (    (mn.equals("has") || mn.equals("get"))
                         && arguments != null
                         && arguments.length == 2
                         && arguments[0] instanceof String
                         && arguments[1] instanceof org.mozilla.javascript.Scriptable
                       )
                    || (    (mn.equals("put"))
                         && arguments != null
                         && arguments.length == 3
                         && arguments[0] instanceof String
                         && arguments[1] instanceof org.mozilla.javascript.Scriptable
                       )
                   );
        }
        protected java.util.Hashtable accessed;
        public Object handleInvocation(Object proxy, Delegate delegate, Object target, Method method, Object[] arguments) throws Throwable {
            if (accessed == null) {
                accessed = new java.util.Hashtable(4);
            }
            String mn = method.getName();
            if (mn.equals("put")) {
                String name = (String) (arguments[0]);
                if (accessed.get(name) == null) {
                    accessed.put(name, "put");
                }
                // well, we do this since otherwise put call produces infinite
                // loop - this is so since proxy handling does work 100%
                // transparently (identity is not preserved!): the argument[1]
                // is passed here the proxy, too, and inside the target object
                // (JS IdScriptableObject) a check this == argument is made,
                // which fails and produces that named loop
                if (arguments[1] == proxy) {
                    arguments[1] = target;
                }
                return method.invoke(target, arguments);
            } else {
                String name = (String) (arguments[0]);

                CallContext context = RootContext.getFallbackCallContext();
                boolean is_volatile = (    (    volatile_include != null
                                             || volatile_exclude != null
                                           )
                                        && (    volatile_include == null
                                             || volatile_include.matches(context, name) == true
                                           )
                                        && (    volatile_exclude == null
                                             || volatile_exclude.matches(context, name) == false
                                           )
                                      );

                if (is_volatile || accessed.get(name) == null) {
                    accessed.put(name, "get");
                    Scope.Result result = this.scope.tryGetWithNull(name);
                    if (result != null) {
                        this.jsscope.put(name, this.jsscope, result.value == null ? null : this.jsctx.toObject(result.value, this.jsscope));
                    } else {
                        if (is_volatile) {
                            // only for volatile, since otherwise we may delete
                            // items that were stored in the jsscope before already
                            this.jsscope.delete(name);
                        }
                    }

                    if (create_script_cache) {
                        if (signature == null) {
                            signature = new Vector<Tuple<String>>(4);
                        }
                        signature.add(new Tuple<String>(null /* context */, "S", name, result == null || result.value == null ? "---" : result.value.getClass().getName()));
                    }
                }
                return method.invoke(target, arguments);
            }
        }
    }

    protected org.mozilla.javascript.Scriptable jsscope;
    protected RegularExpression volatile_include;
    protected RegularExpression volatile_exclude;
    protected JSScopeAccessInterceptor jsscope_interceptor;
    static protected java.lang.reflect.Constructor<org.mozilla.javascript.Scriptable> proxy_constructor;

    static public JavaScriptJavaCache jsjc = null;

    public void release(CallContext context) {
        this.jsscope = null;
        this.jsscope_interceptor = null;
    }

    public<T> T evaluateToTargetClass (CallContext call_context, String expression, java.util.Hashtable parameters, Scope scope, Class<T> target_class) {
        // required: we need in doEvaluation cause of RuntimeStep
        Context context = Context.create(call_context);

        if (doLoadScriptCache(context) && jsjc != null) {
            try {
                JavaScriptJavaCache.Result jsjcr = jsjc.evaluate(context, expression, parameters, scope);
                if (jsjcr != null) { return (T) jsjcr.result; }
            } catch (Throwable t) {
                CustomaryContext.create(Context.create(context)).throwConfigurationError(context, t, "Evaluation of translated JavaScript code '%(code)' failed", "code", expression);
                throw (ExceptionConfigurationError) null; // compiler insists
            }
        }

        org.mozilla.javascript.Context jsctx = org.mozilla.javascript.Context.enter();
        // jsctx.setErrorReporter(new ScriptErrorReporter(ctx));

        if (this.stateful == false) {
            this.jsscope = null;
            this.jsscope_interceptor = null;
        }

        boolean need_to_restore_class_loader = false;
        ClassLoader current_javascript_class_loader = null;
        ClassLoader current_thread_class_loader = null;
        try {
            current_javascript_class_loader = jsctx.getApplicationClassLoader();
            current_thread_class_loader = Thread.currentThread().getContextClassLoader();
            if (current_javascript_class_loader != current_thread_class_loader) {
                need_to_restore_class_loader = true;
                jsctx.setApplicationClassLoader(current_thread_class_loader);
            }

            if (this.jsscope_shared == null) {

                this.jsscope_shared = new org.mozilla.javascript.ImporterTopLevel(jsctx);

                this.jsscope_shared.put("context", this.jsscope_shared, jsctx.toObject(context, this.jsscope_shared));
                String library = config.get(context, "JavaScriptLibrary", (String) null);
                if (library != null) {
                    doEvaluation(context, library, jsctx, this.jsscope_shared);
                }
            }
            if (this.jsscope == null) {
                this.jsscope = jsctx.newObject(jsscope_shared);

                // according to https://developer.mozilla.org/En/Rhino_documentation/Scopes_and_Contexts
                // so that new vars are created in jsscope
                this.jsscope.setPrototype(jsscope_shared);
                this.jsscope.setParentScope(null);

                this.jsscope.put("context", this.jsscope, jsctx.toObject(context, this.jsscope));

                org.mozilla.javascript.NativeJavaPackage pkg = new org.mozilla.javascript.NativeJavaPackage("", current_thread_class_loader);
                org.mozilla.javascript.ScriptRuntime.setObjectProtoAndParent(pkg, this.jsscope);
                this.jsscope.put("RootPackage", this.jsscope, pkg);
            }

            this.signature = null;

            if (scope != null) {

                this.jsscope.put("scope", this.jsscope, jsctx.toObject(scope, this.jsscope));

                if (this.jsscope_interceptor == null) {
                    this.jsscope_interceptor = new JSScopeAccessInterceptor(context, scope, jsctx, jsscope);

                    if (proxy_constructor == null) {
                        proxy_constructor = Delegate.getProxyConstructor(org.mozilla.javascript.Scriptable.class);
                    }

                    this.jsscope = Delegate.createInstance(proxy_constructor, (org.mozilla.javascript.Scriptable) this.jsscope, this.jsscope_interceptor);
                } else {
                    this.jsscope_interceptor.attachScope(context, scope);
                }
            }

            if (parameters != null) {
                java.util.Set entry_set = parameters.entrySet();
                java.util.Iterator iterator = entry_set.iterator();
                while (iterator.hasNext()) {
                    java.util.Map.Entry me = (java.util.Map.Entry) iterator.next();
                    String name = (java.lang.String) me.getKey();
                    Object value = me.getValue();
                    this.jsscope.put(name, this.jsscope, jsctx.toObject(value, this.jsscope));
                    if (create_script_cache) {
                        if (signature == null) {
                            signature = new Vector<Tuple<String>>(4);
                        }
                        signature.add(new Tuple<String>(null /* context */, "H", name, value == null ? "---" : value.getClass().getName()));
                    }
                }
            }

            Object o = doEvaluation(context, expression, jsctx, this.jsscope);

            if (target_class == null) {
                return (T) o;
            }
            if (target_class == String.class) {
                return (T) (o == null ? null : o.toString());
            }
            return (T) jsctx.jsToJava(o, target_class);

        } catch (org.mozilla.javascript.JavaScriptException jse) {
            CustomaryContext.create((Context)context).throwEnvironmentFailure(context, jse, "Could not create JavaScript instance scope");
            throw (ExceptionEnvironmentFailure) null; // compiler insists
        } finally {
            if (need_to_restore_class_loader) {
                // denn trotz exit wird im thread der gleiche context benutzt!
                jsctx.setApplicationClassLoader(current_javascript_class_loader);
            }

            if (jsctx != null) {
                jsctx.exit();
            }

            if (this.stateful == false) {
                this.jsscope = null;
                this.jsscope_interceptor = null;
            }
        }
    }

//     static class MyErrorReporter implements org.mozilla.javascript.ErrorReporter { 
//         CallContext context;
//         public ScriptErrorReporter(CallContext context) {
//             this.context = context;
//         }
//         public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
//             // reports
//         }
//         public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
//             // reports
//         }
//         public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
//             // to overload created default exception
//             return new EvaluatorException(message, sourceName, line, lineSource, lineOffset);
//         }
//     }

    static public void enableInterrupts(CallContext context) {
        ((StopableContextFactory) org.mozilla.javascript.ContextFactory.getGlobal()).enableInterrupts(context);
    }

    static public void interrupt(CallContext context) {
        ((StopableContextFactory) org.mozilla.javascript.ContextFactory.getGlobal()).interrupt(context);
    }

    static public void dumpScripts(CallContext context) {
        if (create_script_cache) {
            for (String script : scripts.keySet()) {
                ScriptEntry se = scripts.get(script);
                System.err.printf("%8s %s\n", se.count, script);
                if (se.signatures != null) {
                    for (Tuple<Tuple<String>> signature : se.signatures) {
                        boolean first = true;
                        for (Tuple<String> parameter : signature.getItems(context)) {
                            System.err.printf("         %s %32s [%s]: %s\n", first ? "-" : " ", parameter.getItem(context, 0), parameter.getItem(context, 1), parameter.getItem(context, 2));
                            first = false;
                        }
                    }
                }
            }
        }
    } 

    static protected String java_cache_file;
    static protected boolean shutdown_in_progress;

    static public void saveCacheOnExit(CallContext context) {
        // we get it here since during shutdown strange things may happen
        java_cache_file = config.get(context, "JavaScriptJavaCacheFile", (String) null);
        java.lang.Runtime.getRuntime().addShutdownHook(new Thread() { public void run() { saveCache(RootContext.getDestructionContext()); } });
    }

    static public void saveCache(CallContext context) {
        if (create_script_cache) {
            shutdown_in_progress = true;
            try {
                if (java_cache_file != null) {
                    File f = new File(java_cache_file);
                    f.setWritable(true);
                    FileOutputStream fos = new FileOutputStream(f);
                    OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
                    BufferedWriter bw = new BufferedWriter(osw);
                    PrintWriter pw = new PrintWriter(bw);
                    
                    pw.print("package com.sphenon.basics.configurationjs;\n");
                    pw.print("\n");
                    pw.print("import com.sphenon.basics.context.*;\n");
                    pw.print("import com.sphenon.basics.context.classes.*;\n");
                    pw.print("import com.sphenon.basics.debug.*;\n");
                    pw.print("import com.sphenon.basics.message.*;\n");
                    pw.print("import com.sphenon.basics.notification.*;\n");
                    pw.print("import com.sphenon.basics.exception.*;\n");
                    pw.print("import com.sphenon.basics.customary.*;\n");
                    pw.print("import com.sphenon.basics.configuration.*;\n");
                    pw.print("import com.sphenon.basics.expression.*;;\n");
                    pw.print("\n");
                    pw.print("import com.sphenon.basics.many.Tuple;\n");
                    pw.print("\n");
                    pw.print("import java.util.Hashtable;\n");
                    pw.print("import java.util.Vector;\n");
                    pw.print("import java.util.Map;\n");
                    pw.print("import java.util.HashMap;\n");
                    pw.print("import java.util.Set;\n");
                    pw.print("import java.util.HashSet;\n");
                    pw.print("\n");
                    pw.print("public class JavaScriptJavaCacheImpl implements JavaScriptJavaCache {\n");
                    pw.print("    public Result evaluate(CallContext context, String expression, Hashtable parameters, Scope scope) {\n");
                    pw.print("        Object[] values;\n");
                    pw.print("        switch (expression.hashCode()) {\n");

                    Set<Integer> hash_codes = new HashSet<Integer>();
                    for (String script : scripts.keySet()) {
                        ScriptEntry se = scripts.get(script);
                        Integer hc = script.hashCode();
                        if (hash_codes.contains(hc)) {
                            System.err.print("*** WARNING! *** hash code duplicate!\n");
                        }
                        hash_codes.add(hc);
                        pw.printf("            // %8d %s\n", se.count, script.replace("\n", " "));
                        pw.print("            case " + hc + ":\n");
                        if (se.signatures != null) {
                            for (Tuple<Tuple<String>> signature : se.signatures) {
                                pw.print("                if ((values = checkSignature(context, parameters, scope");
                                for (Tuple<String> parameter : signature.getItems(context)) {
                                    String from = parameter.getItem(context, 0);
                                    String name = parameter.getItem(context, 1);
                                    String type = parameter.getItem(context, 2);
                                    if (ignoreInCache(context, name, type)) {
                                        continue;
                                    }
                                    pw.print(", '" + from + "', \"" + name + "\", " + type + ".class");
                                }
                                pw.print(")) != null) {\n");
                                int i = 0;
                                for (Tuple<String> parameter : signature.getItems(context)) {
                                    String from = parameter.getItem(context, 0);
                                    String name = parameter.getItem(context, 1);
                                    String type = parameter.getItem(context, 2);
                                    if (ignoreInCache(context, name, type)) {
                                        continue;
                                    }
                                    pw.print("                    " + type + " " + name + " = (" + type + ") (values[" + i++ + "]);\n");
                                }
                                String[] java = convertJSToJava(context, script);
                                if (java[0] != null) {
                                    pw.print("                    " + java[0] + "\n");
                                }
                                pw.print("                    return new Result(" + (java[1] == null ? "null" : java[1]) + ");\n");
                                pw.print("                }\n");
                            }
                        }
                        pw.print("                break;\n");
                    }

                    pw.print("        }\n");
                    pw.print("        return null;\n");
                    pw.print("    }\n");
                    pw.print("\n");

                    pw.print("    static protected Object[] checkSignature(CallContext context, Hashtable parameters, Scope scope, Object... arguments) {\n");
                    pw.print("        Object[] values = new Object[arguments == null ? 0 : (arguments.length / 2)];\n");
                    pw.print("        if (arguments != null) {\n");
                    pw.print("            if (arguments.length != 0 && scope == null) { return null; }\n");
                    pw.print("            for (int i=0, j=0; i<arguments.length; i+=3, j++) {\n");
                    pw.print("                Object value;\n");
                    pw.print("                if (((Character) arguments[i]) == 'S') {\n");
                    pw.print("                    Scope.Result sr = scope.tryGetWithNull (context, (String) arguments[i+1]);\n");
                    pw.print("                    if (sr == null) { return null; }\n");
                    pw.print("                    value = sr.value;\n");
                    pw.print("                } else {\n");
                    pw.print("                    value = parameters.get((String) arguments[i+1]);\n");
                    pw.print("                    if (value == null) { return null; }\n");
                    pw.print("                }\n");
                    pw.print("                if (arguments[i+2] != null && value != null && (((Class)(arguments[i+2])).isAssignableFrom(value.getClass())) == false) { return null; }\n");
                    pw.print("                values[j] = value;\n");
                    pw.print("            }\n");
                    pw.print("        }\n");
                    pw.print("        return values;\n");
                    pw.print("    }\n");
                    pw.print("}\n");
                    pw.print("\n");
                    
                    pw.close();
                    bw.close();
                    osw.close();
                    fos.close();
                }
            } catch (FileNotFoundException fnfe) {
                CustomaryContext.create((Context)context).throwPreConditionViolation(context, fnfe, "Cannot write to file '%(filename)'", "filename", java_cache_file);
                throw (ExceptionPreConditionViolation) null; // compiler insists
            } catch (UnsupportedEncodingException uee) {
                CustomaryContext.create((Context)context).throwPreConditionViolation(context, uee, "Cannot write to file '%(filename)'", "filename", java_cache_file);
                throw (ExceptionPreConditionViolation) null; // compiler insists
            } catch (IOException ioe) {
                CustomaryContext.create((Context)context).throwPreConditionViolation(context, ioe, "Cannot write to file '%(filename)'", "filename", java_cache_file);
                throw (ExceptionPreConditionViolation) null; // compiler insists
            }
        }
    }

    static protected boolean ignoreInCache(CallContext context, String name, String type) {
        if (    (name.equals("context") && type.equals("---"))
             || (name.equals("Locator") && type.equals("---"))
             || (name.equals("ObjectIterable") && type.equals("---"))
             || (name.equals("Packages") && type.equals("---"))
             || (name.equals("java") && type.equals("---"))
           ) {
            return true;
        }
        return false;
    }

    static protected String[] convertJSToJava(CallContext context, String script) {
        String[] result = new String[2];

        StringBuilder code = new StringBuilder();
        StringBuilder line = new StringBuilder();
        boolean in_1 = false;
        boolean in_2 = false;
        boolean bs = false;
        for (int i=0; i<script.length(); i++) {
            char c = script.charAt(i);
            if (in_1) {
                switch (c) {
                    case '\'':
                        if (bs) {
                            line.append('\'');
                            bs = false;
                        } else {
                            line.append('"');
                            in_1 = false;
                        }
                        break;
                    case '\\':
                        if (bs) {
                            line.append('\\');
                            bs = false;
                        } else {
                            bs = true;
                        }
                        break;
                    default:
                        if (bs) {
                            line.append('\\');
                            bs = false;
                        }
                        line.append(c);
                        break;
                }
            } else if (in_2) {
                switch (c) {
                    case '"':
                        if (bs) {
                            line.append('\\');
                            line.append('"');
                            bs = false;
                        } else {
                            line.append('"');
                            in_2 = false;
                        }
                        break;
                    default:
                        line.append(c);
                        break;
                }
            } else  {
                switch (c) {
                    case '\'':
                        in_1 = true;
                        line.append('"');
                        break;
                    case '"':
                        in_2 = true;
                        line.append('"');
                        break;
                    case ';':
                        code.append(line);
                        code.append(c);
                        line.setLength(0);
                        break;
                    default:
                        line.append(c);
                        break;
                }
            }
        }
        result[0] = code.length() == 0 ? null : code.toString();
        result[1] = line.toString();
        
        return result;
    }
}
