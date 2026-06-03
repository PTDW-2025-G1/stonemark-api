package pt.estga.support.dtos;

import pt.estga.support.enums.ContactStatus;

public record ContactRequestFilter(
        ContactStatus status,
        String name,
        String email
) {}
