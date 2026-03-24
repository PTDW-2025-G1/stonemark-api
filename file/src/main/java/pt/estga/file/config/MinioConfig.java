package pt.estga.file.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "storage.provider", havingValue = "minio")
public class MinioConfig {

    private final MinioProperties props;

    public MinioConfig(MinioProperties props) {
        this.props = props;
    }

    @Bean
    public MinioClient minioClient() {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();
        createBucketIfNotExists(minioClient);
        return minioClient;
    }

    private void createBucketIfNotExists(MinioClient minioClient) {
        try {
            if (minioClient.bucketExists(BucketExistsArgs.builder().bucket(props.getBucketName()).build())) {
                return;
            }
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(props.getBucketName()).build());
        } catch (MinioException e) {
            throw new RuntimeException("Could not initialize MinIO bucket", e);
        }
    }
}
