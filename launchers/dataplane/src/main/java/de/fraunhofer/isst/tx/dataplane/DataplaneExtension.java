package de.fraunhofer.isst.tx.dataplane;

import org.eclipse.edc.api.authentication.filter.JwtValidatorFilter;
import org.eclipse.edc.keys.resolver.JwksPublicKeyResolver;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.rules.IssuerEqualsValidationRule;
import org.eclipse.edc.token.rules.NotBeforeValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;

import java.time.Clock;
import java.util.List;

public class DataplaneExtension implements ServiceExtension {

    private static final String API_CONTEXT = "data";
    private static final long FIVE_MINUTES = 1000 * 60 * 5;

    @Inject
    private WebService webService;
    @Inject
    private PortMappingRegistry portMappingRegistry;
    @Inject
    private KeyParserRegistry keyParserRegistry;
    @Inject
    private TokenValidationService tokenValidationService;
    @Inject
    private Clock clock;

    @Configuration
    private SigletConfig sigletConfig;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var portMapping = new PortMapping(API_CONTEXT, 8888, "/data");
        portMappingRegistry.register(portMapping);

        webService.registerResource(API_CONTEXT, new DataController());
        var resolver = JwksPublicKeyResolver.create(keyParserRegistry, sigletConfig.jwksUrl(), context.getMonitor(), sigletConfig.cacheValidityInMillis());
        webService.registerResource(API_CONTEXT, new JwtValidatorFilter(tokenValidationService, resolver, getRules()));
    }

    private List<TokenValidationRule> getRules() {
        return List.of(
                new IssuerEqualsValidationRule(sigletConfig.expectedIssuer),
                new NotBeforeValidationRule(clock, 0, true),
                new ExpirationIssuedAtValidationRule(clock, 0, false)
        );
    }

    @Settings
    record SigletConfig(
            @Setting(key = "edc.dataplane.iam.siglet.issuer", description = "Issuer of the Siglet server", required = false)
            String expectedIssuer,
            @Setting(key = "edc.dataplane.iam.siglet.jwks.url", description = "Absolute URL where the JWKS of the Siglet server is hosted")
            String jwksUrl,
            @Setting(key = "edc.dataplane.iam.siglet.jwks.cache.validity", description = "Time (in ms) that cached JWKS are cached", defaultValue = "" + FIVE_MINUTES)
            long cacheValidityInMillis
    ) {

    }
}
