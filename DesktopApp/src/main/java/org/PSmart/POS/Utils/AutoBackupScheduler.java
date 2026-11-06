package org.PSmart.POS.Utils;

import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.StoreConfig;

/**
 * Runs automatic local backups on a schedule read from StoreConfig. It keeps
 * running as a daemon so it never blocks the app or stops it exiting.
 */
public final class AutoBackupScheduler {

    private AutoBackupScheduler() {}

    /** Starts a daemon that periodically creates a backup while auto-backup is enabled. */
    public static void start() {
        Thread worker = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    StoreConfig config = DatabaseUtils.getInstance().getStoreConfigRepository().getStoreConfig();
                    if (config != null && Boolean.TRUE.equals(config.getAutoBackupEnabled())) {
                        int minutes = config.getAutoBackupMinutes() != null ? config.getAutoBackupMinutes() : 60;
                        if (shouldBackupNow(config)) {
                            int keep = config.getBackupKeepCount() != null ? config.getBackupKeepCount() : 10;
                            try {
                                BackupService.createBackupAndRotate(keep);
                                config.setLastBackupAt(System.currentTimeMillis());
                                DatabaseUtils.getInstance().getStoreConfigRepository().saveOrUpdate(config);
                                System.out.println("Auto-backup completed");
                            } catch (Exception ex) {
                                System.err.println("Auto-backup failed: " + ex.getMessage());
                            }
                        }
                        Thread.sleep(Math.max(1, Math.min(minutes, 720)) * 60L * 1000L);
                    } else {
                        Thread.sleep(60L * 1000L);
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception ex) {
                    try {
                        Thread.sleep(60L * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, "AutoBackup");
        worker.setDaemon(true);
        worker.start();
    }

    private static boolean shouldBackupNow(StoreConfig config) {
        Long last = config.getLastBackupAt();
        if (last == null) return true;
        long minutes = config.getAutoBackupMinutes() != null ? config.getAutoBackupMinutes() : 60;
        long elapsed = (System.currentTimeMillis() - last) / (60L * 1000L);
        return elapsed >= minutes;
    }
}