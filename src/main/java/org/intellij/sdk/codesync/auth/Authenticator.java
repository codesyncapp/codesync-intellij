package org.intellij.sdk.codesync.auth;

import com.auth0.jwt.interfaces.Claim;

import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.intellij.sdk.codesync.files.UserFile;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import com.auth0.jwt.*;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;

import org.intellij.sdk.codesync.clients.Utils;
import static org.intellij.sdk.codesync.Constants.*;


public class Authenticator extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String accessToken = request.getParameter("access_token");
        String idToken = request.getParameter("id_token");
        createUser(accessToken, idToken);

        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);

        response.getWriter().print("<body><h1 class=\"\" style=\"text-align: center;\" >You are logged in, you can close this window now.</h1></body>\n");
    }

    public void createUser(String accessToken, String idToken){
        Map<String, Claim> claims;

        try {
            DecodedJWT jwt = JWT.decode(idToken);
            claims = jwt.getClaims();
        } catch (JWTDecodeException exception){
            System.out.println("Could not login the user.");
            return;
        }
        JSONObject payload = new JSONObject();
        payload.putAll(claims);
        JSONObject jsonResponse = Utils.sendPost(API_USERS, payload, accessToken);

        if (jsonResponse == null) {
            System.out.println("Could not login the user.");
            return;
        }
        if (jsonResponse.containsKey("error")) {
            System.out.println("Could not login the user.");
            return;
        }
        UserFile userFile;
        try {
            userFile = new UserFile(USER_FILE_PATH);
        } catch (FileNotFoundException e) {
            if(UserFile.createFile(USER_FILE_PATH)){
                try {
                    userFile = new UserFile(USER_FILE_PATH);
                } catch (FileNotFoundException | InvalidYmlFileError fileNotFoundException) {
                    fileNotFoundException.printStackTrace();
                    // Could not open user file.
                    return;
                }
            } else {
                // Could not create user file.
                return;
            }
            e.printStackTrace();
        } catch (InvalidYmlFileError invalidYmlFileError) {
            invalidYmlFileError.printStackTrace();
            // Could not read user file.
            return;
        }
        String userEmail = claims.get("email").asString();
        userFile.setUser(userEmail, accessToken);
        try {
            userFile.writeYml();
        } catch (FileNotFoundException | InvalidYmlFileError e) {
            System.out.println("Could not login the user.");
            e.printStackTrace();
        }
    }
}
