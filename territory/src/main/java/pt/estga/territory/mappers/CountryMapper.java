package pt.estga.territory.mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import pt.estga.territory.dtos.CountryRequestDto;
import pt.estga.territory.entities.Country;

@Mapper(componentModel = "spring")
public interface CountryMapper {

    Country toEntity(CountryRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromRequest(CountryRequestDto dto, @MappingTarget Country entity);

    void update(Country source, @MappingTarget Country target);
}
