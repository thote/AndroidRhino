package tw.rhino.somerhino;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.commonjs.module.ModuleScope;
import org.mozilla.javascript.commonjs.module.ModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.commonjs.module.RequireBuilder;
import org.mozilla.javascript.commonjs.module.provider.ModuleSource;
import org.mozilla.javascript.commonjs.module.provider.ModuleSourceProvider;
import org.mozilla.javascript.commonjs.module.provider.ModuleSourceProviderBase;
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

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
              try {
                something("main.js");
              } catch (URISyntaxException e) {
                e.printStackTrace();
              }
            }
        }).start();

    }

    public void something(String fileName) throws URISyntaxException {
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

        Reader reader = new BufferedReader(new InputStreamReader(getAssets().open(fileName)));

        ModuleScope scope1 = new ModuleScope(scope, new URI(fileName), null);
        Object result = context.evaluateReader(scope1, reader, fileName, 0, null);

        String value = context.toString(result);

        System.out.println("result1 : " + value);
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        Context.exit();
      }
    }
}
