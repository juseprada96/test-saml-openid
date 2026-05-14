package com.example.openid;

import java.util.List;

/**
 * Optional request body for /token/generate.
 * Every field is optional — defaults are applied in TokenService.
 */
public class TokenRequest {

    private String sub;
    private String email;
    private String givenName;
    private String familyName;
    private String name;
    private String userId;
    private String buId;
    private String audience;
    private String clientId;
    private List<String> groups;

    public String getSub()          { return sub; }
    public void   setSub(String v)  { this.sub = v; }

    public String getEmail()         { return email; }
    public void   setEmail(String v) { this.email = v; }

    public String getGivenName()          { return givenName; }
    public void   setGivenName(String v)  { this.givenName = v; }

    public String getFamilyName()          { return familyName; }
    public void   setFamilyName(String v)  { this.familyName = v; }

    public String getName()          { return name; }
    public void   setName(String v)  { this.name = v; }

    public String getUserId()          { return userId; }
    public void   setUserId(String v)  { this.userId = v; }

    public String getBuId()          { return buId; }
    public void   setBuId(String v)  { this.buId = v; }

    public String getAudience()          { return audience; }
    public void   setAudience(String v)  { this.audience = v; }

    public String getClientId()          { return clientId; }
    public void   setClientId(String v)  { this.clientId = v; }

    public List<String> getGroups()           { return groups; }
    public void         setGroups(List<String> v) { this.groups = v; }
}
