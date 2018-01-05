
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
package co.edu.uniandes.csw.auth.service;

import co.edu.uniandes.csw.auth.api.AuthenticationApi;
import co.edu.uniandes.csw.auth.model.UserDTO;
import co.edu.uniandes.csw.auth.filter.StatusCreated;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.SignatureException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.json.JSONException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.ws.rs.WebApplicationException;

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthService {
    
    private final  AuthenticationApi auth;

    @Context
    private HttpServletResponse rsp;

    @Context
    private HttpServletRequest req;
    private static boolean logged = false;

    public AuthService() throws IOException, UnirestException, JSONException, InterruptedException, ExecutionException {
        this.auth = new AuthenticationApi();
    }

    @Path("/login")
    @POST
    public UserDTO login(UserDTO user) throws UnirestException, JSONException, IOException, InterruptedException, ExecutionException {
     try{
        String  str = auth.getSubject(user, rsp);
        logged = true;
      return  AuthenticationApi.getProfileCache().get(str);
     }catch(SignatureException se){
     throw new WebApplicationException(se.getMessage());
     }
    }

    @Path("/logout")
    @GET
    public void logout() {
        auth.authenticationLogout();
        logged = false;
    }

    @Path("/register")
    @POST
    @StatusCreated
    public void register(UserDTO user) throws UnirestException, JSONException, IOException, InterruptedException, ExecutionException {

      HttpResponse<String> rs = auth.authenticationSignUP(user);
       auth.HttpServletResponseBinder(rs, rsp);
    }

    @Path("/me")
    @GET
    public UserDTO getCurrentUser() throws JSONException, UnirestException, IOException, InterruptedException, ExecutionException {
       Jws<Claims> claim = null;
       String subject;
        if (logged & req.getCookies()!=null) {     
            claim = auth.decryptToken(req);
        }
        if (claim != null) {
            subject = claim.getBody().getSubject(); 
           return AuthenticationApi.getProfileCache().get(subject);
        }
      return null;  
      
    }

}
