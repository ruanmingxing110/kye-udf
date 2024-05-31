package com.kye.utils;

import org.roaringbitmap.longlong.Roaring64Bitmap;

import java.io.*;

public class BitmapUtil {
    public static byte[] serializeToBytes(Roaring64Bitmap bitmap) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        bitmap.serialize(dos);
        dos.close();
        return bos.toByteArray();
    }

    public static Roaring64Bitmap deserializeToBitmap(byte[] bytes) throws IOException {
        Roaring64Bitmap bitmapValue = new Roaring64Bitmap();
        if (bytes == null) {
            return bitmapValue;
        }
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        bitmapValue.deserialize(in);
        in.close();
        return bitmapValue;
    }
}