package com.example.openid;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads a static RSA-2048 key pair from classpath PEM files (keys/private.pem, keys/public.pem).
 * Using static keys ensures the JWKS hosted on InfinityFree always matches the signing key.
 */
@Component
public class RsaKeyService {

    private final KeyPair keyPair;
    private final String keyId = "sample-key-1";

    public RsaKeyService() {
        try {
            this.keyPair = new KeyPair(loadPublicKey(), loadPrivateKey());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA key pair from classpath", e);
        }
    }

    private PrivateKey loadPrivateKey() throws Exception {
        String pem = readPem("keys/private.pem");
        String b64 = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s+", "");
        byte[] der = Base64.getDecoder().decode(b64);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private PublicKey loadPublicKey() throws Exception {
        String pem = readPem("keys/public.pem");
        String b64 = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s+", "");
        byte[] der = Base64.getDecoder().decode(b64);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    private String readPem(String classpathPath) throws Exception {
        try (InputStream is = new ClassPathResource(classpathPath).getInputStream()) {
            return new String(is.readAllBytes());
        }
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public String getKeyId() {
        return keyId;
    }

    /**
     * Returns the public key in JWK (JSON Web Key) format.
     */
    public Map<String, Object> toJwk() {
        RSAPublicKey pub = (RSAPublicKey) keyPair.getPublic();
        // BigInteger.toByteArray() may include a leading 0x00 sign byte; strip it for RFC 7517-compliant unsigned encoding
        byte[] modBytes = pub.getModulus().toByteArray();
        if (modBytes.length > 0 && modBytes[0] == 0) {
            modBytes = Arrays.copyOfRange(modBytes, 1, modBytes.length);
        }
        Map<String, Object> jwk = new LinkedHashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("kid", keyId);
        jwk.put("n", Base64.getUrlEncoder().withoutPadding().encodeToString(modBytes));
        jwk.put("e", Base64.getUrlEncoder().withoutPadding()
                .encodeToString(pub.getPublicExponent().toByteArray()));
        return jwk;
    }

    public Map<String, Object> toJwkSet() {
        Map<String, Object> jwks = new LinkedHashMap<>();
        jwks.put("keys", List.of(toJwk()));
        return jwks;
    }
}
