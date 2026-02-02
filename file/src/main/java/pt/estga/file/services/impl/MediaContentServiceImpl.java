package pt.estga.file.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import pt.estga.file.services.FileStorageService;
import pt.estga.file.services.MediaContentService;
import pt.estga.shared.exceptions.FileStorageException;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaContentServiceImpl implements MediaContentService {

    private final FileStorageService fileStorageService;

    @Override
    public String saveContent(InputStream fileStream, String filename) throws IOException {
        // TODO: Integrate CDN invalidation here if needed
        return fileStorageService.storeFile(fileStream, filename);
    }

    @Override
    public Resource loadContent(String storagePath) {
        return fileStorageService.loadFile(storagePath);
    }

    @Override
    public byte[] getContentBytes(String storagePath) {
        try {
            Resource resource = loadContent(storagePath);
            return resource.getInputStream().readAllBytes();
        } catch (IOException e) {
            throw new FileStorageException("Could not read file content from path: " + storagePath, e);
        }
    }
}
