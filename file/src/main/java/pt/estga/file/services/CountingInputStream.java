package pt.estga.file.services;

import lombok.Getter;
import org.jspecify.annotations.NonNull;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream wrapper that counts bytes read. This keeps counting logic reusable and testable.
 */
@Getter
public final class CountingInputStream extends FilterInputStream {

    /**
     * -- GETTER --
     *  Returns number of bytes read through this stream.
     */
    private long count;

    public CountingInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        int result = super.read();
        if (result != -1) {
            count++;
        }
        return result;
    }

    @Override
    public int read(byte @NonNull [] b, int off, int len) throws IOException {
        int result = super.read(b, off, len);
        if (result != -1) {
            count += result;
        }
        return result;
    }

}

