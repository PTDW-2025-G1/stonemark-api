package pt.estga.file.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.repositories.MediaFileRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaMetadataService {

    private final MediaFileRepository mediaFileRepository;

    @Transactional
    public MediaFile saveMetadata(MediaFile mediaFile) {
        return mediaFileRepository.save(mediaFile);
    }

    public Optional<MediaFile> findById(Long id) {
        return mediaFileRepository.findById(id);
    }
}
