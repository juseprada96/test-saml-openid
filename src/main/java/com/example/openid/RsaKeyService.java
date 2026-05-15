package com.example.openid;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads a static RSA-2048 key pair from classpath PEM files (keys/private.pem, keys/public.pem)
 * and loads the matching self-signed X.509 certificate from keys/cert.pem so the
 * /protocol/openid-connect/certs endpoint returns x5c / x5t / x5t#S256 fields that are
 * consistent with the hardcoded response in the /docs static site.
 */
@Component
public class RsaKeyService {

    private final KeyPair keyPair;
    private final X509Certificate certificate;
    private final String keyId = "sample-key-1";

    public RsaKeyService() {
        try {
            this.keyPair     = new KeyPair(loadPublicKey(), loadPrivateKey());
            this.certificate = loadCertificate();
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

    private X509Certificate loadCertificate() throws Exception {
        try (InputStream is = new ClassPathResource("keys/cert.pem").getInputStream()) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        }
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public String getKeyId() {
        return keyId;
    }

    /**
     * Returns the public key in standard JWK format (no x5c).
     */
    public Map<String, Object> toJwk() {
        RSAPublicKey pub = (RSAPublicKey) keyPair.getPublic();
        byte[] modBytes = stripLeadingZero(pub.getModulus().toByteArray());
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

    /**
     * Returns the public key in JWK format with x5c / x5t / x5t#S256 fields
     * (Keycloak-style /protocol/openid-connect/certs response).
     */
    public Map<String, Object> toJwkWithCert() {
        try {
            RSAPublicKey pub  = (RSAPublicKey) keyPair.getPublic();
            byte[] certDer    = certificate.getEncoded();
            byte[] modBytes   = stripLeadingZero(pub.getModulus().toByteArray());

            String x5c        = Base64.getEncoder().encodeToString(certDer);
            String x5t        = Base64.getUrlEncoder().withoutPadding()
                                      .encodeToString(MessageDigest.getInstance("SHA-1").digest(certDer));
            String x5tS256    = Base64.getUrlEncoder().withoutPadding()
                                      .encodeToString(MessageDigest.getInstance("SHA-256").digest(certDer));

            Map<String, Object> jwk = new LinkedHashMap<>();
            jwk.put("kid",     keyId);
            jwk.put("kty",     "RSA");
            jwk.put("alg",     "RS256");
            jwk.put("use",     "sig");
            jwk.put("x5c",     List.of(x5c));
            jwk.put("x5t",     x5t);
            jwk.put("x5t#S256", x5tS256);
            jwk.put("n", Base64.getUrlEncoder().withoutPadding().encodeToString(modBytes));
            jwk.put("e", Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(pub.getPublicExponent().toByteArray()));
            return jwk;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build JWK with certificate", e);
        }
    }

    public Map<String, Object> toJwkSet() {
        Map<String, Object> jwks = new LinkedHashMap<>();
        jwks.put("keys", List.of(toJwk()));
        return jwks;
    }

    public Map<String, Object> toJwkSetWithCerts() {
        Map<String, Object> jwks = new LinkedHashMap<>();
        jwks.put("keys", List.of(toJwkWithCert()));
        return jwks;
    }

    private byte[] stripLeadingZero(byte[] bytes) {
        if (bytes.length > 0 && bytes[0] == 0) {
            return Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return bytes;
    }
}
