package net.hasor.dataql.host.spi;

import net.hasor.dataql.host.HostContext;
import net.hasor.dataql.kernel.FragmentProcess;

/** SPI factory for a named fragment process. */
public interface FragmentProcessFactory {

    String getName();

    FragmentProcess create(HostContext context);
}
