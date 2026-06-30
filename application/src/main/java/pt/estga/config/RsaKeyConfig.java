package pt.estga.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Configuration
public class RsaKeyConfig {

    private final Environment env;
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    public RsaKeyConfig(Environment env) {
        this.env = env;
    }

    @PostConstruct
    public void init() {
        String privateKeyB64 = env.getProperty("JWT_PRIVATE_KEY");
        String publicKeyB64 = env.getProperty("JWT_PUBLIC_KEY");

        if (privateKeyB64 != null && !privateKeyB64.isBlank()
                && publicKeyB64 != null && !publicKeyB64.isBlank()) {
            this.privateKey = parsePrivateKey(privateKeyB64);
            this.publicKey = parsePublicKey(publicKeyB64);
            log.info("RSA key pair loaded from environment");
        } else {
            generateAndLogKeyPair();
        }
    }

    @Bean
    public RSAPrivateKey jwtPrivateKey() {
        return privateKey;
    }

    @Bean
    public RSAPublicKey jwtPublicKey() {
        return publicKey;
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    private void generateAndLogKeyPair() {
        log.warn("JWT_PRIVATE_KEY / JWT_PUBLIC_KEY not set — generating ephemeral key pair");
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair pair = gen.generateKeyPair();
            this.publicKey = (RSAPublicKey) pair.getPublic();
            this.privateKey = (RSAPrivateKey) pair.getPrivate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate RSA key pair", e);
        }
    }

    private static RSAPrivateKey parsePrivateKey(String base64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64.replaceAll("\\s", ""));
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT_PRIVATE_KEY", e);
        }
    }

    private static RSAPublicKey parsePublicKey(String base64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64.replaceAll("\\s", ""));
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT_PUBLIC_KEY", e);
        }
    }
}
