package pt.estga.file.dtos;

/**
 * Result for content save operation returning storage path and number of bytes written.
 */
public record SaveResult(String storagePath, long size) {}