package pt.estga.support.mappers;

import pt.estga.support.dtos.ContactRequestDto;
import pt.estga.support.entities.ContactRequest;

public class ContactRequestMapper {

    private ContactRequestMapper() {}

    public static ContactRequest toEntity(ContactRequestDto dto) {
        if (dto == null) return null;
        ContactRequest entity = new ContactRequest();
        entity.setName(dto.name());
        entity.setEmail(dto.email());
        entity.setSubject(dto.subject());
        entity.setMessage(dto.message());
        return entity;
    }
}
