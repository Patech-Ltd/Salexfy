package com.patechltd.salexfypos.drive;

/**
 * Google Drive backup configuration.
 *
 * <p>Authenticates through Google Sign-In (Play Services). No folder picker is required; backups go
 * to the fixed folder {@link #BACKUP_FOLDER} in the signed-in user's Drive.</p>
 *
 * <p>Before it works in production you must, in the Google Cloud Console for this app:</p>
 * <ul>
 *   <li>Enable the "Google Drive API".</li>
 *   <li>Create an OAuth 2.0 Client ID of type "Android" for package
 *       {@code com.patechltd.salexfypos} with the signing certificate SHA-1 of the release keystore.
 *       Google Sign-In resolves this client automatically from the package + SHA-1.</li>
 * </ul>
 */
public final class DriveConfig {

    public static final String APP_NAME = "Salex POS";

    /** OAuth scopes: drive-scoped file access so we can read/write/delete our backup folder only. */
    public static final java.util.List<String> SCOPES =
            java.util.Collections.singletonList(
                    com.google.api.services.drive.DriveScopes.DRIVE_FILE);

    /** Fixed target folder name inside the user's Google Drive. */
    public static final String BACKUP_FOLDER = "salexpos_backups";

    /** Prefix for safety copies uploaded shortly before a restore. */
    public static final String BEFORE_RESTORE_PREFIX = "slex_db_before_restore_";

    private DriveConfig() {
    }
}
