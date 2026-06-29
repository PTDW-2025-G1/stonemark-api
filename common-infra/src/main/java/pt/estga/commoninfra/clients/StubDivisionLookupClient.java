package pt.estga.commoninfra.clients;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import pt.estga.commoncore.interfaces.DivisionLookupClient;
import pt.estga.commoncore.models.DivisionRef;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Placeholder implementation used until the geo service HTTP client is provided.
 * Returns empty results so the application runs without the external dependency.
 * Replace with the real client (or mark it {@code @Primary}) once the API schema lands.
 */
@Component
@ConditionalOnMissingBean(DivisionLookupClient.class)
public class StubDivisionLookupClient implements DivisionLookupClient {

    @Override
    public Optional<DivisionRef> findByCode(String code) {
        return Optional.empty();
    }

    @Override
    public Map<String, DivisionRef> findByCodes(Collection<String> codes) {
        return Map.of();
    }

    @Override
    public Optional<DivisionRef> resolveByCoordinates(double latitude, double longitude) {
        return Optional.empty();
    }
}
