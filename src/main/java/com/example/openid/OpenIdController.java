package com.example.openid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class OpenIdController {

    private final RsaKeyService rsaKeyService;
    private final TokenService tokenService;

    @Value("${openid.issuer-url}")
    private String issuerUrl;

    public OpenIdController(RsaKeyService rsaKeyService, TokenService tokenService) {
        this.rsaKeyService = rsaKeyService;
        this.tokenService  = tokenService;
    }

    /** Standard OpenID Connect discovery document */
    @GetMapping(value = "/.well-known/openid-configuration", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> openIdConfiguration() {
        String base = issuerUrl.endsWith("/") ? issuerUrl.substring(0, issuerUrl.length() - 1) : issuerUrl;
        String iss  = base + "/";

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("issuer",                                iss);
        doc.put("jwks_uri",                              base + "/jwks");
        doc.put("token_endpoint",                        base + "/token/generate");
        doc.put("authorization_endpoint",                base + "/authorize");
        doc.put("userinfo_endpoint",                     base + "/userinfo");
        doc.put("response_types_supported",              List.of("code", "token", "id_token"));
        doc.put("subject_types_supported",               List.of("public"));
        doc.put("id_token_signing_alg_values_supported", List.of("RS256"));
        doc.put("scopes_supported",                      List.of("openid", "email", "profile"));
        doc.put("token_endpoint_auth_methods_supported", List.of("client_secret_basic", "client_secret_post"));
        doc.put("claims_supported",                      List.of(
                "sub", "iss", "aud", "exp", "iat", "nbf", "jti",
                "email", "email_verified", "name", "given_name", "family_name",
                "preferred_username", "groups", "scope",
                "realm_access", "resource_access",
                "userId", "buId", "client_id", "azp",
                "auth_strategy", "grant_type"
        ));
        doc.put("grant_types_supported",                 List.of("authorization_code", "implicit"));
        return doc;
    }

    /** JWKS endpoint — returns the public key used to verify tokens */
    @GetMapping(value = "/jwks", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> jwks() {
        return rsaKeyService.toJwkSet();
    }

    /**
     * Token generation endpoint.
     * POST /token/generate with an optional JSON body (TokenRequest) to customise user claims.
     * GET  /token/generate uses all defaults.
     */
    @RequestMapping(value = "/token/generate",
                    method = {RequestMethod.GET, RequestMethod.POST},
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> generateToken(
            @RequestBody(required = false) TokenRequest request) {

        if (request == null) {
            request = new TokenRequest();
        }
        String token = tokenService.generateToken(request);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", token);
        response.put("token_type",   "Bearer");
        response.put("expires_in",   3600);
        return response;
    }
}
