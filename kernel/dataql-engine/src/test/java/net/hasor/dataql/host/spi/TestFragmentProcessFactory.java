package net.hasor.dataql.host.spi;

import java.util.Map;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.host.HostContext;
import net.hasor.dataql.kernel.FragmentProcess;

public class TestFragmentProcessFactory implements FragmentProcessFactory {
    @Override
    public String[] getNames() {
        return new String[] { "testFragment", "testFragmentAlias" };
    }

    @Override
    public FragmentProcess create(String name, HostContext context) {
        return new FragmentProcess() {
            @Override
            public Object runFragment(Hints hints, Map<String, Object> params, String fragmentString) {
                return fragmentString;
            }
        };
    }
}
