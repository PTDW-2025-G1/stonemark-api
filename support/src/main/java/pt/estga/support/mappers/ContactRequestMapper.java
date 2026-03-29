package pt.estga.support.mappers;

import org.mapstruct.Mapper;
import pt.estga.support.dtos.ContactRequestDto;
import pt.estga.support.entities.ContactRequest;

@Mapper(componentModel = "spring")
public interface ContactRequestMapper {

    ContactRequest toEntity(ContactRequestDto dto);

}
