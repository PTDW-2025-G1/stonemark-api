package pt.estga.commoncore.interfaces;

import pt.estga.commoncore.models.DivisionRef;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Port to the external geo service that owns administrative divisions.
 * Implementations call the geo API to resolve division codes to names and to
 * reverse-geocode coordinates. A no-op stub is used until the real client is wired.
 */
public interface DivisionLookupClient {

    /**
     * Resolve a single division by its code.
     */
    Optional<DivisionRef> findByCode(String code);

    /**
     * Resolve several divisions in one call, keyed by code. Intended for enriching
     * list responses without issuing one request per item.
     */
    Map<String, DivisionRef> findByCodes(Collection<String> codes);

    /**
     * Reverse-geocode a coordinate to the lowest containing division.
     */
    Optional<DivisionRef> resolveByCoordinates(double latitude, double longitude);
}
