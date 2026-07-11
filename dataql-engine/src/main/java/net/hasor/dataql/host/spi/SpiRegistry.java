package net.hasor.dataql.host.spi;

import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.hasor.cobble.ref.LinkedCaseInsensitiveMap;
import net.hasor.dataql.host.HostContext;
import net.hasor.dataql.kernel.FragmentProcess;

/**
 * 片段和导入注册器，支持 SPI 自动发现和手动注册。
 */
public class SpiRegistry {
    private final    HostContext              context;
    private final    Map<String, Supplier<?>> fragmentMap = new LinkedCaseInsensitiveMap<>();
    private final    Map<String, Supplier<?>> importMap   = new ConcurrentHashMap<>();
    private volatile boolean                  providersLoaded;
    private volatile boolean                  providerLoading;

    public SpiRegistry(HostContext context) {
        this.context = Objects.requireNonNull(context, "context is null.");
    }

    // ========== SPI 加载 ==========

    public synchronized void loadProviders() {
        if (this.providersLoaded) {
            return;
        }

        this.providersLoaded = true;
        this.providerLoading = true;
        try {
            ClassLoader classLoader = this.context.getClassLoader();
            ServiceLoader<FragmentProcessFactory> fragLoader = classLoader != null//
                    ? ServiceLoader.load(FragmentProcessFactory.class, classLoader)//
                    : ServiceLoader.load(FragmentProcessFactory.class);
            fragLoader.forEach(factory -> this.addFragment(factory.getName(), () -> factory.create(this.context)));

            ServiceLoader<UdfSourceFactory> udfLoader = classLoader != null//
                    ? ServiceLoader.load(UdfSourceFactory.class, classLoader)//
                    : ServiceLoader.load(UdfSourceFactory.class);
            udfLoader.forEach(factory -> this.addImport(factory.getResourceName(), () -> factory.create(this.context)));
        } catch (RuntimeException | Error e) {
            this.providersLoaded = false;
            throw e;
        } finally {
            this.providerLoading = false;
        }
    }

    // ========== 手动注册 ==========

    public void addFragment(String name, Supplier<? extends FragmentProcess> provider) {
        if (!this.providerLoading) {
            this.loadProviders();
        }
        if (!this.providerLoading || !this.fragmentMap.containsKey(name)) {
            this.fragmentMap.put(name, provider);
        }
    }

    public void addImport(String name, Supplier<?> provider) {
        if (!this.providerLoading) {
            this.loadProviders();
        }
        if (!this.providerLoading || !this.importMap.containsKey(name)) {
            this.importMap.put(name, provider);
        }
    }

    // ========== 查找 ==========

    public FragmentProcess findFragment(String name) {
        this.loadProviders();
        Supplier<?> supplier = this.fragmentMap.get(name);
        return supplier != null ? (FragmentProcess) supplier.get() : null;
    }

    public Object findImport(String name) {
        this.loadProviders();
        Supplier<?> supplier = this.importMap.get(name);
        return supplier != null ? supplier.get() : null;
    }

    // ========== 内部用到的方法 ==========

    public boolean isProviderLoading() {
        return this.providerLoading;
    }
}
