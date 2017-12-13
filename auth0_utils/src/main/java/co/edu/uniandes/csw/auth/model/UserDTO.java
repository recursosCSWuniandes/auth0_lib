
/*
The MIT License (MIT)

Copyright (c) 2015 Los Andes University

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package co.edu.uniandes.csw.auth.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.annotation.XmlRootElement;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@XmlRootElement
public class UserDTO implements Serializable {

    private String givenName;
    private String middleName;
    private String surName;
    private String userName;
    private String password;
    private String email;
    private String rememberMe;
    private List<String> roles;
    private List<String> permissions;

    public UserDTO() {
    }

    public UserDTO(JSONObject json) {

        try {
            if (json.getJSONObject("user_metadata").get("username") != null) {
                this.setUserName((String) json.getJSONObject("user_metadata").get("username"));
            }
            if (json.getJSONObject("user_metadata").get("given_name") != null) {
                this.setGivenName((String) json.getJSONObject("user_metadata").get("given_name"));
            }
            if (json.getJSONObject("user_metadata").get("middle_name") != null) {
                this.setMiddleName((String) json.getJSONObject("user_metadata").get("middle_name"));
            }
            if (json.getJSONObject("user_metadata").get("sur_name") != null) {
                this.setSurName((String) json.getJSONObject("user_metadata").get("sur_name"));
            }
            if (json.getJSONObject("user_metadata").get("email") != null) {
                this.setEmail((String) json.getJSONObject("user_metadata").get("email"));
            }
            if (json.getJSONObject("app_metadata").getJSONObject("authorization").getJSONArray("roles").length() > 0) {
                JSONArray roles = json.getJSONObject("app_metadata").getJSONObject("authorization").getJSONArray("roles");
                List<String> roleList = new ArrayList<>();
                for (int c = 0; c < roles.length(); c++) {
                    roleList.add(roles.getString(c));
                }
                this.setRoles(roleList);
            }
            if (json.getJSONObject("app_metadata").getJSONObject("authorization").getJSONArray("permissions").length() > 0) {
                JSONArray permissions = json.getJSONObject("app_metadata").getJSONObject("authorization").getJSONArray("permissions");
                List<String> permissionList = new ArrayList<>();
                for (int c = 0; c < permissions.length(); c++) {
                   permissionList.add(permissions.getString(c));
                }
                this.setPermissions(permissionList);
            }

        } catch (JSONException ex) {
            Logger.getLogger(UserDTO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @return the givenName
     */
    public String getGivenName() {
        return givenName;
    }

    /**
     * @param givenName the givenName to set
     */
    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    /**
     * @return the middleName
     */
    public String getMiddleName() {
        return middleName;
    }

    /**
     * @param middleName the middleName to set
     */
    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    /**
     * @return the surName
     */
    public String getSurName() {
        return surName;
    }

    /**
     * @param surName the surName to set
     */
    public void setSurName(String surName) {
        this.surName = surName;
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName the userName to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the rememberMe
     */
    public String isRememberMe() {
        return rememberMe;
    }

    /**
     * @param rememberMe the rememberMe to set
     */
    public void setRememberMe(String rememberMe) {
        this.rememberMe = rememberMe;
    }

    /**
     * @return the roles
     */
    public List<String> getRoles() {
        return roles;
    }

    /**
     * @param roles the roles to set
     */
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    /**
     * @return the permissions
     */
    public List<String> getPermissions() {
        return permissions;
    }

    /**
     * @param permissions the permissions to set
     */
    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    /**
     * @return the id_token
     */
}
