package pt.estga.file.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import pt.estga.file.services.storage.FileStorageService;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaContentService {

    private final FileStorageService fileStorageService;

    public String saveContent(InputStream fileStream, String filename) throws IOException {
        // TODO: Integrate CDN invalidation here if needed
        return fileStorageService.storeFile(fileStream, filename);
    }

    public Resource loadContent(String storagePath) {
        return fileStorageService.loadFile(storagePath);
    }
}
