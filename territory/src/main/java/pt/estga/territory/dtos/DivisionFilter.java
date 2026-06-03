package pt.estga.territory.dtos;

public record DivisionFilter(
        String name,
        Long parentId,
        Boolean rootOnly
) {}
