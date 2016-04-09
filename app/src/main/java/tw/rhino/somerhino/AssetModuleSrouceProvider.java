package tw.rhino.somerhino;

import org.mozilla.javascript.commonjs.module.provider.*;

import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.List;

public class AssetModuleSrouceProvider extends ModuleSourceProviderBase {

    private static final long serialVersionUID = 1L;
    private final Iterable<URI> privilegedUris;
    private final Iterable<URI> fallbackUris;
    private final UrlConnectionSecurityDomainProvider urlConnectionSecurityDomainProvider;
    private final UrlConnectionExpiryCalculator urlConnectionExpiryCalculator;

    public AssetModuleSrouceProvider(Iterable<URI> privilegedUris, Iterable<URI> fallbackUris) {
        this(privilegedUris, fallbackUris, new DefaultUrlConnectionExpiryCalculator(), (UrlConnectionSecurityDomainProvider) null);
    }

    public AssetModuleSrouceProvider(Iterable<URI> privilegedUris, Iterable<URI> fallbackUris, UrlConnectionExpiryCalculator urlConnectionExpiryCalculator, UrlConnectionSecurityDomainProvider urlConnectionSecurityDomainProvider) {
        this.privilegedUris = privilegedUris;
        this.fallbackUris = fallbackUris;
        this.urlConnectionExpiryCalculator = urlConnectionExpiryCalculator;
        this.urlConnectionSecurityDomainProvider = urlConnectionSecurityDomainProvider;
    }

    protected ModuleSource loadFromPrivilegedLocations(String moduleId, Object validator) throws IOException, URISyntaxException {
        return this.loadFromPathList(moduleId, validator, this.privilegedUris);
    }

    protected ModuleSource loadFromFallbackLocations(String moduleId, Object validator) throws IOException, URISyntaxException {
        return this.loadFromPathList(moduleId, validator, this.fallbackUris);
    }

    private ModuleSource loadFromPathList(String moduleId, Object validator, Iterable<URI> paths) throws IOException, URISyntaxException {
        if (paths == null) {
            return null;
        } else {
            Iterator i$ = paths.iterator();

            ModuleSource moduleSource;
            do {
                if (!i$.hasNext()) {
                    return null;
                }

                URI path = (URI) i$.next();
                moduleSource = this.loadFromUri(path.resolve(moduleId), path, validator);
            } while (moduleSource == null);

            return moduleSource;
        }
    }

    protected ModuleSource loadFromUri(URI uri, URI base, Object validator) throws IOException, URISyntaxException {
        URI fullUri = new URI(uri + ".js");
        ModuleSource source = this.loadFromActualUri(fullUri, base, validator);
        return source != null ? source : this.loadFromActualUri(uri, base, validator);
    }

    protected ModuleSource loadFromActualUri(URI uri, URI base, Object validator) throws IOException {
        URL url = new URL(base == null ? null : base.toURL(), uri.toString());
        URLConnection urlConnection = this.openUrlConnection(url);
        return new ModuleSource(getReader(urlConnection), this.getSecurityDomain(urlConnection), uri, base, null);
    }

    private static Reader getReader(URLConnection urlConnection) throws IOException {
        return new InputStreamReader(urlConnection.getInputStream(), getCharacterEncoding(urlConnection));
    }

    private static String getCharacterEncoding(URLConnection urlConnection) {
        ParsedContentType pct = new ParsedContentType(urlConnection.getContentType());
        String encoding = pct.getEncoding();
        if (encoding != null) {
            return encoding;
        } else {
            String contentType = pct.getContentType();
            return contentType != null && contentType.startsWith("text/") ? "8859_1" : "utf-8";
        }
    }

    private Object getSecurityDomain(URLConnection urlConnection) {
        return this.urlConnectionSecurityDomainProvider == null ? null : this.urlConnectionSecurityDomainProvider.getSecurityDomain(urlConnection);
    }

    private void close(URLConnection urlConnection) {
        try {
            urlConnection.getInputStream().close();
        } catch (IOException var3) {
            this.onFailedClosingUrlConnection(urlConnection, var3);
        }

    }

    protected void onFailedClosingUrlConnection(URLConnection urlConnection, IOException cause) {
    }

    protected URLConnection openUrlConnection(URL url) throws IOException {
        return url.openConnection();
    }

    private static class URLValidator implements Serializable {
        private static final long serialVersionUID = 1L;
        private final URI uri;
        private final long lastModified;
        private final String entityTags;
        private long expiry;

        public URLValidator(URI uri, URLConnection urlConnection, long request_time, UrlConnectionExpiryCalculator urlConnectionExpiryCalculator) {
            this.uri = uri;
            this.lastModified = urlConnection.getLastModified();
            this.entityTags = this.getEntityTags(urlConnection);
            this.expiry = this.calculateExpiry(urlConnection, request_time, urlConnectionExpiryCalculator);
        }

        boolean updateValidator(URLConnection urlConnection, long request_time, UrlConnectionExpiryCalculator urlConnectionExpiryCalculator) throws IOException {
            boolean isResourceChanged = this.isResourceChanged(urlConnection);
            if (!isResourceChanged) {
                this.expiry = this.calculateExpiry(urlConnection, request_time, urlConnectionExpiryCalculator);
            }

            return isResourceChanged;
        }

        private boolean isResourceChanged(URLConnection urlConnection) throws IOException {
            return urlConnection instanceof HttpURLConnection ? ((HttpURLConnection) urlConnection).getResponseCode() == 304 : this.lastModified == urlConnection.getLastModified();
        }

        private long calculateExpiry(URLConnection urlConnection, long request_time, UrlConnectionExpiryCalculator urlConnectionExpiryCalculator) {
            if ("no-cache".equals(urlConnection.getHeaderField("Pragma"))) {
                return 0L;
            } else {
                String cacheControl = urlConnection.getHeaderField("Cache-Control");
                if (cacheControl != null) {
                    if (cacheControl.indexOf("no-cache") != -1) {
                        return 0L;
                    }

                    int explicitExpiry = this.getMaxAge(cacheControl);
                    if (-1 != explicitExpiry) {
                        long response_time = System.currentTimeMillis();
                        long apparent_age = Math.max(0L, response_time - urlConnection.getDate());
                        long corrected_received_age = Math.max(apparent_age, (long) urlConnection.getHeaderFieldInt("Age", 0) * 1000L);
                        long response_delay = response_time - request_time;
                        long corrected_initial_age = corrected_received_age + response_delay;
                        long creation_time = response_time - corrected_initial_age;
                        return (long) explicitExpiry * 1000L + creation_time;
                    }
                }

                long explicitExpiry1 = urlConnection.getHeaderFieldDate("Expires", -1L);
                return explicitExpiry1 != -1L ? explicitExpiry1 : (urlConnectionExpiryCalculator == null ? 0L : urlConnectionExpiryCalculator.calculateExpiry(urlConnection));
            }
        }

        private int getMaxAge(String cacheControl) {
            int maxAgeIndex = cacheControl.indexOf("max-age");
            if (maxAgeIndex == -1) {
                return -1;
            } else {
                int eq = cacheControl.indexOf(61, maxAgeIndex + 7);
                if (eq == -1) {
                    return -1;
                } else {
                    int comma = cacheControl.indexOf(44, eq + 1);
                    String strAge;
                    if (comma == -1) {
                        strAge = cacheControl.substring(eq + 1);
                    } else {
                        strAge = cacheControl.substring(eq + 1, comma);
                    }

                    try {
                        return Integer.parseInt(strAge);
                    } catch (NumberFormatException var7) {
                        return -1;
                    }
                }
            }
        }

        private String getEntityTags(URLConnection urlConnection) {
            List etags = (List) urlConnection.getHeaderFields().get("ETag");
            if (etags != null && !etags.isEmpty()) {
                StringBuilder b = new StringBuilder();
                Iterator it = etags.iterator();
                b.append((String) it.next());

                while (it.hasNext()) {
                    b.append(", ").append((String) it.next());
                }

                return b.toString();
            } else {
                return null;
            }
        }

        boolean appliesTo(URI uri) {
            return this.uri.equals(uri);
        }

        void applyConditionals(URLConnection urlConnection) {
            if (this.lastModified != 0L) {
                urlConnection.setIfModifiedSince(this.lastModified);
            }

            if (this.entityTags != null && this.entityTags.length() > 0) {
                urlConnection.addRequestProperty("If-None-Match", this.entityTags);
            }

        }

        boolean entityNeedsRevalidation() {
            return System.currentTimeMillis() > this.expiry;
        }
    }

}
