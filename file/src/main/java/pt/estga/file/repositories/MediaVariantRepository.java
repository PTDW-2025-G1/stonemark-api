package pt.estga.file.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.entities.MediaVariant;
import pt.estga.file.enums.MediaVariantType;

import java.util.Optional;

public interface MediaVariantRepository extends JpaRepository<MediaVariant, Long> {

    boolean existsByMediaFileAndType(MediaFile mediaFile, MediaVariantType type);
    
    Optional<MediaVariant> findByMediaFileIdAndType(Long mediaId, MediaVariantType type);

}
