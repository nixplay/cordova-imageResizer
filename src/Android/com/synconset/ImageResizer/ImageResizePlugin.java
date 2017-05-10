/**
 * An Image Resizer Plugin for Cordova/PhoneGap.
 * <p>
 * More Information : https://github.com/raananw/
 * <p>
 * The android version of the file stores the images using the local storage.
 * <p>
 * The software is open source, MIT Licensed.
 * Copyright (C) 2012, webXells GmbH All Rights Reserved.
 *
 * @author Raanan Weber, webXells GmbH, http://www.webxells.com
 */
package com.synconset;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import it.sephiroth.android.library.exif2.ExifInterface;
import it.sephiroth.android.library.exif2.ExifTag;

public class ImageResizePlugin extends CordovaPlugin {
    public static final String IMAGE_DATA_TYPE_BASE64 = "base64Image";
    public static final String IMAGE_DATA_TYPE_URL = "urlImage";
    public static final String RESIZE_TYPE_FACTOR = "factorResize";
    public static final String RESIZE_TYPE_MIN_PIXEL = "minPixelResize";
    public static final String RESIZE_TYPE_MAX_PIXEL = "maxPixelResize";
    public static final String RETURN_BASE64 = "returnBase64";
    public static final String RETURN_URI = "returnUri";
    public static final String FORMAT_JPG = "jpg";
    public static final String FORMAT_PNG = "png";
    public static final String DEFAULT_FORMAT = "jpg";
    public static final String DEFAULT_IMAGE_DATA_TYPE = IMAGE_DATA_TYPE_BASE64;
    public static final String DEFAULT_RESIZE_TYPE = RESIZE_TYPE_FACTOR;

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        JSONObject params = data.getJSONObject(0);
        if (action.equals("resizeImage")) {
            ResizeImage resizeImage = new ResizeImage(params, callbackContext);
            cordova.getThreadPool().execute(resizeImage);
            return true;
        } else if (action.equals("imageSize")) {
            GetImageSize imageSize = new GetImageSize(params, callbackContext);
            cordova.getThreadPool().execute(imageSize);
            return true;
        } else if (action.equals("storeImage")) {
            StoreImage storeImage = new StoreImage(params, callbackContext);
            cordova.getThreadPool().execute(storeImage);
            return true;
        } else {
            Log.d("PLUGIN", "unknown action");
            return false;
        }
    }

    private class ImageTools {
        protected JSONObject params;
        protected CallbackContext callbackContext;
        protected String format;
        protected String imageData;
        protected String imageDataType;

        public ImageTools(JSONObject params, CallbackContext callbackContext) throws JSONException {
            this.params = params;
            this.callbackContext = callbackContext;
            imageData = params.getString("data");
            imageDataType = DEFAULT_IMAGE_DATA_TYPE;
            if (params.has("imageDataType")) {
                imageDataType = params.getString("imageDataType");
            }
            format = DEFAULT_FORMAT;
            if (params.has("format")) {
                format = params.getString("format");
            }
        }

        protected Bitmap getBitmap(String imageData, String imageDataType, BitmapFactory.Options options) throws IOException, URISyntaxException {
            Bitmap bmp;
            if (imageDataType.equals(IMAGE_DATA_TYPE_BASE64)) {
                byte[] blob = Base64.decode(imageData, Base64.DEFAULT);
                bmp = BitmapFactory.decodeByteArray(blob, 0, blob.length, options);
            } else {
                URI uri = new URI(imageData);
                File imageFile = new File(uri);
                bmp = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
            }
            return bmp;
        }

        protected void storeImage(JSONObject params, String format, Bitmap bmp, CallbackContext callbackContext) throws JSONException, IOException, URISyntaxException {
            int quality = params.getInt("quality");
            String filename = params.getString("filename");
            String filePath = System.getProperty("java.io.tmpdir") + "/" + filename + ".jpg";
            File file = new File(filePath);
            OutputStream outStream = new FileOutputStream(file);
            if (format.equals(FORMAT_PNG)) {
                bmp.compress(Bitmap.CompressFormat.PNG, quality,
                        outStream);
            } else {
                bmp.compress(Bitmap.CompressFormat.JPEG, quality,
                        outStream);
            }
            outStream.flush();
            outStream.close();
            JSONObject res = new JSONObject();
            res.put("filePath", Uri.fromFile(file).toString());
            res.put("width", bmp.getWidth());
            res.put("height", bmp.getHeight());
            callbackContext.success(res);
        }

        //James Kong 2017-01-27
        protected void storeImageWithExif(JSONObject params, String format, Bitmap bmp, ExifInterface exif, CallbackContext callbackContext) throws JSONException, IOException, URISyntaxException {
            int quality = params.getInt("quality");
            String filename = params.getString("filename");
            String filePath = System.getProperty("java.io.tmpdir") + "/" + filename + ".jpg";
            File file = new File(filePath);
            exif.writeExif(bmp, filePath, quality);
            JSONObject res = new JSONObject();
            res.put("filePath", Uri.fromFile(file).toString());
            res.put("width", bmp.getWidth());
            res.put("height", bmp.getHeight());
            callbackContext.success(res);
        }
    }

    private class GetImageSize extends ImageTools implements Runnable {
        public GetImageSize(JSONObject params, CallbackContext callbackContext) throws JSONException {
            super(params, callbackContext);
        }

        @Override
        public void run() {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                Bitmap bmp = getBitmap(imageData, imageDataType, options);
                JSONObject res = new JSONObject();
                res.put("width", options.outWidth);
                res.put("height", options.outHeight);
                callbackContext.success(res);
            } catch (JSONException e) {
                callbackContext.error(e.getMessage());
            } catch (IOException e) {
                Log.d("PLUGIN", e.getMessage());
                callbackContext.error(e.getMessage());
            } catch (URISyntaxException e) {
                Log.d("PLUGIN", e.getMessage());
                callbackContext.error(e.getMessage());
            }
        }
    }

    private class StoreImage extends ImageTools implements Runnable {
        public StoreImage(JSONObject params, CallbackContext callbackContext) throws JSONException {
            super(params, callbackContext);
        }

        @Override
        public void run() {
            try {
                Bitmap bmp = getBitmap(imageData, imageDataType, new BitmapFactory.Options());
                if (bmp == null) {
                    throw new IOException("The image file could not be opened.");
                }
                this.storeImage(params, format, bmp, callbackContext);
            } catch (JSONException e) {
                Log.d("PLUGIN", e.getMessage());
                callbackContext.error(e.getMessage());
            } catch (IOException e) {
                Log.d("PLUGIN", e.getMessage());
                callbackContext.error(e.getMessage());
            } catch (URISyntaxException e) {
                Log.d("PLUGIN", e.getMessage());
                callbackContext.error(e.getMessage());
            }
        }
    }

    private class ResizeImage extends ImageTools implements Runnable {
        public ResizeImage(JSONObject params, CallbackContext callbackContext) throws JSONException {
            super(params, callbackContext);
        }

        @Override
        public void run() {
            try {
                URI uri = new URI(imageData);
                File imageFile = new File(uri);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

                float[] sizes = calculateFactors(params, options.outWidth, options.outHeight);
                float reqWidth = options.outWidth * sizes[0];
                float reqHeight = options.outHeight * sizes[1];
                int inSampleSize = calculateInSampleSize(options, (int) reqWidth, (int) reqHeight);
                options.inSampleSize = inSampleSize;
                Bitmap bmp = null;
                try {
                    bmp = BitmapUtil.decodeLargeBitmap(cordova.getActivity().getBaseContext(), imageFile, (int) reqWidth, (int) reqHeight, inSampleSize);
                } catch (BitmapUtil.UnableToDecodeBitmapException e) {
                    e.printStackTrace();
                }
//                Bitmap bmp = getBitmap(imageData, imageDataType, options);
                if (bmp == null) {
                    throw new IOException("The image file could not be opened.");
                }

                sizes = calculateFactors(params, options.outWidth, options.outHeight);

                ExifInterface exif = new ExifInterface();
                long orientation = 0;
                ExifTag orientationTag = null;
                try {
                    exif.readExif(imageFile.getAbsolutePath(), ExifInterface.Options.OPTION_ALL);


                    orientationTag = exif.getTag(ExifInterface.TAG_ORIENTATION);
                    orientation = orientationTag.getValueAsLong(0);

                } catch (Exception e) {
                    Log.e("ImageResizer", "exif.readExif( " + imageFile.getAbsolutePath() + " , ExifInterface.Options.OPTION_ALL )");
                }
                Log.d("Exif", exif.toString());
                bmp = getResizedBitmap(bmp, sizes[0], sizes[1], (short) orientation);
                try {
                    exif.setTagValue(ExifInterface.TAG_ORIENTATION, 1);

                } catch (Exception e) {
                    Log.e("ImageResizer", "exif.setTagValue(ExifInterface.TAG_ORIENTATION,1)");
                }
                if (params.getInt("storeImage") > 0) {
                    //James Kong 2017-01-27
                    try {
                        storeImageWithExif(params, format, bmp, exif, callbackContext);
                    } catch (Exception e) {
                        storeImage(params, format, bmp, callbackContext);
                    }
                } else {
                    int quality = params.getInt("quality");
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    if (format.equals(FORMAT_PNG)) {
                        bmp.compress(Bitmap.CompressFormat.PNG, quality, baos);
                    } else {
                        bmp.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                    }
                    byte[] b = baos.toByteArray();
                    String returnString = Base64.encodeToString(b, Base64.NO_WRAP);
                    // return object
                    JSONObject res = new JSONObject();
                    res.put("imageData", returnString);
                    res.put("width", bmp.getWidth());
                    res.put("height", bmp.getHeight());
                    callbackContext.success(res);
                }
            } catch (JSONException e) {
                Log.d("PLUGIN", e.getMessage());
                callbackContext.error(e.getMessage());
            } catch (IOException e) {
                Log.d("PLUGIN", e.getMessage());
                callbackContext.error(e.getMessage());
            } catch (URISyntaxException e) {
                Log.d("PLUGIN", e.getMessage());
                callbackContext.error(e.getMessage());
            }
        }

        private Bitmap getResizedBitmap(Bitmap bm, float widthFactor, float heightFactor, short orientation) {
            int width = bm.getWidth();
            int height = bm.getHeight();
            int rotate = 0;

            /**

             * BOTTOM_LEFT 3 180
             * RIGHT_TOP 6 90
             * RIGHT_BOTTOM 8 270
             */
            switch (orientation) {
                case ExifInterface.Orientation.RIGHT_BOTTOM:
                    rotate = 270;
                    break;
                case ExifInterface.Orientation.BOTTOM_LEFT:
                    rotate = 180;
                    break;
                case ExifInterface.Orientation.RIGHT_TOP:
                    rotate = 90;
                    break;
            }
            // create a matrix for the manipulation
            Matrix matrix = new Matrix();
            // resize the bit map
            matrix.postScale(widthFactor, heightFactor);
            matrix.postRotate(rotate);
            // recreate the new Bitmap
            Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
            return resizedBitmap;
        }

        private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
            // Raw height and width of image
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {
                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                    inSampleSize *= 2;
                }
            }

            return inSampleSize;
        }

        private float[] calculateFactors(JSONObject params, int width, int height) throws JSONException {
            float widthFactor;
            float heightFactor;
            // String resizeType = params.getString("resizeType");
            String resizeType = RESIZE_TYPE_MAX_PIXEL;
            float desiredWidth = (float) params.getDouble("width");
            float desiredHeight = (float) params.getDouble("height");
            if (resizeType.equals(RESIZE_TYPE_MIN_PIXEL)) {
                widthFactor = desiredWidth / (float) width;
                heightFactor = desiredHeight / (float) height;
                if (widthFactor > heightFactor && widthFactor <= 1.0) {
                    heightFactor = widthFactor;
                } else if (heightFactor <= 1.0) {
                    widthFactor = heightFactor;
                } else {
                    widthFactor = 1.0f;
                    heightFactor = 1.0f;
                }
            } else if (resizeType.equals(RESIZE_TYPE_MAX_PIXEL)) {
                widthFactor = desiredWidth / (float) width;
                heightFactor = desiredHeight / (float) height;
                if (widthFactor == 0.0) {
                    widthFactor = heightFactor;
                } else if (heightFactor == 0.0) {
                    heightFactor = widthFactor;
                } else if (widthFactor > heightFactor) {
                    widthFactor = heightFactor; // scale to fit height
                } else {
                    heightFactor = widthFactor; // scale to fit width
                }
            } else {
                widthFactor = desiredWidth;
                heightFactor = desiredHeight;
            }


            float[] sizes = {widthFactor, heightFactor};
            return sizes;
        }
    }


}

class BitmapUtil {

    // Expect no devices need more than 2048px
    private final static int MAX_BITMAP_SIZE = 2048;
    private final static int NO_MAX_SAMPLE_SIZE = -1;

    public static Bitmap rotateBitmap(Bitmap originalBitmap, int rotation, boolean flipHorizontal) {
        Matrix matrix = new Matrix();
        if (rotation > 0) matrix.postRotate(rotation);
        // Flip bitmap if need
        if (flipHorizontal) matrix.postScale(-1, 1);
        Bitmap rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(),
                matrix, true);
        originalBitmap.recycle();
        return rotatedBitmap;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int displayWidth, int displayHeight, int maxSampleSize, boolean isFillScreen) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > displayHeight || width > displayWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) > displayHeight || (halfWidth / inSampleSize) > displayWidth) {
                inSampleSize *= 2;
            }

            // Check inSampleSize for other orientation, pick the small one
            if (isFillScreen) {
                int orientationInSampleSize = 1;
                while ((halfHeight / orientationInSampleSize) > displayWidth || (halfWidth / orientationInSampleSize) > displayHeight)
                    orientationInSampleSize *= 2;
                inSampleSize = (orientationInSampleSize < inSampleSize) ? orientationInSampleSize : inSampleSize;
            }
        }

        if (maxSampleSize != NO_MAX_SAMPLE_SIZE) {
            if (inSampleSize > maxSampleSize) {
                inSampleSize = maxSampleSize;
            }
        }

        return inSampleSize;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int displayWidth, int displayHeight) {
        return calculateInSampleSize(options, displayWidth, displayHeight, NO_MAX_SAMPLE_SIZE, false);
    }

    public static Bitmap decodeSampledBitmapFromFile(String path, int requiredWidth, int requiredHeight, int maxSampleSize) {
        BitmapFactory.Options options = getBitmapOptions(path);

        int sampleSize = calculateInSampleSize(options, requiredWidth, requiredHeight, maxSampleSize, true);
        options.inSampleSize = sampleSize;
        Log.d("BitmpaUtils", "sampleSize: " + sampleSize);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    public static Bitmap decodeFitToScreenBitmapFromFile(String path, int screenWidth, int screenHeight, int maxSampleSize) {
        //Log.d("BitmpUtils","path: " + path);
        //Log.d("BitmpUtils","GHK screenWidth: " + screenWidth);
        //Log.d("BitmpUtils","GHK screenHeight: " + screenHeight);
        BitmapFactory.Options options = getBitmapOptions(path);

        int bitmapHeight = options.outHeight;
        //Log.d("BitmpUtils","GHK inBitmapHeight: " + bitmapHeight);
        int bitmapWidth = options.outWidth;
        //Log.d("BitmpUtils","GHK inBitmapWidth: " + bitmapWidth);

        // Portrait
        if (bitmapWidth > bitmapHeight) {

            float scaleRatio = (float) screenWidth / (float) bitmapWidth;
            //Log.d("BitmpUtils","scaleRatio: " + scaleRatio);
            bitmapWidth *= scaleRatio;
            bitmapHeight *= scaleRatio;

        } else {
            float scaleRatio = (float) screenHeight / (float) bitmapHeight;
            //Log.d("BitmpUtils","scaleRatio: " + scaleRatio);
            bitmapWidth *= scaleRatio;
            bitmapHeight *= scaleRatio;
        }

        //Log.d("BitmpUtils","GHK requestedOutBitmapWidth: " + bitmapWidth);
        //Log.d("BitmpUtils","GHK requestedOutBitmapHeight: " + bitmapHeight);

        int inSampleSize = calculateInSampleSize(options, bitmapWidth, bitmapHeight, maxSampleSize, false);
        Log.d("BitmpaUtils", "sampleSize: " + inSampleSize);
        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;

        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        //Log.d("BitmpUtils","GHK outBitmapWidth: " + bitmap.getWidth());
        //Log.d("BitmpUtils","GHK outBitmapHeight: " + bitmap.getHeight());
        return bitmap;
    }

    public static Bitmap decodeSampledBitmap(String path, int requiredWidth, int requiredHeight) {
        try {
            BitmapFactory.Options options = getBitmapOptions(path);

            int sampleSize = calculateInSampleSize(options, requiredWidth, requiredHeight);
            Log.d("BitmpaUtils", "texture - sampleSize: " + sampleSize);
            options.inSampleSize = sampleSize;
            options.inJustDecodeBounds = false;
            Bitmap result = BitmapFactory.decodeFile(path, options);
            if (result == null)
                throw new UnableToDecodeBitmapException(new Exception("Possibly CMYK jpeg"));
            return result;
        } catch (Exception ex) {
            throw new UnableToDecodeBitmapException(ex);
        }
    }

    public static boolean writeToFile(Bitmap bitmap, File outputFile) throws FileNotFoundException {
        FileOutputStream outputStream = new FileOutputStream(outputFile);
        boolean isSuccess = false;
        try {
            isSuccess = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        } catch (Exception e) {
            Log.e("writeToFile", e.toString());
        } finally {
            if (outputStream == null) return false;
            try {
                outputStream.close();
            } catch (Exception ignore) {
            }
        }
        return isSuccess;
    }

    public static class UnableToDecodeBitmapException extends RuntimeException {
        public UnableToDecodeBitmapException(Exception innerEx) {
            super("Unable to decode bitmap", innerEx);
        }
    }

    /**
     * Resize large bitmap by pieces, calculate the size by maxDisplayDimension provided,
     * and limit the max size to MAX_BITMAP_SIZE
     *
     * @param context             this context
     * @param source              source file
     * @param maxDisplayDimension
     * @return false if too small to resize or resize failed
     */
    public static Bitmap decodeLargeBitmap(Context context, File source, int maxDisplayDimension, int maxSampleSize) {
        BitmapFactory.Options options = getBitmapOptions(source.toString());
        int sourceWidth = options.outWidth;
        int sourceHeight = options.outHeight;

        int requiredWidth, requiredHeight;
        if (sourceWidth > sourceHeight) {
            requiredHeight = maxDisplayDimension;
            requiredWidth = (int) ((float) requiredHeight / (float) sourceHeight * (float) sourceWidth);
            float ratio = (float) requiredHeight / (float) requiredWidth;
            if (requiredWidth > MAX_BITMAP_SIZE) {
                requiredWidth = MAX_BITMAP_SIZE;
                requiredHeight = (int) (MAX_BITMAP_SIZE * ratio);
            }
        } else {
            requiredWidth = maxDisplayDimension;
            requiredHeight = (int) ((float) requiredWidth / (float) sourceWidth * (float) sourceHeight);
            float ratio = (float) requiredWidth / (float) requiredHeight;
            if (requiredHeight > MAX_BITMAP_SIZE) {
                requiredHeight = MAX_BITMAP_SIZE;
                requiredWidth = (int) (MAX_BITMAP_SIZE * ratio);
            }
        }
        return decodeLargeBitmap(context, source, requiredWidth, requiredHeight, maxSampleSize);
    }

    /**
     * Resize large bitmap by pieces, calculate the size by maxDisplayDimension provided
     *
     * @param context        this context
     * @param source         source file
     * @param requiredWidth  new width
     * @param requiredHeight new height
     * @return false if too small to resize or resize failed
     */
    public static Bitmap decodeLargeBitmap(Context context, File source, int requiredWidth, int requiredHeight, int maxSampleSize) {
        BitmapFactory.Options options = getBitmapOptions(source.toString());
        int sourceWidth = options.outWidth;
        int sourceHeight = options.outHeight;

        int sampleSize = calculateInSampleSize(options, requiredWidth, requiredHeight, maxSampleSize, false);
//        if (sourceWidth <= requiredWidth || sourceHeight <= requiredHeight || sampleSize == 1 && requiredWidth < MAX_BITMAP_SIZE && requiredHeight < MAX_BITMAP_SIZE) {
//            options.inSampleSize = sampleSize;
//            options.inJustDecodeBounds = false;
//            return BitmapFactory.decodeFile(source.toString(), options);
//        }

        // Limit the block size to prevent out of memory
        int BLOCK_SIZE_LIMIT = 1024;
        int blockWidth = sourceWidth;
        int blockHeight = sourceHeight;

        // Find the suitable block size for balancing resize time and heap usage
        int countBlock = 2;
        while (sourceWidth / countBlock > BLOCK_SIZE_LIMIT) {
            countBlock++;
        }
        blockWidth = sourceWidth / countBlock;
        countBlock = 2;
        while (sourceHeight / countBlock > BLOCK_SIZE_LIMIT) {
            countBlock++;
        }
        blockHeight = sourceHeight / countBlock;

        int noOfWidth = (int) Math.ceil((float) sourceWidth / (float) blockWidth);
        int noOfHeight = (int) Math.ceil((float) sourceHeight / (float) blockHeight);
        double ratioWidth = (double) requiredWidth / (double) sourceWidth;
        double ratioHeight = (double) requiredHeight / (double) sourceHeight;
        int resizedBlockWidth = (int) Math.ceil(blockWidth * ratioWidth);
        int resizedBlockHeight = (int) Math.ceil(blockHeight * ratioHeight);

        Log.d("BitmpUtils", "block: " + blockWidth + ", " + blockHeight);
        Log.d("BitmpUtils", "noOfBlock: " + noOfWidth + ", " + noOfHeight);
        Log.d("BitmpUtils", "resizedBlock: " + resizedBlockWidth + ", " + resizedBlockHeight);
        Log.d("BitmpUtils", "name: " + source.toString());

        try {
            Bitmap fullImage = Bitmap.createBitmap(requiredWidth, requiredHeight, Bitmap.Config.ARGB_8888);
            fullImage.setHasAlpha(false);
            Canvas canvas = new Canvas(fullImage);
            InputStream sourceInputStream = context.getContentResolver().openInputStream(Uri.fromFile(source));
            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(sourceInputStream, false);
            int count = 0;
            BitmapFactory.Options optionsForDecodeRegion = new BitmapFactory.Options();
            optionsForDecodeRegion.inPreferredConfig = Bitmap.Config.ARGB_8888;
            for (int y = 0; y < noOfHeight; y++) {
                for (int x = 0; x < noOfWidth; x++) {

                    Rect region = new Rect(blockWidth * x,
                            blockHeight * y,
                            (blockWidth * x) + blockWidth,
                            (blockHeight * y) + blockHeight);
                    Bitmap blockBitmap = decoder.decodeRegion(region, optionsForDecodeRegion);
                    blockBitmap.setHasAlpha(false);

                    /**
                     * To process scaling, image must be smaller than target image.
                     */
                    if (blockBitmap.getWidth() > resizedBlockWidth && blockBitmap.getHeight() > resizedBlockHeight)
                        blockBitmap = getScaledBitmap(blockBitmap, resizedBlockWidth, resizedBlockHeight);

                    canvas.drawBitmap(blockBitmap, x * resizedBlockWidth, y * resizedBlockHeight, new Paint());
                    blockBitmap.recycle();

                    Log.d("BitmpUtils", "total: " + count++ + "/" + noOfWidth * noOfHeight);
                }
            }
            canvas.save();
            return fullImage;
        } catch (Exception e) {
            throw new UnableToDecodeBitmapException(e);
        }
    }

    /**
     * Scale the bitmap
     *
     * @param bitmap    input source
     * @param newWidth  new width
     * @param newHeight new height
     * @return resized bitmap
     */
    public static Bitmap getScaledBitmap(Bitmap bitmap, int newWidth, int newHeight) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        bitmap.recycle();
        return resizedBitmap;
    }

    /**
     * Get the bitmap options from bitmap without open the file
     *
     * @param file
     * @return options
     */
    private static BitmapFactory.Options getBitmapOptions(String file) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file, options);
        return options;
    }
}
