/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.edu.uniandes.csw.auth.conexions;

import co.edu.uniandes.csw.auth.model.UserDTO;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.jsonwebtoken.lang.Collections;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AuthorizationApi {

    private Properties prop = new Properties();
    private InputStream input = null;
    private static final String path = System.getenv("AUTH0_PROPERTIES");

    public AuthorizationApi() throws IOException, UnirestException, JSONException, InterruptedException, ExecutionException {

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

    }

    public HttpResponse<String> authorizationAccessToken() throws UnirestException {

        Unirest.setTimeouts(0, 0);
        return Unirest.post(prop.getProperty("accessToken").trim())
                .header("content-type", "application/json")
                .body("{\"grant_type\":\"client_credentials\","
                        + "\"audience\":\"" + prop.getProperty("audience").trim() + "\","
                        + "\"client_id\":\"" + prop.getProperty("managementClientId").trim() + "\","
                        + "\"client_secret\":\"" + prop.getProperty("managementSecretKey").trim() + "\"}").asString();
    }

    public HttpResponse<String> authorizationGetGroups() throws UnirestException, JSONException, InterruptedException, ExecutionException {
        Unirest.setTimeouts(0, 0);
        return Unirest.get(prop.getProperty("authorizationExtension").trim() + "/groups")
                .header("Authorization", "Bearer " + getAuthorizationAccessToken()).asString();

    }

    public HttpResponse<String> authorizationGetGroupRoles() throws UnirestException, JSONException, InterruptedException, ExecutionException {
        Unirest.setTimeouts(0, 0);
        return Unirest.get(prop.getProperty("authorizationExtension").trim() + "/groups/" + getGroupID(getGroups()) + "/roles")
                .header("Authorization", "Bearer " + getAuthorizationAccessToken()).asString();
    }

    public void authorizationAddUserToGroup(String userId) throws UnirestException, JSONException, InterruptedException, ExecutionException {
        String user = "auth0%7C" + userId;
        Unirest.setTimeouts(0, 0);
        Unirest.patch(prop.getProperty("authorizationExtension").trim() + "/users/" + user + "/groups")
                .header("Authorization", "Bearer " + getAuthorizationAccessToken())
                .body("[\"" + getGroupID(getGroups()) + "\"]").asString();
    }

    public void authorizationAddRoleToUser(String userId, String roles) throws UnirestException, JSONException, InterruptedException, ExecutionException {
        String user = "auth0%7C" + userId;
        Unirest.setTimeouts(0, 0);
        HttpResponse<String> rsp = Unirest.patch(prop.getProperty("authorizationExtension").trim() + "/users/" + user + "/roles")
                .header("Authorization", "Bearer " + getAuthorizationAccessToken())
                .body("[\"" + roles + "\"]").asString();
    }

    public HttpResponse<String> authorizationGetRoles() throws UnirestException, JSONException, InterruptedException, ExecutionException {
        Unirest.setTimeouts(0, 0);
        return Unirest.get(prop.getProperty("authorizationExtension").trim() + "/roles")
                .header("Authorization", "Bearer " + getAuthorizationAccessToken()).asString();
    }

    public HttpResponse<String> authorizationGetPermissionsByRole(String id) throws UnirestException, JSONException {
        Unirest.setTimeouts(0, 0);
        return Unirest.get(prop.getProperty("authorizationExtension").trim() + "/permissions/" + id)
                .header("Authorization", "Bearer " + getAuthorizationAccessToken()).asString();

    }

    public HttpResponse<String> authorizationGetUserRoles(String userId) throws UnirestException, JSONException {
        String user = userId.replace("auth0|", "auth0%7C");
        Unirest.setTimeouts(0, 0);
        return Unirest.get(prop.getProperty("authorizationExtension").trim() + "/users/" + user + "/roles")
                .header("Authorization", "Bearer " + getAuthorizationAccessToken()).asString();
    }

    public String getAuthorizationAccessToken() throws UnirestException, JSONException {
        HttpResponse<String> res = authorizationAccessToken();
        return getField(res, "access_token");
    }

    public JSONObject getGroups() throws UnirestException, JSONException, InterruptedException, ExecutionException {
        return new JSONObject(authorizationGetGroups().getBody());
    }

    public String getGroupID(JSONObject jsonObject) throws JSONException {

        JSONArray jsonArray = jsonObject.getJSONArray("groups");
        for (int i = 0; i < jsonArray.length(); i++) {
            if (jsonArray.getJSONObject(i).get("name").equals(prop.getProperty("groupName").trim())) {
                return jsonArray.getJSONObject(i).get("_id").toString();
            }
        }
        return null;
    }

    public List<String> getRoles(JSONObject json) throws JSONException {
        String[] roles = json.getJSONObject("app_metadata")
                .getJSONObject("authorization").get("roles").toString().replaceAll("\"", "").replace("[", "").replace("]", "").split(",");
        return Collections.arrayToList(roles);
    }

    public List<String> getRoles(JSONArray json) throws JSONException {

        List<String> list = new ArrayList<>();
        for (int i = 0; i < json.length(); i++) {
            list.add((String) json.getJSONObject(i).get("name"));
        }
        return list;
    }

    public List<String> getSignUpRolesId(UserDTO user, HttpResponse<String> rsp) throws UnirestException, JSONException, InterruptedException, ExecutionException {
        Iterator<String> it = user.getRoles().iterator();
        List<String> list = new ArrayList<>();
        String roles = rsp.getBody();
        while (it.hasNext()) {
            list.add(getRoleByName(it.next(), roles));
        }
        return list;
    }

    public String getRoleByName(String name, String roles) throws UnirestException, JSONException, InterruptedException, ExecutionException {
        JSONObject json = new JSONObject(roles);
        JSONArray jsonArray = json.getJSONArray("roles");
        for (int i = 0; i < jsonArray.length(); i++) {
            if (jsonArray.getJSONObject(i).get("name").equals(name)) {
                return (String) jsonArray.getJSONObject(i).get("_id");
            }
        }
        return null;
    }

    public String getField(HttpResponse<String> rsp, String field) throws JSONException {
        JSONObject json = new JSONObject(rsp.getBody());
        return (String) json.get(field);
    }

    public List<String> getRolesIDPerUser(HttpResponse<String> rsp) throws JSONException {
        JSONArray jarray = new JSONArray(rsp.getBody());
        List<String> list = new ArrayList<>();

        for (int i = 0; i < jarray.length(); i++) {
            list.add((String) jarray.getJSONObject(i).get("_id"));
        }

        return list;
    }

    public List<String> getPermissionsPerRole(List<String> list) throws UnirestException, JSONException, InterruptedException, ExecutionException {

        JSONObject json = new JSONObject(authorizationGetRoles().getBody());
        JSONArray jarray = json.getJSONArray("roles");
        List<String> plist = new ArrayList<>();

        for (int i = 0; i < jarray.length(); i++) {
            if (list.contains(jarray.getJSONObject(i).get("_id"))) {

                for (int j = 0; j < jarray.getJSONObject(i).getJSONArray("permissions").length(); j++) {

                    Logger.getAnonymousLogger().info((String) new JSONObject(authorizationGetPermissionsByRole((String) jarray.getJSONObject(i).getJSONArray("permissions").get(j)).getBody()).get("name"));
                    plist.add((String) new JSONObject(authorizationGetPermissionsByRole((String) jarray.getJSONObject(i).getJSONArray("permissions").get(j)).getBody()).get("name"));
                }
            }
        }
        return plist;
    }

    public Map<String, List<String>> getPermissionsPerRole() throws UnirestException, JSONException, ExecutionException, InterruptedException {
        JSONObject json;
        json = new JSONObject(authorizationGetRoles().getBody());
        JSONArray jarray = json.getJSONArray("roles");
        Map<String, List<String>> mp = new HashMap<>();
        List<String> plist = new ArrayList<>();

        for (int i = 0; i < jarray.length(); i++) {
            plist = new ArrayList<>();
            for (int j = 0; j < jarray.getJSONObject(i).getJSONArray("permissions").length(); j++) {
                String d = (String) new JSONObject(authorizationGetPermissionsByRole((String) jarray.getJSONObject(i).getJSONArray("permissions").get(j)).getBody()).get("name");
                Logger.getAnonymousLogger().info("agregando permiso " + d.split(":")[0] + " para el recurso  " + d.split(":")[1] + " a cache");
                plist.add(d);
            }
            mp.put((String) jarray.getJSONObject(i).get("_id"), plist);
        }

        return mp;
    }

}
