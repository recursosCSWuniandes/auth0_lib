package co.edu.uniandes.csw.auth.filter;

import co.edu.uniandes.csw.auth.conexions.CacheManager;
import co.edu.uniandes.csw.auth.conexions.AuthenticationApi;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.SignatureException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONException;

@WebFilter(filterName = "FiltroAutenticacion", urlPatterns = {"/api/*"})
public class FiltroAutenticacion implements Filter {

    private AuthenticationApi auth;
    private static int cacheCount;
   static{try {
       
       if(cacheCount==0){
           System.out.println("Inicializando cache.....");
      cacheCount = CacheManager.cacheInit();
       }
        } catch (UnirestException | JSONException | InterruptedException | ExecutionException | IOException ex) {
            Logger.getLogger(FiltroAutenticacion.class.getName()).log(Level.SEVERE, null, ex);
        }
   }

    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            this.auth = new AuthenticationApi(); 
        } catch (UnirestException | JSONException | InterruptedException | ExecutionException ex) {
            Logger.getLogger(FiltroAutenticacion.class.getName()).log(Level.SEVERE, null, ex);
        }
        PropertiesLoader prop = new PropertiesLoader();
        HttpResponse<String> rp = null;
        String usuario = null, jwt = null, path = null, resource = null, subject = null;
        Jws<Claims> claim = null;
        List<String> permissions = null;
        path = ((HttpServletRequest) request).getRequestURI().substring(((HttpServletRequest) request).getContextPath().length()).replace("[/]+$", "");
        path = String.join("/", Arrays.copyOfRange(path.split("/"), 0, 3)).trim();
        resource = path.split("/")[2];
        boolean allowedPath = prop.containsKey(path);
        
        Cookie[] cookie = ((HttpServletRequest) request).getCookies();
        if("development".equals(prop.getPropertyAsString("environment"))){
            chain.doFilter(request, response);
        }
        if(cookie != null){
        for (Cookie c : cookie) {
            if ("id_token".equals(c.getName())) {
                jwt = c.getValue();
            }
            if ("username".equals(c.getName())) {
                usuario = c.getValue();
            }
        }
        }
        try { 
            
               if (usuario != null & jwt != null){
                claim = auth.decryptToken(jwt);
                if (claim != null) {
                    subject = claim.getBody().getSubject();  
                    permissions = CacheManager.getPermissionsCache().get(subject);
                }
               }
               else{ if (!allowedPath & !"users".equals(resource)) {
                throw new SignatureException("No autenticado");
               }
               }
        } catch (SignatureException e) {
            if(!"development".equals(prop.getPropertyAsString("environment"))){
           errorResponse(403, "Usuario no autenticado", (HttpServletResponse) response);
          ((HttpServletResponse) response).sendRedirect("http://localhost:8080" + ((HttpServletRequest) request).getContextPath() + "/api/users/me");  
        }
           
        } catch (ExecutionException ex) {
            Logger.getLogger(FiltroAutenticacion.class.getName()).log(Level.SEVERE, null, ex);
        }
         
        if (permissions != null & !"users".equals(resource) & !allowedPath) {
            if (!permissions.contains(methodMapper(((HttpServletRequest) request).getMethod()).concat(":").concat(resource))) {
                errorResponse(405, "no existe permiso " + methodMapper(((HttpServletRequest) request).getMethod()).concat(":").concat(resource), (HttpServletResponse) response);
            } else {
                chain.doFilter(request, response);
            }
        } else {

            chain.doFilter(request, response);
        }
  
    }

    @Override
    public void destroy() {
        //no realiza ninguna accion
    }

    public static void errorResponse(Integer code, String msg, HttpServletResponse response) throws IOException {
        response.setStatus(code);
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(msg);
        response.flushBuffer();
    }

    public static String methodMapper(String s) {
        String str;
        switch (s) {
            case "POST":
                str = "create";
                break;
            case "GET":
                str = "read";
                break;
            case "PUT":
                str = "update";
                break;
            case "DELETE":
                str = "delete";
                break;
            default:
                str = "";
        }
        return str;
    }

}
