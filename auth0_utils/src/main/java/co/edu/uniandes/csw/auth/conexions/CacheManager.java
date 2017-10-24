/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.edu.uniandes.csw.auth.conexions;

import co.edu.uniandes.csw.auth.model.UserDTO;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Asistente
 */
public class CacheManager {

    private static AuthorizationApi authorization;
    private static AuthenticationApi authentication;
    private static LoadingCache<String, List<String>> permissionsCache;
    private static LoadingCache<String, List<String>> rolesByUserCache;
    private static LoadingCache<String, HttpResponse<String>> rolesCache;
    private static LoadingCache<String, UserDTO> profileCache;
    private static HttpResponse<String> userRoles;
    private static LoadingCache<String, Map<String, List<String>>> permissionsByRoleCache;

    static {
        try {
            authorization = new AuthorizationApi();

        } catch (IOException | UnirestException | JSONException | InterruptedException | ExecutionException ex) {
            Logger.getLogger(CacheManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            authentication = new AuthenticationApi();
        } catch (IOException | UnirestException | JSONException | InterruptedException | ExecutionException ex) {
            Logger.getLogger(CacheManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        permissionsCache = CacheBuilder.newBuilder()
                .maximumSize(10000) // maximum 100 records can be cached
                .expireAfterAccess(30, TimeUnit.MINUTES) // cache will expire after 30 minutes of access
                .build(new CacheLoader<String, List<String>>() { // build the cacheloader

                    @Override
                    public List<String> load(String roleId) throws Exception {
                        if (AuthenticationApi.devPermissions() != null) {
                            return AuthenticationApi.devPermissions();
                        }
                        List<String> rolesId = getAuthorization().getRolesIDPerUser(getUserRoles());
                        List<String> result = new ArrayList<>();
                        Map<String, List<String>> mp = getPermissionsByRoleCache().get("s");

                        for (String s : rolesId) {
                            for (String rr : mp.get(s)) {
                                result.add(rr);
                            }
                        }
                        return result;
                    }
                });

        setPermissionsByRoleCache(CacheBuilder.newBuilder()
                .maximumSize(10000) // maximum 100 records can be cached
                .expireAfterAccess(30, TimeUnit.MINUTES) // cache will expire after 30 minutes of access
                .build(new CacheLoader<String, Map<String, List<String>>>() { // build the cacheloader
                    @Override
                    public Map<String, List<String>> load(String k) throws Exception {
                        return getAuthorization().getPermissionsPerRole(); //To change body of generated methods, choose Tools | Templates.
                    }

                }));

        rolesByUserCache = CacheBuilder.newBuilder()
                .maximumSize(10000) // maximum 100 records can be cached
                .expireAfterAccess(30, TimeUnit.MINUTES) // cache will expire after 30 minutes of access
                .build(new CacheLoader<String, List<String>>() { // build the cacheloader

                    @Override
                    public List<String> load(String userId) throws Exception {
                        List<String> ls = new ArrayList<>();
                        if (AuthenticationApi.devPermissions() != null) {
                            ls.add("admin");
                            return ls;
                        }
                        setUserRoles(getAuthorization().authorizationGetUserRoles(userId));
                        return getAuthorization().getRoles(new JSONArray(getUserRoles().getBody()));
                    }
                });

        rolesCache = CacheBuilder.newBuilder()
                .maximumSize(10000) // maximum 100 records can be cached
                .expireAfterAccess(30, TimeUnit.MINUTES) // cache will expire after 30 minutes of access
                .build(new CacheLoader<String, HttpResponse<String>>() { // build the cacheloader
                    @Override
                    public HttpResponse<String> load(String userId) throws Exception {

                        return getAuthorization().authorizationGetRoles();
                    }
                });
        profileCache = CacheBuilder.newBuilder()
                .maximumSize(10000) // maximum 100 records can be cached
                .expireAfterAccess(30, TimeUnit.MINUTES) // cache will expire after 30 minutes of access
                .build(new CacheLoader<String, UserDTO>() { // build the cacheloader

                    @Override
                    public UserDTO load(String userId) throws Exception {
                        if (AuthenticationApi.devPermissions() != null) {
                            UserDTO devUser = new UserDTO();
                            devUser.setEmail("uniandes@uniandes.com");
                            devUser.setGivenName("alumno");
                            devUser.setMiddleName("gestion");
                            devUser.setSurName("proyectos");
                            devUser.setUserName("development");
                            return devUser;
                        } else {
                            HttpResponse<String> resp = getAuthentication().managementGetUser(userId);
                            JSONObject json = new JSONObject(resp.getBody());
                            return new UserDTO(json.getJSONObject("user_metadata"));
                        }
                    }
                });

    }

    public static int cacheInit() throws UnirestException, JSONException, InterruptedException, ExecutionException, IOException {
        if (AuthenticationApi.devPermissions() == null) {
            getRolesCache().get("roles");
            JSONArray jarray = getAuthorization().getGroups().getJSONArray("groups").getJSONObject(0).getJSONArray("members");
            int k = 0;

            for (; k < jarray.length(); k++) {

                Logger.getAnonymousLogger().info("cargando perfil para usuario con id ".concat(jarray.get(k).toString()));
                getProfileCache().get(jarray.get(k).toString());
                Logger.getAnonymousLogger().info("cargando roles para usuario con id  ".concat(jarray.get(k).toString()));
                getRolesByUserCache().get(jarray.get(k).toString());
                Logger.getAnonymousLogger().info("cargando permisos para usuario con id  ".concat(jarray.get(k).toString()));
                getPermissionsCache().get(jarray.get(k).toString());

            }
            return k;
        } else {
            Logger.getAnonymousLogger().info("cargando perfil para usuario con id ".concat("auth development"));
            getProfileCache().get("auth");
            Logger.getAnonymousLogger().info("cargando roles para usuario con id  ".concat("auth development"));
            getRolesByUserCache().get("auth");
            Logger.getAnonymousLogger().info("cargando permisos para usuario con id  ".concat("auth development"));
            getPermissionsCache().get("auth");
            return 1;
        }

    }

    /**
     * @return the authorization
     */
    public static AuthorizationApi getAuthorization() {
        return authorization;
    }

    /**
     * @return the authentication
     */
    public static AuthenticationApi getAuthentication() {
        return authentication;
    }

    /**
     * @return the permissionsCache
     */
    public static LoadingCache<String, List<String>> getPermissionsCache() {
        return permissionsCache;
    }

    /**
     * @return the rolesByUserCache
     */
    public static LoadingCache<String, List<String>> getRolesByUserCache() {
        return rolesByUserCache;
    }

    /**
     * @return the rolesCache
     */
    public static LoadingCache<String, HttpResponse<String>> getRolesCache() {
        return rolesCache;
    }

    /**
     * @return the profileCache
     */
    public static LoadingCache<String, UserDTO> getProfileCache() {
        return profileCache;
    }

    /**
     * @return the userRoles
     */
    public static HttpResponse<String> getUserRoles() {
        return userRoles;
    }

    /**
     * @param aUserRoles the userRoles to set
     */
    public static void setUserRoles(HttpResponse<String> aUserRoles) {
        userRoles = aUserRoles;
    }

    /**
     * @return the roleList
     */
    /**
     * @return the permissionsByRoleCache
     */
    public static LoadingCache<String, Map<String, List<String>>> getPermissionsByRoleCache() {
        return permissionsByRoleCache;
    }

    /**
     * @param aPermissionsByRoleCache the permissionsByRoleCache to set
     */
    public static void setPermissionsByRoleCache(LoadingCache<String, Map<String, List<String>>> aPermissionsByRoleCache) {
        permissionsByRoleCache = aPermissionsByRoleCache;
    }

    /**
     * @return the isInitialized
     */
}
