package pt.estga.file.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.enums.MediaStatus;

import java.time.Instant;
import java.util.List;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

	@Query("select m from MediaFile m where m.status = :status and m.createdAt < :before")
	List<MediaFile> findProcessingOlderThan(@Param("status") MediaStatus status, @Param("before") Instant before);
}
