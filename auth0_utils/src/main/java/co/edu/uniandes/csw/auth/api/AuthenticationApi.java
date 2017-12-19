/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.edu.uniandes.csw.auth.api;

import co.edu.uniandes.csw.auth.model.UserDTO;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Asistente
 */
public class AuthenticationApi {

    /**
     * @return the profileCache
     */
    public static LoadingCache<String, UserDTO> getProfileCache() {
        return profileCache;
    }

    /**
     * @param aProfileCache the profileCache to set
     */
    public static void setProfileCache(LoadingCache<String, UserDTO> aProfileCache) {
        profileCache = aProfileCache;
    }

    private Properties prop = new Properties();
    private InputStream input = null;
    private static HttpServletResponse rsp;
    private static LoadingCache<String, UserDTO> profileCache;
    private static final String path = System.getenv("AUTH0_PROPERTIES");

    public AuthenticationApi() throws IOException, UnirestException, JSONException, InterruptedException, ExecutionException {

        try {
            input = new FileInputStream(path);
            try {
                prop.load(input);
            } catch (IOException ex) {
                Logger.getLogger(AuthenticationApi.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AuthenticationApi.class.getName()).log(Level.SEVERE, null, ex);
        }
        profileCache = CacheBuilder.newBuilder()
                .maximumSize(10000) // maximum 100 records can be cached
                .expireAfterAccess(30, TimeUnit.MINUTES) // cache will expire after 30 minutes of access
                .build(new CacheLoader<String, UserDTO>() { // build the cacheloader
                    @Override
                    public UserDTO load(String userId) throws Exception {
                        JSONObject jo = new JSONObject(managementGetUser(userId).getBody());
                        return new UserDTO(jo);
                    }
                });
    }

    public HttpResponse<String> managementToken() throws UnirestException {
        Unirest.setTimeouts(0, 0);
        return Unirest.post(getProp().getProperty("accessToken").trim())
                .header("content-type", "application/json")
                .body("{\"grant_type\":\"client_credentials\","
                        + "\"client_id\": \"" + getProp().getProperty("managementClientId").trim() + "\","
                        + "\"client_secret\": \"" + getProp().getProperty("managementSecretKey").trim() + "\","
                        + "\"audience\":\"" + getProp().getProperty("managementAudience").trim() + "\"}").asString();

    }
    /*
    accessToken
    managementClientId
    managementScretKey
    managementAudience
    users
    grantType
    authenticationClientId
    authenticationClientScretKey
    signUp
    groupName
    userInfo
    
    
    */

    public HttpResponse<String> managementGetUser(String id) throws UnirestException, JSONException {
        Unirest.setTimeouts(0, 0);
        return Unirest.get(getProp().getProperty("users").trim() + "/" + id.replace("|", "%7C"))
                .header("content-type", "application/json")
                .header("Authorization", "Bearer " + getManagementAccessToken()).asString();
    }

    public HttpResponse<String> authenticationToken(UserDTO dto) throws UnirestException {
        Unirest.setTimeouts(0, 0);
        return Unirest.post(getProp().getProperty("accessToken").trim())
                .header("content-type", "application/json")
                .body("{"
                        + "\"grant_type\":\"" + getProp().getProperty("grantType").trim() + "\","
                        + "\"username\":\"" + dto.getUserName() + "\","
                        + "\"password\":\"" + dto.getPassword() + "\","
                        + "\"client_id\":\"" + getProp().getProperty("authenticationClientId").trim() + "\","
                        + "\"client_secret\":\"" + getProp().getProperty("authenticationSecretKey").trim() + "\""
                        + "}").asString();
    }

    public HttpResponse<String> authenticationSignUP(UserDTO dto) throws UnirestException {
        Unirest.setTimeouts(1000, 10000);
        return Unirest.post(getProp().getProperty("signUp").trim())
                .header("content-type", "application/json")
                .body("{\"client_id\":\"" + getProp().getProperty("authenticationClientId").trim() + "\","
                        + "\"email\":\"" + dto.getEmail() + "\","
                        + "\"password\":\"" + dto.getPassword() + "\","
                        + "\"connection\":\"" + getProp().getProperty("connection").trim() + "\","
                        + "\"user_metadata\":{\"given_name\":\"" + dto.getGivenName() + "\","
                        + "\"email\":\"" + dto.getEmail() + "\","
                        + "\"username\":\"" + dto.getUserName() + "\","
                        + "\"roles\":\"" + dto.getRoles() + "\","
                        + "\"group\":\"" + getProp().getProperty("groupName").trim() + "\","
                        + "\"middle_name\":\"" + dto.getMiddleName() + "\","
                        + "\"sur_name\":\"" + dto.getSurName() + "\"}}").asString();

    }

    public HttpResponse<String> authenticationUserInfo(UserDTO dto, HttpServletResponse rsp) throws UnirestException, JSONException {
        Unirest.setTimeouts(0, 0);
        return Unirest.get(getProp().getProperty("userInfo").trim())
                .header("Authorization", "Bearer " + getAuthenticationAccessToken(dto, rsp)).asString();
    }

    public void authenticationLogout() {
        Unirest.setTimeouts(0, 0);
        Unirest.get(getProp().getProperty("logout").trim());
    }

    public String getManagementAccessToken() throws UnirestException, JSONException {
        HttpResponse<String> res = managementToken();
        JSONObject json = new JSONObject(res.getBody());
        return (String) json.get("access_token");
    }

    public String getAuthenticationAccessToken(UserDTO dto, HttpServletResponse rsp) throws UnirestException, JSONException {
        HttpResponse<String> res = authenticationToken(dto);
        JSONObject json = new JSONObject(res.getBody());
        try {
            if (json.get("error_description").equals("Wrong email or password.")) {
                throw new SignatureException("Email o password invalidos");
            }
        } catch (JSONException je) {
            rsp.addHeader("id_token", json.get("id_token").toString());
        }
        return (String) json.get("access_token");
    }
    //get user profile

    public String getSubject(UserDTO dto, HttpServletResponse rsp) throws UnirestException, JSONException {
        HttpResponse<String> res = authenticationUserInfo(dto, rsp);
        JSONObject json = new JSONObject(res.getBody());
        return json.get("sub").toString();
    }

    public void HttpServletResponseBinder(HttpResponse<String> rsp, HttpServletResponse res) throws IOException {
        res.setHeader("content-type", "application/json");
        res.setStatus(rsp.getCode());
        res.getWriter().print(rsp.getBody());
        res.flushBuffer();
    }

    public Jws<Claims> decryptToken(String token) {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        String secret = getProp().getProperty("authenticationSecretKey").trim();
        Key signingKey = new SecretKeySpec(secret.getBytes(), signatureAlgorithm.getJcaName());
        Jws<Claims> j;

        try {
            if (token != null) {

                j = Jwts.parser().setSigningKey(signingKey).parseClaimsJws(token);
            } else {
                throw new SignatureException("no autenticado");
            }
        } catch (SignatureException se) {
            return null;
        }
        return j;
    }

    public Jws<Claims> decryptToken(HttpServletRequest req) {
        Cookie[] cookie = req.getCookies();
        String jwt = null;

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        String secret = getProp().getProperty("authenticationSecretKey").trim();
        Key signingKey = new SecretKeySpec(secret.getBytes(), signatureAlgorithm.getJcaName());
        Jws<Claims> j;
        for (Cookie c : cookie) {
            if ("id_token".equals(c.getName())) {
                jwt = c.getValue();
            }
        }
        try {
            if (jwt != null) {
                j = Jwts.parser().setSigningKey(signingKey).parseClaimsJws(jwt);
            } else {
                throw new SignatureException("no autenticado");
            }
        } catch (SignatureException se) {
            return null;
        }
        return j;
    }

    /**
     * @return the prop
     */
    public Properties getProp() {
        return prop;
    }

    /**
     * @param prop the prop to set
     */
    public void setProp(Properties prop) {
        this.prop = prop;
    }

}
