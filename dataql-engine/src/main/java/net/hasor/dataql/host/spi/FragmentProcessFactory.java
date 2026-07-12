package net.hasor.dataql.host.spi;

import net.hasor.dataql.host.HostContext;
import net.hasor.dataql.kernel.FragmentProcess;

/** SPI factory for one or more named fragment processes. */
public interface FragmentProcessFactory {

    String[] getNames();

    FragmentProcess create(String name, HostContext context);
}
