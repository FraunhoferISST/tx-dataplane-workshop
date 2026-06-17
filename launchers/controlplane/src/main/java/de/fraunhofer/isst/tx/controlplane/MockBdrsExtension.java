package de.fraunhofer.isst.tx.controlplane;

import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;

public class MockBdrsExtension implements ServiceExtension {

    private static final String API_CONTEXT = "data";
    private static final long FIVE_MINUTES = 1000 * 60 * 5;

    @Provider
    public BdrsClient bdrsClient() {
        return new MockBdrsClient();
    }
}
