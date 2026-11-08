package com.snd.assaheed.seerah;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Optional offline copy of the complete book.
 *
 * DownloadManager writes to the app-specific external directory (no storage
 * permission needed on API 19+), then the verified file is copied atomically to
 * getFilesDir()/seerah/seerah.json. The reader checks that internal file first.
 */
public final class OfflineSeerahManager {
    private static final String DIRECTORY = "seerah";
    private static final String FILE_NAME = "seerah.json";
    private static final String EXTERNAL_NAME = "seerah-download.json";
    private static final String PREFS = "seerah_offline";
    private static final String KEY_DOWNLOAD_ID = "download_id";
    private static final long MIN_VALID_BYTES = 1000L;

    private final Context context;

    public OfflineSeerahManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public File getInternalFile() {
        return new File(new File(context.getFilesDir(), DIRECTORY), FILE_NAME);
    }

    public File getExternalFile() {
        File directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        return directory == null ? null : new File(directory, EXTERNAL_NAME);
    }

    public boolean isAvailableOffline() {
        File file = getInternalFile();
        return file.isFile() && file.length() > MIN_VALID_BYTES;
    }

    public long getOfflineSize() {
        File file = getInternalFile();
        return file.isFile() ? file.length() : 0L;
    }

    public String readFull() throws IOException {
        return readFile(getInternalFile());
    }

    public SeerahChapter readChapter(int chapterId) throws Exception {
        return SeerahJson.parseChapterFromFull(readFull(), chapterId);
    }

    public void deleteOffline() {
        File internal = getInternalFile();
        if (internal.exists()) {
            internal.delete();
        }
        File external = getExternalFile();
        if (external != null && external.exists()) {
            external.delete();
        }
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        preferences.edit().remove(KEY_DOWNLOAD_ID).apply();
    }

    public long enqueueDownload() throws IOException {
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager == null) {
            throw new IOException("DownloadManager unavailable");
        }
        File external = getExternalFile();
        if (external == null) {
            throw new IOException("External files directory unavailable");
        }
        if (external.exists()) {
            external.delete();
        }
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(SeerahUrls.FULL));
        request.setTitle("সীরাতুন নবী");
        request.setDescription("অফলাইনে পড়ার জন্য সংরক্ষণ");
        request.setMimeType("application/json");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            request.setAllowedOverMetered(true);
        }
        request.setAllowedOverRoaming(false);
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, EXTERNAL_NAME);
        long id = manager.enqueue(request);
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        preferences.edit().putLong(KEY_DOWNLOAD_ID, id).apply();
        return id;
    }

    public long getDownloadId() {
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return preferences.getLong(KEY_DOWNLOAD_ID, -1L);
    }

    public void clearDownloadId() {
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        preferences.edit().remove(KEY_DOWNLOAD_ID).apply();
    }

    public DownloadProgress queryProgress(long id) {
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager == null || id < 0) {
            return new DownloadProgress(DownloadManager.STATUS_FAILED, 0L, 0L, 0);
        }
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(id);
        Cursor cursor = manager.query(query);
        if (cursor == null) {
            return new DownloadProgress(DownloadManager.STATUS_FAILED, 0L, 0L, 0);
        }
        try {
            if (!cursor.moveToFirst()) {
                return new DownloadProgress(DownloadManager.STATUS_FAILED, 0L, 0L, 0);
            }
            int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            long downloaded = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            long total = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            int reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
            return new DownloadProgress(status, downloaded, total, reason);
        } finally {
            cursor.close();
        }
    }

    /**
     * Validate the finished DownloadManager file and copy it into internal storage.
     * Returns false when the download is incomplete or the JSON is invalid.
     */
    public boolean finalizeDownload(long id) {
        DownloadProgress progress = queryProgress(id);
        if (!progress.isSuccessful()) {
            return false;
        }
        File source = getExternalFile();
        if (source == null || !source.isFile() || source.length() <= MIN_VALID_BYTES) {
            return false;
        }
        try {
            String raw = readFile(source);
            if (SeerahJson.parseAllChapters(raw).size() < 50) {
                throw new IOException("Incomplete book");
            }
            copyToInternal(raw);
            source.delete();
            clearDownloadId();
            return true;
        } catch (Exception error) {
            if (source.exists()) {
                source.delete();
            }
            return false;
        }
    }

    public boolean isDownloadRunning() {
        long id = getDownloadId();
        if (id < 0) {
            return false;
        }
        DownloadProgress progress = queryProgress(id);
        return progress.isPending() || progress.isRunning();
    }

    private void copyToInternal(String raw) throws IOException {
        File directory = new File(context.getFilesDir(), DIRECTORY);
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Cannot create seerah directory");
        }
        File target = new File(directory, FILE_NAME);
        File temporary = new File(directory, FILE_NAME + ".part");
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(temporary);
            output.write(raw.getBytes("UTF-8"));
            output.getFD().sync();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ignored) {
                }
            }
        }
        if (target.exists()) {
            target.delete();
        }
        if (!temporary.renameTo(target)) {
            FileInputStream input = null;
            FileOutputStream fallback = null;
            try {
                input = new FileInputStream(temporary);
                fallback = new FileOutputStream(target);
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    fallback.write(buffer, 0, read);
                }
                fallback.getFD().sync();
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException ignored) {
                    }
                }
                if (fallback != null) {
                    try {
                        fallback.close();
                    } catch (IOException ignored) {
                    }
                }
                if (temporary.exists()) {
                    temporary.delete();
                }
            }
        }
    }

    private String readFile(File file) throws IOException {
        if (!file.isFile()) {
            throw new IOException("File not found");
        }
        FileInputStream input = null;
        try {
            input = new FileInputStream(file);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toString("UTF-8");
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /** Immutable DownloadManager progress snapshot. */
    public static final class DownloadProgress {
        public final int status;
        public final long downloaded;
        public final long total;
        public final int reason;

        public DownloadProgress(int status, long downloaded, long total, int reason) {
            this.status = status;
            this.downloaded = downloaded;
            this.total = total;
            this.reason = reason;
        }

        public boolean isPending() {
            return status == DownloadManager.STATUS_PENDING;
        }

        public boolean isRunning() {
            return status == DownloadManager.STATUS_RUNNING;
        }

        public boolean isPaused() {
            return status == DownloadManager.STATUS_PAUSED;
        }

        public boolean isSuccessful() {
            return status == DownloadManager.STATUS_SUCCESSFUL;
        }

        public boolean isFailed() {
            return status == DownloadManager.STATUS_FAILED;
        }

        public int percent() {
            if (total <= 0L) {
                return 0;
            }
            long value = downloaded * 100L / total;
            if (value < 0L) {
                return 0;
            }
            if (value > 100L) {
                return 100;
            }
            return (int) value;
        }
    }
}
