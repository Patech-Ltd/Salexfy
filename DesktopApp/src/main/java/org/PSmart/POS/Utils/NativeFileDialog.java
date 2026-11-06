package org.PSmart.POS.Utils;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

/**
 * "Save as" using the operating system's own file dialog (Windows/macOS: the native AWT dialog;
 * Linux: GTK via zenity, or KDE via kdialog, whichever is installed). Falls back to the AWT dialog
 * only if neither is available. Remembers the last folder.
 *
 * Call from a background thread (the Linux dialogs are separate processes and would block the screen otherwise).
 */
public final class NativeFileDialog {

    private static final Preferences PREFS = Preferences.userNodeForPackage(NativeFileDialog.class);
    private static final String LAST_DIR = "lastExportDir";

    private NativeFileDialog() {}

    private static File startDir() {
        String saved = PREFS.get(LAST_DIR, null);
        if (saved != null && new File(saved).isDirectory()) return new File(saved);
        File docs = new File(System.getProperty("user.home"), "Documents");
        return docs.isDirectory() ? docs : new File(System.getProperty("user.home"));
    }

    /** @return the chosen file (extension added when missing), or null if the user cancelled. */
    public static File save(Component parent, String title, String defaultName, String ext, String description) {
        File dir = startDir();
        String os = System.getProperty("os.name", "").toLowerCase();
        File chosen = null;
        if (os.contains("linux") || os.contains("bsd")) {
            if (exists("zenity")) {
                chosen = run(new String[]{"zenity", "--file-selection", "--save", "--confirm-overwrite",
                        "--title=" + title, "--filename=" + new File(dir, defaultName + "." + ext).getAbsolutePath(),
                        "--file-filter=" + description + " | *." + ext});
                return finish(chosen, ext);
            }
            if (exists("kdialog")) {
                chosen = run(new String[]{"kdialog", "--title", title, "--getsavefilename",
                        new File(dir, defaultName + "." + ext).getAbsolutePath(), "*." + ext + "|" + description});
                return finish(chosen, ext);
            }
        }
        return finish(awtDialog(parent, title, dir, defaultName + "." + ext), ext);
    }

    private static File finish(File f, String ext) {
        if (f == null) return null;
        if (!f.getName().toLowerCase().endsWith("." + ext)) f = new File(f.getAbsolutePath() + "." + ext);
        if (f.getParentFile() != null) PREFS.put(LAST_DIR, f.getParentFile().getAbsolutePath());
        return f;
    }

    private static boolean exists(String cmd) {
        try {
            Process p = new ProcessBuilder("which", cmd).redirectErrorStream(true).start();
            return p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static File run(String[] cmd) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(false).start();
            String out;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                out = r.readLine();
            }
            int code = p.waitFor();
            if (code != 0 || out == null || out.isBlank()) return null;
            return new File(out.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static File awtDialog(Component parent, String title, File dir, String defaultFile) {
        final File[] result = new File[1];
        Runnable show = () -> {
            Window w = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
            Frame owner = w instanceof Frame ? (Frame) w : new Frame();
            FileDialog fd = new FileDialog(owner, title, FileDialog.SAVE);
            fd.setDirectory(dir.getAbsolutePath());
            fd.setFile(defaultFile);
            fd.setVisible(true);
            if (fd.getFile() != null && fd.getDirectory() != null) result[0] = new File(fd.getDirectory(), fd.getFile());
        };
        try {
            if (SwingUtilities.isEventDispatchThread()) show.run(); else SwingUtilities.invokeAndWait(show);
        } catch (Exception e) {
            return null;
        }
        return result[0];
    }
}
