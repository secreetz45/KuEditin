package com.example.mediamatch;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.database.Cursor;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CloudinaryUtil {
    public interface Callback {
        void onSuccess(String url, String publicId, String name, String mime);
        void onError(String message);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static boolean isConfigured(Context ctx) {
        String cloud = getString(ctx, "cloudinary_cloud_name");
        String preset = getString(ctx, "cloudinary_upload_preset");
        return cloud != null && preset != null && !cloud.contains("ISI_") && !preset.contains("ISI_") && !cloud.trim().isEmpty() && !preset.trim().isEmpty();
    }

    public static void uploadUri(Context ctx, Uri uri, String folder, Callback cb) {
        if (uri == null) {
            MAIN.post(() -> cb.onError("File belum dipilih"));
            return;
        }
        EXECUTOR.execute(() -> {
            try {
                if (!isConfigured(ctx)) throw new Exception("Cloudinary belum dikonfigurasi. Isi cloudinary_cloud_name dan cloudinary_upload_preset di res/values/cloudinary.xml");
                String name = getFileName(ctx, uri);
                String mime = ctx.getContentResolver().getType(uri);
                if (mime == null || mime.trim().isEmpty()) mime = "application/octet-stream";
                InputStream in = ctx.getContentResolver().openInputStream(uri);
                if (in == null) throw new Exception("File tidak bisa dibuka");
                byte[] bytes = readAll(in);
                String result = uploadBytesInternal(ctx, bytes, name, mime, folder);
                JSONObject json = new JSONObject(result);
                String url = json.optString("secure_url", json.optString("url", ""));
                String publicId = json.optString("public_id", "");
                if (url.isEmpty()) throw new Exception("Upload berhasil tetapi URL kosong");
                String finalName = name;
                String finalMime = mime;
                MAIN.post(() -> cb.onSuccess(url, publicId, finalName, finalMime));
            } catch (Exception e) {
                MAIN.post(() -> cb.onError(e.getMessage()));
            }
        });
    }

    public static void uploadBitmap(Context ctx, Bitmap bitmap, String fileName, String folder, Callback cb) {
        if (bitmap == null) {
            MAIN.post(() -> cb.onError("Foto kosong"));
            return;
        }
        EXECUTOR.execute(() -> {
            try {
                if (!isConfigured(ctx)) throw new Exception("Cloudinary belum dikonfigurasi. Isi cloudinary_cloud_name dan cloudinary_upload_preset di res/values/cloudinary.xml");
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, bos);
                String name = fileName == null || fileName.trim().isEmpty() ? "camera_" + System.currentTimeMillis() + ".jpg" : fileName;
                String result = uploadBytesInternal(ctx, bos.toByteArray(), name, "image/jpeg", folder);
                JSONObject json = new JSONObject(result);
                String url = json.optString("secure_url", json.optString("url", ""));
                String publicId = json.optString("public_id", "");
                if (url.isEmpty()) throw new Exception("Upload berhasil tetapi URL kosong");
                MAIN.post(() -> cb.onSuccess(url, publicId, name, "image/jpeg"));
            } catch (Exception e) {
                MAIN.post(() -> cb.onError(e.getMessage()));
            }
        });
    }

    private static String uploadBytesInternal(Context ctx, byte[] bytes, String name, String mime, String folder) throws Exception {
        String cloud = getString(ctx, "cloudinary_cloud_name");
        String preset = getString(ctx, "cloudinary_upload_preset");
        String boundary = "----KuEditin" + UUID.randomUUID().toString().replace("-", "");
        String resourceType = mime != null && mime.startsWith("image/") ? "image" : (mime != null && mime.startsWith("video/") ? "video" : "auto");
        URL url = new URL("https://api.cloudinary.com/v1_1/" + cloud + "/" + resourceType + "/upload");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        DataOutputStream out = new DataOutputStream(conn.getOutputStream());
        writeField(out, boundary, "upload_preset", preset);
        if (folder != null && !folder.trim().isEmpty()) writeField(out, boundary, "folder", folder);
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + sanitize(name) + "\"\r\n");
        out.writeBytes("Content-Type: " + (mime == null ? "application/octet-stream" : mime) + "\r\n\r\n");
        out.write(bytes);
        out.writeBytes("\r\n--" + boundary + "--\r\n");
        out.flush();
        out.close();

        int code = conn.getResponseCode();
        InputStream res = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        String body = new String(readAll(res), "UTF-8");
        if (code < 200 || code >= 300) throw new Exception("Upload Cloudinary gagal: " + body);
        return body;
    }

    private static void writeField(DataOutputStream out, String boundary, String name, String value) throws Exception {
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        out.writeBytes(value + "\r\n");
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
        in.close();
        return bos.toByteArray();
    }

    public static String getFileName(Context ctx, Uri uri) {
        try (Cursor c = ctx.getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) return c.getString(i);
            }
        } catch (Exception ignored) {}
        return "kueditin_file";
    }

    private static String getString(Context ctx, String name) {
        int id = ctx.getResources().getIdentifier(name, "string", ctx.getPackageName());
        return id == 0 ? "" : ctx.getString(id).trim();
    }

    private static String sanitize(String s) {
        if (s == null || s.trim().isEmpty()) return "file";
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
