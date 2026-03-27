package pt.estga.file.models;

import java.nio.file.Path;

/**
 * Simple data carrier for a generated image variant stored in a temporary file.
 */
public record VariantResult(Path file, int width, int height, long size) {
}

