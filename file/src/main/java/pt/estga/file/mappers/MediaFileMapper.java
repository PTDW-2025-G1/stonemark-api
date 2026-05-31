package pt.estga.file.mappers;

import org.springframework.stereotype.Component;
import pt.estga.file.dtos.MediaFileDto;
import pt.estga.file.entities.MediaFile;

@Component
public class MediaFileMapper {

    public MediaFileDto toDto(MediaFile entity) {
        if (entity == null) return null;
        return new MediaFileDto(
                entity.getId(),
                entity.getFilename(),
                entity.getOriginalFilename(),
                entity.getSize(),
                entity.getStatus() != null ? entity.getStatus().name() : null,
                null
        );
    }
}
