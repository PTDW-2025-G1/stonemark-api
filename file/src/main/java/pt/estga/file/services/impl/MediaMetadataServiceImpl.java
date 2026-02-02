package pt.estga.file.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.repositories.MediaFileRepository;
import pt.estga.file.services.MediaMetadataService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaMetadataServiceImpl implements MediaMetadataService {

    private final MediaFileRepository mediaFileRepository;

    @Override
    @Transactional
    public MediaFile saveMetadata(MediaFile mediaFile) {
        return mediaFileRepository.save(mediaFile);
    }

    @Override
    public Optional<MediaFile> findById(Long id) {
        return mediaFileRepository.findById(id);
    }
}
