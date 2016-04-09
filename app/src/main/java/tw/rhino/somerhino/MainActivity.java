package tw.rhino.somerhino;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.commonjs.module.ModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.commonjs.module.RequireBuilder;
import org.mozilla.javascript.commonjs.module.provider.*;
import org.mozilla.javascript.tools.shell.Global;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    public  static class AssetModuleProvider extends ModuleSourceProviderBase {

        private android.content.Context activityContext;

        public AssetModuleProvider(android.content.Context context) {
            this.activityContext = context;
        }

        @Override
        protected ModuleSource loadFromUri(URI uri, URI base, Object validator) throws IOException, URISyntaxException {
            System.out.println("load from uri: " + uri + " base : " + base );
            Map<String, String> moduleMap = new HashMap<String, String>();
            moduleMap.put("modules/math", "math");
            moduleMap.put("modules/add", "add");
            moduleMap.put( "modules/subtract", "subtract");
            System.out.println("ModuleId for the uri : "  + uri.toString() + " is : " + moduleMap.get(uri.toString()));
            return load(moduleMap.get(uri.toString()));
        }

        @Override
        protected ModuleSource loadFromPrivilegedLocations(
                String moduleId, Object validator)
                throws IOException, URISyntaxException
        {
            System.out.println("load from Locations moduleId: " + moduleId );
            try {
                return load(moduleId);

            } catch (Exception e) {
                    e.printStackTrace();
                }
            return null;

        }

        private ModuleSource load(String moduleId) throws URISyntaxException, IOException {
            System.out.println("load : 1 : " + moduleId);
            Map<String, String> moduleMap = new HashMap<String, String>();
            moduleMap.put("math", "modules/math.js");
            moduleMap.put("add", "modules/add.js");
            moduleMap.put("subtract", "modules/subtract.js");
            return load(moduleMap, moduleId);
        }

        private ModuleSource load(Map<String, String> moduleMap, String moduleId) throws URISyntaxException, IOException {
            System.out.println("load: " + moduleId + " map: " + moduleMap.get(moduleId));

            URI base = new URI("modules/");
            URI uri = new URI(moduleMap.get(moduleId));

            System.out.println("base: " + base.toString());
            System.out.println("uri: " + base.toString());

            Reader reader = new BufferedReader(new InputStreamReader(
                activityContext.getAssets().open(moduleMap.get(moduleId))));
            return new ModuleSource(
                    reader,
                    null,
                    uri,
                    base,
                    null);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread(new Runnable() {
            @Override
            public void run() {
                something();
            }
        }).start();

    }

    public void something() {
        ModuleSourceProvider sourceProvider = new AssetModuleProvider(this);
        ModuleScriptProvider scriptProvider = new SoftCachingModuleScriptProvider(sourceProvider);
        RequireBuilder builder = new RequireBuilder();
        builder.setModuleScriptProvider(scriptProvider);

        Context context = Context.enter();
        try {
            context.setOptimizationLevel(-1);
            Scriptable scope = context.initStandardObjects();
            Require require = builder.createRequire(context, scope);
            require.install(scope);
            System.out.println("result1 : " + executeFile(context, scope, "main.js"));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Context.exit();
        }
    }

    private void oldThing() {
        System.out.println("This is main");
        Global global = new Global();
        Context context = Context.enter();
        try {
            context.setOptimizationLevel(-1);
            context.setErrorReporter(getErrorReporter());
            Scriptable scope = context.initStandardObjects();

            List<String> modulesPath = Arrays.asList("file:///android_asset/modules/math");
            Require require = global.installRequire(context, modulesPath, false);
            require.install(scope);

//      System.out.println("result2 : " + executeFile(context, scope, "modules/modules.math/add.js"));
//      System.out.println("result2 : " + executeFile(context, scope, "modules/modules.math/subtract.js"));

            System.out.println("result1 : " + executeFile(context, scope, "main.js"));

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Context.exit();
        }
    }


    private ErrorReporter getErrorReporter() {
        return new ErrorReporter() {
            @Override
            public void warning(String s, String s1, int i, String s2, int i1) {
                System.out.println(s + ":" + s1 + ":" + i + ":" + s2 + ":" + i1);
            }

            @Override
            public void error(String s, String s1, int i, String s2, int i1) {
                System.out.println(s + ":" + s1 + ":" + i + ":" + s2 + ":" + i1);
            }

            @Override
            public EvaluatorException runtimeError(String s, String s1, int i, String s2, int i1) {
                System.out.println(s + ":" + s1 + ":" + i + ":" + s2 + ":" + i1);
                return null;
            }
        };
    }

    private String executeFile(Context context, Scriptable scope, String fileName) throws IOException {
        String content = readFile1(fileName);
        Reader source = new BufferedReader(new InputStreamReader(getAssets().open(fileName)));
        Object result = context.evaluateReader(scope, source, fileName, 0, null);

//        Object result = context.evaluateString(scope, content, fileName, 1, null);
        return context.toString(result);
    }

    private  String readFile(String fileName) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
        String line;
        String content = "";
        while ((line = reader.readLine()) != null) {
            content += line;
        }
        return content;
    }
    private  String readFile1(String fileName) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(fileName)));
        String line;
        String content = "";
        while ((line = reader.readLine()) != null) {
            content += line;
        }
        return content;
    }
}
