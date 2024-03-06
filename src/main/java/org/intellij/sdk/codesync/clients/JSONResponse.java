package org.intellij.sdk.codesync.clients;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;
import org.intellij.sdk.codesync.Constants;
import org.intellij.sdk.codesync.exceptions.InvalidJsonError;
import org.intellij.sdk.codesync.exceptions.response.StatusCodeError;
import org.intellij.sdk.codesync.state.StateUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;

/*
Class to store API response data.
*/
public class JSONResponse {
    private final int statusCode;
    private final JSONObject jsonResponse;

    public JSONResponse(int statusCode, JSONObject jsonResponse) {
        this.statusCode = statusCode;
        this.jsonResponse = jsonResponse;
    }


    public static JSONResponse from(HttpResponse response) throws InvalidJsonError {
        String responseContent;
        StatusLine responseStatusLine;
        JSONObject jsonResponse;
        try {

            responseStatusLine = response.getStatusLine();
            responseContent = EntityUtils.toString(response.getEntity());
        } catch (IOException | ParseException error) {
            System.out.printf("Error processing response of the request. Error: %s%n", error.getMessage());
            throw new InvalidJsonError(
                String.format("Error processing response of the request. Error: %s", error.getMessage())
            );
        }

        try {
            jsonResponse = (JSONObject) JSONValue.parseWithException(responseContent);
        } catch (org.json.simple.parser.ParseException | ClassCastException error) {
            System.out.printf("Error processing response of the request. Error: %s%n", error.getMessage());
            throw new InvalidJsonError(
                String.format("Error processing response of the request. Error: %s", error.getMessage())
            );
        }

        return new JSONResponse(responseStatusLine.getStatusCode(), jsonResponse);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public JSONObject getJsonResponse() {
        return jsonResponse;
    }

    /*
    Validate the status code of the API response and throw status code exception if the response status is not 2xx.
    */
    public void raiseForStatus () throws StatusCodeError {
        Number errorCode = null;
        int customErrorCode = 0;
        // Only throw for client or server error, all other responses are considered success response.
        if(this.statusCode >= 400) {
            if (this.statusCode == Constants.ErrorCodes.ACCOUNT_DEACTIVATED) {
                StateUtils.deactivateAccount();
            }
            String errorMessage = "API returned an error status code.";
            if (this.jsonResponse.containsKey("error")) {
                JSONObject errorObject = (JSONObject)this.jsonResponse.get("error");
                if (errorObject != null && errorObject.containsKey("message")) {
                    errorMessage = (String) errorObject.get("message");
                }
                // Extract error_code only if a 402 is received
                if(this.statusCode == 402){
                    if (errorObject != null && errorObject.containsKey("error_code")) {
                        errorCode = ((Number) errorObject.get("error_code"));
                        if (errorCode != null) {
                            customErrorCode = errorCode.intValue();
                        }
                    }
                }
            }
            throw new StatusCodeError(this.statusCode, customErrorCode, errorMessage);
        }
    }
}
