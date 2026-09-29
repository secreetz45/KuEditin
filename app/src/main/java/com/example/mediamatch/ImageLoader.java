package com.example.mediamatch;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageLoader {
    public static void load(ImageView view, String source) {
        if (view == null || source == null || source.trim().isEmpty()) return;

        String value = source.trim();

        // Support foto profil tanpa Firebase Storage: data:image/jpeg;base64,...
        if (value.startsWith("data:image")) {
            try {
                int commaIndex = value.indexOf(',');
                if (commaIndex >= 0) value = value.substring(commaIndex + 1);
                byte[] bytes = Base64.decode(value, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) view.setImageBitmap(bitmap);
            } catch (Exception ignored) {}
            return;
        }

        // Support raw base64 kalau suatu saat disimpan tanpa prefix data:image.
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            try {
                byte[] bytes = Base64.decode(value, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) view.setImageBitmap(bitmap);
            } catch (Exception ignored) {}
            return;
        }

        // Support URL biasa jika nanti memakai Cloudinary/Firebase Storage.
        String url = value;
        new Thread(() -> {
            try {
                URL u = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.connect();
                InputStream input = conn.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                input.close();
                new Handler(Looper.getMainLooper()).post(() -> view.setImageBitmap(bitmap));
            } catch (Exception ignored) {}
        }).start();
    }
}
