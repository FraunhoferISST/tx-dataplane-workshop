package de.fraunhofer.isst.tx.controlplane;

import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;

public class MockBdrsClient implements BdrsClient {


    @Override
    public String resolveDid(String s) {
        if (s.contains("provider")) {
            return "did:web:provider";
        } else {
            return "did:web:consumer";
        }
    }

    @Override
    public String resolveBpn(String s) {
        if (s.contains("PROVIDER")) {
            return "BPNL0000PROVIDER";
        } else {
            return "BPNL0000CONSUMER";
        }
    }
}
