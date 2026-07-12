package net.hasor.dataql.host.spi;

import net.hasor.dataql.host.HostContext;

/** SPI factory for a host-scoped attachment. */
public interface HostAttachmentFactory<T> {

    Class<T> getAttachmentType();

    T create(HostContext context);
}
