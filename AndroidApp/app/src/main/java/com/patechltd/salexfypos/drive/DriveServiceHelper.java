package com.patechltd.salexfypos.drive;

import android.content.Context;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.patechltd.salexfypos.util.AppLogger;

public final class DriveServiceHelper {

    private DriveServiceHelper() {
    }

    private static final HttpTransport TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON = GsonFactory.getDefaultInstance();

    /**
     * Builds a {@link Drive} service using the supplied signed-in Google account.
     * The {@link GoogleAccountCredential} requests/refreshes OAuth tokens via Google Play services.
     */
    public static Drive getDrive(Context context, GoogleSignInAccount account) {
        GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(context, DriveConfig.SCOPES);
        if (account != null && account.getEmail() != null) {
            credential.setSelectedAccountName(account.getEmail());
        }
        return new Drive.Builder(TRANSPORT, JSON, credential)
                .setApplicationName(DriveConfig.APP_NAME)
                .build();
    }

    public static boolean hasDriveScope(GoogleSignInAccount account) {
        if (account == null) return false;
        return account.getGrantedScopes() != null
                && account.getGrantedScopes().contains(DriveScopes.DRIVE_FILE);
    }

    public static void log(String msg, Throwable t) {
        AppLogger.e("Drive: " + msg, t);
    }
}
