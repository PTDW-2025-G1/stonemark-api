package pt.estga.file.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.file.enums.MediaStatus;
import pt.estga.file.enums.StorageProvider;
import pt.estga.shared.audit.CreationAuditedEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MediaFile extends CreationAuditedEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String filename;

    /**
     * Original filename provided by the uploader. This field is optional metadata
     * and must not be relied upon for storage naming or core logic. It may be null.
     */
    private String originalFilename;

    private Long size;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StorageProvider storageProvider;

    @Column(nullable = false, length = 1024)
    private String storagePath;

    @Column(length = 512)
    private String providerPublicId;

    @OneToMany(
            mappedBy = "mediaFile",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<MediaVariant> variants = new ArrayList<>();

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MediaStatus status;

    /**
     * Creates a MediaFile instance representing an upload in processing state.
     * This factory prepares the entity with a pre-generated stored filename so
     * downstream storage can proceed without requiring a database-generated id.
     *
     * @param storedFilename unique, safe filename used for storage (typically UUID-based)
     * @param originalFilename optional original filename provided by the user
     * @param storageProvider storage provider for this file
     * @return media file in PROCESSING status
     */
    public static MediaFile createForProcessing(String storedFilename, String originalFilename, StorageProvider storageProvider) {
        return MediaFile.builder()
                .filename(storedFilename)
                .originalFilename(originalFilename)
                .size(0L)
                .storageProvider(storageProvider)
                .storagePath("")
                .status(MediaStatus.PROCESSING)
                .build();
    }

    /**
     * Completes the upload by setting storage metadata and final status. State
     * transitions are encapsulated here to keep entity invariants consistent.
     *
     * @param size number of bytes stored
     * @param storagePath storage provider relative path where the file was saved
     * @param providerPublicId optional provider-specific id (e.g., S3 key)
     * @param finalStatus resulting media status (for example UPLOADED)
     */
    public void completeUpload(long size, String storagePath, String providerPublicId, MediaStatus finalStatus) {
        this.size = size;
        this.storagePath = storagePath;
        this.providerPublicId = providerPublicId;
        this.status = finalStatus;
    }

}
