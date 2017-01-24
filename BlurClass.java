
//This class takes care of getting blur of images in android. The image can be a drawable, bitmap, or a id of the drawable,
//Helpful in changing saturation, brightness and contrast of bitmaps.
//Can convert drawable to bitmaps and vice-versa.
//various methods in this class are taken from Stack Overflow and combined.

public class BlurImagesClass {



    public static Bitmap convertToBmpfromResourceDrawable(Context context, int drawableId) {
        return BitmapFactory.decodeResource(context.getResources(), drawableId);
    }

    public static Bitmap convetToBmpFromDrawable (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
    public static Drawable bitmapToDrawable(Context context,Bitmap bmp)
    { Drawable drawable = new BitmapDrawable(context.getResources(),bmp);
        return drawable;
    }


    public static Bitmap reduceBmpSize(Context context,Bitmap bmp) {
        Options options = new Options();
        options.inSampleSize = 8;
        //search how to resize bmp using inSampleSize

        int width = bmp.getWidth();
        int height = bmp.getHeight();
        return Bitmap.createScaledBitmap(bmp, width/4, height/4, true);
    }


    //  * Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>
    public static Bitmap fastblur(Bitmap sentBitmap, int radius) {

        Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        if (radius < 1) {
            radius = 20;
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];

        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;

            }

        }
        bitmap.setPixels(pix, 0, w, 0, 0, w, h);

        return (bitmap);
    }


    //hey there s a big and important difference between contrast and saturation
    public static Bitmap changeBitmapContrastBrightness(Bitmap bmp, float contrast, float brightness)
    {
         /*   @param bmp input bitmap
            * @param contrast 0..10 1 is default
        * @param brightness -255..255 0 is default
        * @return new bitmap  */
        
        
        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        contrast, 0, 0, 0, brightness,
                        0, contrast, 0, 0, brightness,
                        0, 0, contrast, 0, brightness,
                        0, 0, 0, 1, 0
                });

        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());
        Canvas canvas = new Canvas(ret);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bmp, 0, 0, paint);
        
        return ret; 
    }



    public static Bitmap changeSaturation(Context context, Bitmap bmp)
    {

        //  http://www.steves-digicams.com/knowledge-center/brightness-contrast-saturation-and-sharpness.html#b
       // http://stackoverflow.com/questions/25453310/animate-images-saturation
       // http://stackoverflow.com/questions/4354939/understanding-the-use-of-colormatrix-and-colormatrixcolorfilter-to-modify-a-draw
        ColorMatrix cm  = new ColorMatrix();
       cm.setSaturation(0.6f);//checkitformanyvalueswhenfreechoosethebest
       Drawable drawable  = bitmapToDrawable(context,bmp);
        drawable.setColorFilter(colorFilterFromColorMatrix(cm,1.32f));
        bmp = convetToBmpFromDrawable(drawable);
        return bmp;
    }
    public static ColorFilter colorFilterFromColorMatrix(ColorMatrix cm,float x)
    {
        ColorFilter matrixColorFilter = new ColorMatrixColorFilter(cm );
        return  matrixColorFilter;
    }
  public static Bitmap overlapBitmapOverItsBlurredBackgroundWithFitCenterScale(Context context,Bitmap source,int backgroundWidth, int backgroundHeight)
    {
        int sourceHeight = source.getHeight();
        int sourceWidth  = source.getWidth();
        int marginTop = 0;
        int marginLeft = 0;

        /*
          Let c be the multiplier which should be multiplied with sourceHeight and sourceWidth to make fitCenter with backround
          then sourceWidth*c<=backgroundWidth and sourceHeight*c <= backgroundHeight then only they can fit into background
          it means that c should be min of backgroundWidth/sourceWidth and backgroundHeight/sourceHeight;
         */

        float c = Math.min((float)backgroundHeight/sourceHeight,(float)backgroundWidth/sourceWidth);

        sourceWidth = Math.round(sourceWidth*c);
        sourceHeight = Math.round(sourceHeight*c);
        source = getResizedBitmap(source,sourceHeight,sourceWidth);

        marginTop = (backgroundHeight-sourceHeight)/2;
        marginLeft = (backgroundWidth-sourceWidth)/2;

        Bitmap blurBackround = getResizedBitmap(source,backgroundHeight,backgroundWidth);
        blurBackround = fastblur(blurBackround,10);
        blurBackround = changeBitmapContrastBrightness(blurBackround,0.9f,-25);

        Log.d("ankit",sourceHeight+" in BlurClass "+sourceWidth);

        Bitmap bitmapOverlay = Bitmap.createBitmap(backgroundWidth,backgroundHeight,blurBackround.getConfig());
        Canvas canvas = new Canvas(bitmapOverlay);
        canvas.drawBitmap(blurBackround, new Matrix(), null);
        canvas.drawBitmap(source,marginLeft,marginTop,null);
        return bitmapOverlay;
    }

    public static int getScreenWidth(Activity activity)
    {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int width = displaymetrics.widthPixels;
        return  width;
    }
    public static int dpToPx(Context context,int dp) {
        DisplayMetrics displayMetrics =context.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static Bitmap getResizedBitmap(Bitmap bitmap, int newHeight, int newWidth)
    {
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false);
    }
    public static File convertBitmaptoFile(Context context,Bitmap bitmap,String fileName) {
        File f = new File(context.getCacheDir(), fileName);
        try {
            f.createNewFile();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
            byte[] bitmapdata = bos.toByteArray();

//write the bytes in file
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();
            
            //handling io exceptions is must in android.
        }catch (IOException e)
        {
            if(e!=null)
            Log.d("ankit",e.getMessage());
        }
        return f;
    }

}


