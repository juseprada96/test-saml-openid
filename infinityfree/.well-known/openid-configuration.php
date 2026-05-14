<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
$base = "http://test-openid-bug.ct.ws";
echo json_encode([
    "issuer"                                => $base . "/",
    "jwks_uri"                              => $base . "/jwks",
    "token_endpoint"                        => $base . "/token/generate",
    "authorization_endpoint"               => $base . "/authorize",
    "userinfo_endpoint"                     => $base . "/userinfo",
    "response_types_supported"              => ["code", "token", "id_token"],
    "subject_types_supported"               => ["public"],
    "id_token_signing_alg_values_supported" => ["RS256"],
    "scopes_supported"                      => ["openid", "email", "profile"],
    "token_endpoint_auth_methods_supported" => ["client_secret_basic", "client_secret_post"],
    "claims_supported"                      => [
        "sub", "iss", "aud", "exp", "iat", "nbf", "jti",
        "email", "email_verified", "name", "given_name", "family_name",
        "preferred_username", "groups", "scope",
        "realm_access", "resource_access",
        "userId", "buId", "client_id", "azp",
        "auth_strategy", "grant_type"
    ],
    "grant_types_supported"                 => ["authorization_code", "implicit"]
], JSON_UNESCAPED_SLASHES | JSON_PRETTY_PRINT);
