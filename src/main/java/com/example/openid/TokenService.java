package com.example.openid;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TokenService {

    private final RsaKeyService rsaKeyService;

    @Value("${openid.issuer-url}")
    private String issuerUrl;

    @Value("${openid.default-audience}")
    private String defaultAudience;

    @Value("${openid.default-client-id}")
    private String defaultClientId;

    @Value("${openid.default-bu-id}")
    private String defaultBuId;

    public TokenService(RsaKeyService rsaKeyService) {
        this.rsaKeyService = rsaKeyService;
    }

    /**
     * Generates a signed RS256 JWT with the required claims.
     * Times (iat, nbf, exp) are always computed fresh: iat = now, exp = now + 1 hour.
     */
    public String generateToken(TokenRequest request) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(3600);

        String sub    = nvl(request.getSub(),              "6a05034ce82d5dee7310cb31");
        String email  = nvl(request.getEmail(),            "amanda.bertolini-ext@jci2.com");
        String given  = nvl(request.getGivenName(),        "Amanda");
        String family = nvl(request.getFamilyName(),       "Bertolini");
        String name   = nvl(request.getName(),             given + " " + family);
        String userId = nvl(request.getUserId(),           "51df71ca-783f-47f8-bd42-2039067afffe");
        String buId   = nvl(request.getBuId(),             defaultBuId);
        String aud    = nvl(request.getAudience(),         defaultAudience);
        String clientId = nvl(request.getClientId(),       defaultClientId);
        List<String> groups = request.getGroups() != null ? request.getGroups()
                : List.of("API Developer", "SSCustomerAdminDev");

        Map<String, Object> realmAccess = new LinkedHashMap<>();
        realmAccess.put("roles", List.of(
                "API Developer",
                "default-roles-delta-customer-bu",
                "offline_access",
                "uma_authorization",
                "Customer Admin"
        ));

        Map<String, Object> accountRoles = new LinkedHashMap<>();
        accountRoles.put("roles", List.of("manage-account", "manage-account-links", "view-profile"));
        Map<String, Object> resourceAccess = new LinkedHashMap<>();
        resourceAccess.put("account", accountRoles);

        String iss = issuerUrl;

        return Jwts.builder()
                .header().add("kid", rsaKeyService.getKeyId()).and()
                .issuer(iss)
                .subject(sub)
                .audience().add(aud).and()
                .issuedAt(Date.from(now))
                .notBefore(Date.from(now))
                .expiration(Date.from(exp))
                .id(UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                // Custom claims
                .claim("auth_strategy",        "Authorization Code")
                .claim("azp",                  clientId)
                .claim("buId",                 buId)
                .claim("client_id",            clientId)
                .claim("email",                email)
                .claim("email_verified",       true)
                .claim("family_name",          family)
                .claim("given_name",           given)
                .claim("grant_type",           "Authorization Code")
                .claim("groups",               groups)
                .claim("name",                 name)
                .claim("preferred_username",   email)
                .claim("realm_access",         realmAccess)
                .claim("resource_access",      resourceAccess)
                .claim("scope",                "email profile")
                .claim("userId",               userId)
                .signWith(rsaKeyService.getKeyPair().getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    private static String nvl(String val, String fallback) {
        return (val != null && !val.isBlank()) ? val : fallback;
    }
}
