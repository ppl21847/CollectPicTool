/*
 * Copyright (C) 2014 pengjianbo(pengjianbosoft@gmail.com), Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package cn.finalteam.galleryfinal.widget.crop;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.opengl.GLES10;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

import cn.finalteam.galleryfinal.GalleryFinal;
import cn.finalteam.galleryfinal.R;
import cn.finalteam.galleryfinal.utils.ILogger;
import cn.finalteam.galleryfinal.utils.MediaScanner;
import cn.finalteam.toolsfinal.io.FilenameUtils;

/*
 * Modified from original in AOSP.
 */
public abstract class CropImageActivity extends MonitoredActivity {

    private static final int SIZE_DEFAULT = 2048;
    private static final int SIZE_LIMIT = 4096;

    private final Handler handler = new Handler();

    private int aspectX;
    private int aspectY;

    // Output image
    private int maxX;
    private int maxY;
    private int exifRotation;

    private Uri sourceUri;

    private boolean isSaving;

    private int sampleSize;
    private RotateBitmap rotateBitmap;
    private CropImageView imageView;
    private HighlightView cropView;

    private boolean cropEnabled;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setupWindowFlags();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void setupWindowFlags() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    public void initCrop(CropImageView imageView, boolean square, int maxX, int maxY) {
        if (square) {
            this.aspectX = 1;
            this.aspectY = 1;
        }
        this.maxX = maxX;
        this.maxY = maxY;
        this.imageView = imageView;

        imageView.context = this;
        imageView.setRecycler(new ImageViewTouchBase.Recycler() {
            @Override
            public void recycle(Bitmap b) {
                b.recycle();
                System.gc();
            }
        });
    }

    public void setSourceUri(Uri sourceUri) {
        if(rotateBitmap != null) {
            rotateBitmap.recycle();
            rotateBitmap = null;
        }
        this.sourceUri = sourceUri;
        this.isSaving = false;
        this.sampleSize = 0;
        this.rotateBitmap = null;
        this.cropView = null;
        this.imageView.clear();
        if (sourceUri != null) {
            exifRotation = CropUtil.getExifRotation(CropUtil.getFromMediaUri(this, getContentResolver(), sourceUri));
            Log.e("sssssssss","get exifRotation: "+exifRotation);
            InputStream is = null;
            try {
                sampleSize = calculateBitmapSampleSize(sourceUri);
                is = getContentResolver().openInputStream(sourceUri);
                BitmapFactory.Options option = new BitmapFactory.Options();
                option.inSampleSize = sampleSize;
                //option.inSampleSize = 1;
                rotateBitmap = new RotateBitmap(BitmapFactory.decodeStream(is, null, option), exifRotation);
            } catch (IOException e) {
                ILogger.e(e);
//                setCropSaveException(e);
            } catch (OutOfMemoryError e) {
                ILogger.e(e);
//                setCropSaveException(e);
            } finally {
                CropUtil.closeSilently(is);
            }
        }

        //if (rotateBitmap == null) {
        //    finish();
        //    return;
        //}
        if (rotateBitmap != null) {
            startCrop();
        }
    }

    private int calculateBitmapSampleSize(Uri bitmapUri) throws IOException {
        InputStream is = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            is = getContentResolver().openInputStream(bitmapUri);
            BitmapFactory.decodeStream(is, null, options); // Just get image size
        } finally {
            CropUtil.closeSilently(is);
        }

        int maxSize = getMaxImageSize();
        int sampleSize = 1;
        while (options.outHeight / sampleSize > maxSize || options.outWidth / sampleSize > maxSize) {
            sampleSize = sampleSize << 1;
        }
        return sampleSize;
    }

    private int getMaxImageSize() {
        int textureLimit = getMaxTextureSize();
        if (textureLimit == 0) {
            return SIZE_DEFAULT;
        } else {
            return Math.min(textureLimit, SIZE_LIMIT);
        }
    }

    private int getMaxTextureSize() {
        // The OpenGL texture size is the maximum size that can be drawn in an ImageView
        int[] maxSize = new int[1];
        GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxSize, 0);
        return maxSize[0];
    }

    private void startCrop() {
        if (isFinishing()) {
            return;
        }
        imageView.setImageRotateBitmapResetBase(rotateBitmap, true);
        CropUtil.startBackgroundJob(this, null, getResources().getString(R.string.waiting),
                new Runnable() {
                    public void run() {
                        final CountDownLatch latch = new CountDownLatch(1);
                        handler.post(new Runnable() {
                            public void run() {
                                if (imageView.getScale() == 1F) {
                                    imageView.center();
                                }
                                latch.countDown();
                            }
                        });
                        try {
                            latch.await();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        new Cropper().crop();
                    }
                }, handler
        );
    }

    public void setCropEnabled(boolean enabled) {
        this.cropEnabled = enabled;
        if ( enabled ) {
            startCrop();
        }
    }

    private class Cropper {

        private void makeDefault() {
            if (rotateBitmap == null) {
                return;
            }

            HighlightView hv = new HighlightView(imageView, GalleryFinal.getGalleryTheme().getCropControlColor());
            final int width = rotateBitmap.getWidth();
            final int height = rotateBitmap.getHeight();

            Rect imageRect = new Rect(0, 0, width, height);

            // Make the default size about 4/5 of the width or height
            int cropWidth = Math.min(width, height) * 4 / 5;
            @SuppressWarnings("SuspiciousNameCombination")
            int cropHeight = cropWidth;

            if (aspectX != 0 && aspectY != 0) {
                if (aspectX > aspectY) {
                    cropHeight = cropWidth * aspectY / aspectX;
                } else {
                    cropWidth = cropHeight * aspectX / aspectY;
                }
            }

            int x = (width - cropWidth) / 2;
            int y = (height - cropHeight) / 2;

            RectF cropRect = new RectF(x, y, x + cropWidth, y + cropHeight);
            hv.setup(imageView.getUnrotatedMatrix(), imageRect, cropRect, aspectX != 0 && aspectY != 0);
            imageView.add(hv);
        }

        public void crop() {
            handler.post(new Runnable() {
                public void run() {
                    makeDefault();
                    imageView.invalidate();
                    if (imageView.highlightViews.size() == 1) {
                        cropView = imageView.highlightViews.get(0);
                        cropView.setFocus(true);
                    }
                }
            });
        }
    }

    /**
     * 保存裁剪图片
     * @param saveFile
     */
    public void onSaveClicked( File saveFile) {
        if (cropView == null || isSaving) {
            return;
        }
        isSaving = true;

        Bitmap croppedImage;
        Rect r = cropView.getScaledCropRect(sampleSize);
        int width = r.width();
        int height = r.height();

        int outWidth = width;
        int outHeight = height;
        if (maxX > 0 && maxY > 0 && (width > maxX || height > maxY)) {
            float ratio = (float) width / (float) height;
            if ((float) maxX / (float) maxY > ratio) {
                outHeight = maxY;
                outWidth = (int) ((float) maxY * ratio + .5f);
            } else {
                outWidth = maxX;
                outHeight = (int) ((float) maxX / ratio + .5f);
            }
        }

        if(exifRotation == 90 || exifRotation == 270){
            int tmp = outHeight;
            outHeight = outWidth;
            outWidth = tmp;
        }
        try {
            croppedImage = decodeRegionCrop(r, outWidth, outHeight);
        } catch (IllegalArgumentException e) {
            setCropSaveException(e);
            return;
        }

        if (croppedImage != null) {
            Log.e("ssssssssssssss","exifRotation: "+exifRotation);
            imageView.setImageRotateBitmapResetBase(new RotateBitmap(croppedImage, exifRotation), true);
            imageView.center();
            imageView.highlightViews.clear();
        }
        saveImage(croppedImage, saveFile);
    }

    private void saveImage(Bitmap croppedImage, final File saveFile) {
        if (croppedImage != null) {
            final Bitmap b = croppedImage;
            Log.e("sssssssss","save Bitmap width:"+croppedImage.getWidth()+",height:"+croppedImage.getHeight());
            CropUtil.startBackgroundJob(this, null, getResources().getString(R.string.saving),
                    new Runnable() {
                        public void run() {
                            saveOutput(b, saveFile);
                        }
                    }, handler
            );
        }
    }

    private Bitmap decodeRegionCrop(Rect rect, int outWidth, int outHeight) {
        // Release memory now
        clearImageView();

        InputStream is = null;
        Bitmap croppedImage = null;
        try {
            is = getContentResolver().openInputStream(sourceUri);
            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(is, false);
            final int width = decoder.getWidth();
            final int height = decoder.getHeight();

            if (exifRotation != 0) {
                // Adjust crop area to account for image rotation
                Matrix matrix = new Matrix();
                matrix.setRotate(-exifRotation);

                RectF adjusted = new RectF();
                matrix.mapRect(adjusted, new RectF(rect));

                // Adjust to account for origin at 0,0
                adjusted.offset(adjusted.left < 0 ? width : 0, adjusted.top < 0 ? height : 0);
                rect = new Rect((int) adjusted.left, (int) adjusted.top, (int) adjusted.right, (int) adjusted.bottom);
            }

            try {
                croppedImage = decoder.decodeRegion(rect, new BitmapFactory.Options());
                if (croppedImage != null && (rect.width() > outWidth || rect.height() > outHeight)) {
                    Matrix matrix = new Matrix();
                    matrix.postScale((float) outWidth / rect.width(), (float) outHeight / rect.height());
                    croppedImage = Bitmap.createBitmap(croppedImage, 0, 0, croppedImage.getWidth(), croppedImage.getHeight(), matrix, true);
                }
            } catch (IllegalArgumentException e) {
                // Rethrow with some extra information
                throw new IllegalArgumentException("Rectangle " + rect + " is outside of the image ("
                        + width + "," + height + "," + exifRotation + ")", e);
            }

        } catch (IOException e) {
            ILogger.e(e);
            setCropSaveException(e);
        } catch (OutOfMemoryError e) {
            ILogger.e(e);
            setCropSaveException(e);
        } finally {
            CropUtil.closeSilently(is);
        }
        return croppedImage;
    }

    private void clearImageView() {
        imageView.clear();
        if (rotateBitmap != null) {
            rotateBitmap.recycle();
        }
        System.gc();
    }

    /**
     * 读取图片属性：旋转的角度
     * @param path 图片绝对路径
     * @return degree旋转的角度
     */
    public int readPictureDegree(String path) {
        int degree  = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    /**
     * 将图片按照某个角度进行旋转
     *
     * @param bm
     *            需要旋转的图片
     * @param degree
     *            旋转角度
     * @return 旋转后的图片
     */
    public Bitmap rotateBitmapByDegree(Bitmap bm, int degree) {
        Bitmap returnBm = null;

        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        try {
            // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            returnBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),
                    bm.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
        }
        if (returnBm == null) {
            returnBm = bm;
        }
        if (bm != returnBm) {
            bm.recycle();
        }
        return returnBm;
    }

    /**
     * 旋转图片
     * @param angle
     * @param bitmap
     * @return Bitmap
     */
    public Bitmap rotaingImageView(int angle , Bitmap bitmap) {
        //旋转图片 动作
        Matrix matrix = new Matrix();;
        matrix.postRotate(angle);
        System.out.println("angle2=" + angle);
        // 创建新的图片
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizedBitmap;
    }


    private void saveOutput(Bitmap croppedImage, File saveFile) {
        Log.e("sssssssss","save in Bitmap width:"+croppedImage.getWidth()+",height:"+croppedImage.getHeight());
        if (saveFile != null) {
            saveToSd(croppedImage,saveFile);

            /**
             * 获取图片的旋转角度，有些系统把拍照的图片旋转了，有的没有旋转
             */
            Log.e("ssssssssss","exifRotation is: "+exifRotation);
            /*if( exifRotation != 0){
                BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
                Bitmap cameraBitmap = BitmapFactory.decodeFile(saveFile.getAbsolutePath(), bitmapOptions);
                Bitmap bitmap = cameraBitmap;
                 // 把图片旋转为正的方向
                bitmap = rotaingImageView(exifRotation, bitmap);
                saveToSd(bitmap,saveFile);
                bitmap.recycle();
            }*/

           /*CropUtil.copyExifRotation(
                    CropUtil.getFromMediaUri(this, getContentResolver(), sourceUri),
                    CropUtil.getFromMediaUri(this, getContentResolver(), Uri.fromFile(saveFile))
            ); */
            Bitmap saveImg = BitmapFactory.decodeFile(saveFile.getAbsolutePath());
            Log.e("ssssssssss","saveImg width: "+saveImg.getWidth()+",height: "+saveImg.getHeight());
            setCropSaveSuccess(saveFile);
            Log.e("ssssssssss","2222更新截取的图片至相册");
            Log.e("ssssssssss","2222saveFile: "+saveFile);
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,Uri.fromFile(saveFile)));

            // 最后通知图库更新
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + saveFile)));

            try {
                MediaStore.Images.Media.insertImage(getContentResolver(), saveFile.getAbsolutePath(), saveFile.getName(), null);
            } catch(FileNotFoundException e) {
                e.printStackTrace();
            }


            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(saveFile)));
        }

        final Bitmap b = croppedImage;
        handler.post(new Runnable() {
            public void run() {
                imageView.clear();
                b.recycle();
            }
        });
    }

    private void saveToSd(Bitmap croppedImage, File saveFile) {
        Log.e("sssssssss","save Bitmap width:"+croppedImage.getWidth()+",height:"+croppedImage.getHeight());
        if(exifRotation != 0){
            croppedImage = rotateBitmapByDegree(croppedImage,exifRotation);
        }

       /* OutputStream outputStream = null;
        try {
            outputStream = getContentResolver().openOutputStream(Uri.fromFile(saveFile));
            if (outputStream != null) {
                String ext = FilenameUtils.getExtension(saveFile.getAbsolutePath());
                Bitmap.CompressFormat format;
                if ( ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg") ) {
                    format = Bitmap.CompressFormat.JPEG;
                    croppedImage.compress(format, 100, outputStream);
                } else {
                    format = Bitmap.CompressFormat.PNG;
                    croppedImage.compress(format, 100, outputStream);
                }
            }
        } catch (IOException e) {
            setCropSaveException(e);
            ILogger.e(e);
        } finally {
            CropUtil.closeSilently(outputStream);
        }*/

        try {
            FileOutputStream fos = new FileOutputStream(saveFile);
            String ext = FilenameUtils.getExtension(saveFile.getAbsolutePath());
            Bitmap.CompressFormat format;
            if ( ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg") ) {
                format = Bitmap.CompressFormat.JPEG;
            } else {
                format = Bitmap.CompressFormat.PNG;
            }
            croppedImage.compress(format, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rotateBitmap != null) {
            rotateBitmap.recycle();
        }
    }

    @Override
    public boolean onSearchRequested() {
        return false;
    }

    public boolean isSaving() {
        return isSaving;
    }

    public abstract void setCropSaveSuccess(File file);

    public abstract void setCropSaveException(Throwable throwable);

}
