package pt.estga.territory.dtos;

public record CountryRequestDto(
        String code,
        String name,
        Boolean active
) { }
