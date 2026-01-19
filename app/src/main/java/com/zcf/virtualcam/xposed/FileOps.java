package com.zcf.virtualcam.xposed;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FileOps {

    private FileOps() {
    }

    private static final Map<String, CachedBytes> BYTES_CACHE = new ConcurrentHashMap<>();

    @Nullable
    public static byte[] readCachedBytes(@NonNull String path, int maxBytes) {
        try {
            File f = new File(path);
            if (!f.exists() || !f.isFile()) {
                return null;
            }
            long lastModified = f.lastModified();
            long length = f.length();

            CachedBytes cached = BYTES_CACHE.get(path);
            if (cached != null && cached.lastModified == lastModified && cached.length == length) {
                return cached.bytes;
            }

            byte[] bytes = readAllBytes(f, maxBytes);
            if (bytes == null) {
                return null;
            }
            BYTES_CACHE.put(path, new CachedBytes(lastModified, length, bytes));
            return bytes;
        } catch (IOException e) {
            Logger.log("读取文件失败: " + path + ",", e);
            return null;
        }
    }

    @Nullable
    public static byte[] readAllBytes(@NonNull File file, int maxBytes) throws IOException {
        if (file.length() > maxBytes) {
            throw new IOException("文件过大: " + file.getAbsolutePath());
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream((int) file.length());
            copyStream(fis, bos);
            return bos.toByteArray();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static void replaceFile(@NonNull File src, @NonNull File dst) throws IOException {
        if (!src.exists() || !src.isFile()) {
            throw new IOException("源文件不存在: " + src.getAbsolutePath());
        }
        File parent = dst.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("无法创建目录: " + parent.getAbsolutePath());
        }

        File tmp = new File(dst.getAbsolutePath() + ".tmp");
        copyFile(src, tmp);
        if (dst.exists() && !dst.delete()) {
            throw new IOException("无法删除旧文件: " + dst.getAbsolutePath());
        }
        if (!tmp.renameTo(dst)) {
            copyFile(tmp, dst);
            if (!tmp.delete()) {
                Logger.log("临时文件删除失败: " + tmp.getAbsolutePath());
            }
        }
    }

    public static void copyFile(@NonNull File src, @NonNull File dst) throws IOException {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(src);
            fos = new FileOutputStream(dst, false);
            copyStream(fis, fos);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignored) {
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static void copyFromUri(@NonNull Context context, @NonNull Uri uri, @NonNull File dst) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            if (is == null) {
                throw new IOException("无法打开输入流: " + uri);
            }
            os = new FileOutputStream(dst, false);
            copyStream(is, os);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static void copyStream(@NonNull InputStream is, @NonNull OutputStream os) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) >= 0) {
            os.write(buffer, 0, read);
        }
        os.flush();
    }

    private static final class CachedBytes {
        final long lastModified;
        final long length;
        final byte[] bytes;

        CachedBytes(long lastModified, long length, byte[] bytes) {
            this.lastModified = lastModified;
            this.length = length;
            this.bytes = bytes;
        }
    }
}
