package pt.estga.support.mappers;

import org.springframework.stereotype.Component;
import pt.estga.support.dtos.ContactRequestDto;
import pt.estga.support.entities.ContactRequest;

@Component
public class ContactRequestMapper {

    public ContactRequest toEntity(ContactRequestDto dto) {
        if (dto == null) return null;
        ContactRequest entity = new ContactRequest();
        entity.setName(dto.name());
        entity.setEmail(dto.email());
        entity.setSubject(dto.subject());
        entity.setMessage(dto.message());
        return entity;
    }
}
