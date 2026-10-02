/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.hasor.cobble.loader.ResourceLoader;
import net.hasor.dataql.domain.Udf;
import net.hasor.dataql.domain.UdfSource;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.QueryBuilder;
import net.hasor.dataql.kernel.CustomizeScope;
import net.hasor.dataql.kernel.Finder;
import net.hasor.dataql.kernel.FragmentProcess;
import net.hasor.dataway.authorization.AuthorizationCheck;
import net.hasor.dataway.authorization.IdentityProvider;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.result.ResultHandler;
import net.hasor.dataway.result.csv.CsvResultHandler;
import net.hasor.dataway.result.raw.RawResultHandler;
import net.hasor.dataway.result.structure.StructureResultHandler;
import net.hasor.dataway.result.text.TextResultHandler;
import net.hasor.dataway.result.verifycode.VerifyCodeResultHandler;
import net.hasor.dataway.service.admin.AdminInterceptor;
import net.hasor.dataway.service.script.ApiInterceptor;
import net.hasor.dataway.web.body.UploadStorage;

/** Configures shared storage, authorization, HTTP entries and the DataQL runtime. */
public class DatawayConfig {
    private       ApiDataAccessLayer                     dataAccessLayer;
    private final Map<EntityType, String>                tableMappings         = new EnumMap<>(EntityType.class);
    private final Map<EntityType, Map<FieldDef, String>> fieldMappings         = new EnumMap<>(EntityType.class);
    //
    private       IdentityProvider                       identityProvider;
    private       AuthorizationCheck                     authorizationCheck;
    private final List<AdminInterceptor>                 adminInterceptors     = new ArrayList<>();
    private       Path                                   uploadTempDirectory;
    private       int                                    uploadMemoryThreshold = UploadStorage.DEFAULT_MEMORY_THRESHOLD;
    private       String                                 documentTitle         = "Dataway API";
    private       String                                 documentVersion       = "1.0";
    private       String                                 documentServer        = "/api";
    //
    private       Finder                                 finder                = new DatawayFinder();
    private       CustomizeScope                         customizeScope;
    private       String                                 defaultResultHandler  = "structure";
    private       boolean                                wrapAllParameters     = false;
    private       String                                 wrapParameterName     = "root";
    private final List<Consumer<HostConfiguration>>      hostCustomizers       = new ArrayList<>();
    private final List<Consumer<QueryBuilder>>           queryCustomizers      = new ArrayList<>();
    private final List<ApiInterceptor>                   apiInterceptors       = new ArrayList<>();
    private final Map<String, ResultHandler>             resultHandlers        = new LinkedHashMap<>();

    public DatawayConfig() {
        this.resultHandlers.put("structure", new StructureResultHandler());
        this.resultHandlers.put("raw", new RawResultHandler());
        this.resultHandlers.put("csv", new CsvResultHandler());
        this.resultHandlers.put("text", new TextResultHandler());
        this.resultHandlers.put("verifyCode", new VerifyCodeResultHandler());
    }

    /** Registers a named response converter selectable through the API's resultHandler option. */
    public DatawayConfig resultHandler(String name, ResultHandler handler) {
        if (name == null || !name.matches("[a-zA-Z][a-zA-Z0-9_.-]*") || "default".equals(name) || handler == null) {
            throw new IllegalArgumentException("A result handler requires a name other than 'default' and an implementation");
        }

        this.resultHandlers.put(name, handler);
        return this;
    }

    public DatawayConfig dataAccessLayer(ApiDataAccessLayer dataAccessLayer) {
        this.dataAccessLayer = dataAccessLayer;
        return this;
    }

    /** Overrides the table or snapshot entity name. Unspecified entities keep their defaults. */
    public DatawayConfig tableMapping(EntityType entityType, String tableName) {
        this.tableMappings.put(entityType, tableName);
        return this;
    }

    /** Overrides the storage field name within one entity without changing its logical meaning. */
    public DatawayConfig fieldMapping(EntityType entityType, FieldDef field, String fieldName) {
        this.fieldMappings.computeIfAbsent(entityType, type -> new EnumMap<>(FieldDef.class)).put(field, fieldName);
        return this;
    }

    public DatawayConfig identityProvider(IdentityProvider provider) {
        this.identityProvider = provider;
        return this;
    }

    public DatawayConfig authorizationCheck(AuthorizationCheck check) {
        this.authorizationCheck = check;
        return this;
    }

    public DatawayConfig adminInterceptor(AdminInterceptor interceptor) {
        this.adminInterceptors.add(interceptor);
        return this;
    }

    /** Sets the upload cache directory; null uses the system temporary directory. */
    public DatawayConfig uploadTempDirectory(Path directory) {
        this.uploadTempDirectory = directory;
        return this;
    }

    /** Sets the per-file memory threshold in bytes; zero spills every non-empty file to disk. */
    public DatawayConfig uploadMemoryThreshold(int bytes) {
        this.uploadMemoryThreshold = bytes;
        return this;
    }

    public DatawayConfig documentTitle(String title) {
        this.documentTitle = title;
        return this;
    }

    public DatawayConfig documentVersion(String version) {
        this.documentVersion = version;
        return this;
    }

    /** Sets the public API base URL, either an absolute HTTP(S) URL or a root-relative path. */
    public DatawayConfig documentServer(String server) {
        this.documentServer = server;
        return this;
    }

    public DatawayConfig finder(Finder finder) {
        this.finder = finder != null ? finder : new DatawayFinder();
        return this;
    }

    public DatawayConfig resourceLoader(ResourceLoader loader) {
        this.defaultFinder().setResourceLoader(loader);
        return this;
    }

    public DatawayConfig classLoader(ClassLoader loader) {
        this.defaultFinder().setClassLoader(loader);
        return this;
    }

    private DatawayFinder defaultFinder() {
        if (this.finder instanceof DatawayFinder datawayFinder) {
            return datawayFinder;
        }

        throw new IllegalStateException("Configure loaders on the custom Finder, or use DatawayFinder");
    }

    public DatawayConfig customizeScope(CustomizeScope scope) {
        this.customizeScope = scope;
        return this;
    }

    /** Selects the default result handler; an API can override it through its options. */
    public DatawayConfig defaultResultHandler(String name) {
        this.defaultResultHandler = name;
        return this;
    }

    public DatawayConfig wrapAllParameters(boolean wrapAllParameters) {
        this.wrapAllParameters = wrapAllParameters;
        return this;
    }

    public DatawayConfig wrapParameterName(String wrapParameterName) {
        this.wrapParameterName = wrapParameterName;
        return this;
    }

    public DatawayConfig configureHost(Consumer<HostConfiguration> c) {
        this.hostCustomizers.add(c);
        return this;
    }

    public DatawayConfig configureQuery(Consumer<QueryBuilder> c) {
        this.queryCustomizers.add(c);
        return this;
    }

    public DatawayConfig apiInterceptor(ApiInterceptor interceptor) {
        this.apiInterceptors.add(interceptor);
        return this;
    }

    public DatawayConfig function(String name, Udf function) {
        return this.configureQuery(b -> b.addShareVar(name, () -> function));
    }

    public DatawayConfig library(String namespace, Map<String, Udf> functions) {
        // Keep registered functions stable when the source is loaded later.
        Map<String, Udf> copy = Map.copyOf(functions);
        return this.importSource(namespace, () -> (UdfSource) f -> () -> copy);
    }

    public DatawayConfig importSource(String name, Supplier<?> provider) {
        return this.configureHost(h -> h.addImport(name, provider));
    }

    /** Registers a host attachment when Dataway is created. */
    public <T> DatawayConfig attachment(Class<T> attachmentType, T attachment) {
        return this.configureHost(h -> h.addAttachment(attachmentType, attachment));
    }

    public DatawayConfig fragment(String name, Supplier<? extends FragmentProcess> provider) {
        return this.configureHost(h -> h.addFragment(name, provider));
    }

    /** Creates Dataway with the current configuration. */
    public Dataway createDataway() {
        return new Dataway(this);
    }

    //
    //
    //

    /** Lets adapters resolve host storage only when none was explicitly supplied. */
    public ApiDataAccessLayer getDataAccessLayer() {
        return this.dataAccessLayer;
    }

    public Map<EntityType, String> getTableMappings() {
        return this.tableMappings;
    }

    public Map<EntityType, Map<FieldDef, String>> getFieldMappings() {
        return this.fieldMappings;
    }

    public IdentityProvider getIdentityProvider() {
        return this.identityProvider;
    }

    public AuthorizationCheck getAuthorizationCheck() {
        return this.authorizationCheck;
    }

    public List<AdminInterceptor> getAdminInterceptors() {
        return this.adminInterceptors;
    }

    public Path getUploadTempDirectory() {
        return this.uploadTempDirectory;
    }

    public int getUploadMemoryThreshold() {
        return this.uploadMemoryThreshold;
    }

    public String getDocumentTitle() {
        return this.documentTitle;
    }

    public String getDocumentVersion() {
        return this.documentVersion;
    }

    public String getDocumentServer() {
        return this.documentServer;
    }

    public Finder getFinder() {
        return this.finder;
    }

    public ResourceLoader getResourceLoader() {
        return this.finder.getResourceLoader();
    }

    public ClassLoader getClassLoader() {
        return this.finder.getClassLoader();
    }

    public CustomizeScope getCustomizeScope() {
        return this.customizeScope;
    }

    public String getDefaultResultHandler() {
        return this.defaultResultHandler;
    }

    public Map<String, ResultHandler> getResultHandlers() {
        return this.resultHandlers;
    }

    public boolean isWrapAllParameters() {
        return this.wrapAllParameters;
    }

    public String getWrapParameterName() {
        return this.wrapParameterName;
    }

    public List<Consumer<HostConfiguration>> getHostCustomizers() {
        return this.hostCustomizers;
    }

    public List<Consumer<QueryBuilder>> getQueryCustomizers() {
        return this.queryCustomizers;
    }

    public List<ApiInterceptor> getApiInterceptors() {
        return this.apiInterceptors;
    }
}
