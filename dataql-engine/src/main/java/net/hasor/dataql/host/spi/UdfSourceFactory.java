package net.hasor.dataql.host.spi;

import net.hasor.dataql.domain.UdfSource;
import net.hasor.dataql.host.HostContext;

/** SPI factory for a UDF source type. */
public interface UdfSourceFactory {

    String getResourceName();

    UdfSource create(HostContext context);
}
