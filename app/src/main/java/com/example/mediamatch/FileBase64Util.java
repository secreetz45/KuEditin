package com.example.mediamatch;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileBase64Util {
    public static final int MAX_BYTES = 850 * 1024;

    public static class EncodedFile {
        public String name = "";
        public String mime = "application/octet-stream";
        public String base64 = "";
        public long size = 0;
    }

    public static EncodedFile encodeFromUri(Context ctx, Uri uri) throws Exception {
        EncodedFile out = new EncodedFile();
        out.name = getFileName(ctx, uri);
        String type = ctx.getContentResolver().getType(uri);
        out.mime = type == null ? "application/octet-stream" : type;
        InputStream in = ctx.getContentResolver().openInputStream(uri);
        if (in == null) throw new Exception("File tidak bisa dibuka");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            bos.write(buf, 0, n);
            if (bos.size() > MAX_BYTES) {
                in.close();
                throw new Exception("File terlalu besar. Untuk versi tanpa Storage, gunakan file/gambar di bawah 850KB.");
            }
        }
        in.close();
        byte[] bytes = bos.toByteArray();
        out.size = bytes.length;
        out.base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
        return out;
    }

    public static EncodedFile encodeBitmap(Bitmap bitmap, String name) throws Exception {
        EncodedFile out = new EncodedFile();
        out.name = name;
        out.mime = "image/jpeg";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int quality = 80;
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos);
        while (bos.size() > MAX_BYTES && quality > 30) {
            bos.reset();
            quality -= 10;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos);
        }
        if (bos.size() > MAX_BYTES) throw new Exception("Foto terlalu besar setelah dikompres.");
        byte[] bytes = bos.toByteArray();
        out.size = bytes.length;
        out.base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
        return out;
    }

    public static Bitmap bitmapFromBase64(String base64) {
        try {
            byte[] bytes = Base64.decode(base64, Base64.NO_WRAP);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) { return null; }
    }

    public static void openBase64File(Context ctx, String base64, String name, String mime) {
        try {
            if (base64 == null || base64.trim().isEmpty()) {
                Toast.makeText(ctx, "File belum tersedia", Toast.LENGTH_SHORT).show();
                return;
            }
            byte[] bytes = Base64.decode(base64, Base64.NO_WRAP);
            File dir = new File(ctx.getCacheDir(), "kueditin_files");
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, sanitize(name == null || name.trim().isEmpty() ? "file_bahan" : name));
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(bytes);
            fos.close();
            Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".provider", f);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, mime == null || mime.isEmpty() ? "*/*" : mime);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ctx.startActivity(Intent.createChooser(i, "Buka file"));
        } catch (Exception e) {
            Toast.makeText(ctx, "Tidak bisa membuka file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static void openUrl(Context ctx, String url) {
        try {
            if (url == null || url.trim().isEmpty()) {
                Toast.makeText(ctx, "File belum tersedia", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            ctx.startActivity(Intent.createChooser(i, "Buka file"));
        } catch (Exception e) {
            Toast.makeText(ctx, "Tidak bisa membuka link file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static void openFile(Context ctx, String url, String base64, String name, String mime) {
        if (url != null && !url.trim().isEmpty()) openUrl(ctx, url);
        else openBase64File(ctx, base64, name, mime);
    }

    public static String getFileName(Context ctx, Uri uri) {
        try (Cursor c = ctx.getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) return c.getString(i);
            }
        } catch (Exception ignored) {}
        return "file_bahan";
    }

    private static String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
