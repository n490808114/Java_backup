import java.io.InputStream;  
import java.lang.ref.SoftReference;  
import java.net.HttpURLConnection;  
import java.net.URL;  
import java.net.URLConnection;  
import java.util.HashMap;  
import android.content.Context;  
import android.graphics.Bitmap;  
import android.graphics.BitmapFactory;  
import android.os.Handler;  
import android.os.Handler.Callback;  
import android.os.Message;  
import android.widget.ImageView;  
  
  
/** 
 * 功能说明：异步加载图片 
 * 
 */  
  
public class AsyncImageLoaderCore {  
    public Context context; // 做本地缓存时会用到  
    public HashMap<String, SoftReference<Bitmap>> imageCache;// 软引用集合  
     
    public AsyncImageLoaderCore(Context context) {  
        this.context = context;  
        this.imageCache = new HashMap<String, SoftReference<Bitmap>>();  
    }  
  
  
    public Bitmap loadBitmap(final String imageUrl, final ImageView imageView, final ImageCallback imageCallback) {  
        if (imageCache.containsKey(imageUrl)) {  
            SoftReference<Bitmap> softReference = imageCache.get(imageUrl);  
            if (softReference.get() != null)  
                return softReference.get();  
        }  
          
        final Handler handler = new Handler(new Callback() {  
            @Override  
            public boolean handleMessage(Message msg) {  
                imageCallback.imageLoaded((Bitmap) msg.obj, imageView, imageUrl);  
                return false;  
            }  
        });  
          
        new Thread() {  
            @Override  
            public void run() {  
                Bitmap bitmap = null;  
                try {  
                    bitmap = getHttpBitmap(imageUrl);  
                } catch (Exception e) {  
                    e.printStackTrace();  
                    return;  
                }  
                  
                if (null != bitmap) {  
                    imageCache.put(imageUrl, new SoftReference<Bitmap>(bitmap));  
                    handler.sendMessage(handler.obtainMessage(0, bitmap));  
                }  
            }  
        }.start();  
        return null;  
    }  
    private final int MAX_PIC_LENGTH = 200000;// 最大字节长度限制[可调,最好不要超过200000]  
    private final int SAMPLE_SIZE = 14;// 裁剪图片比列(1/14)[可调]  
    /** 
     * 获取网络图片 
     */  
    private Bitmap getHttpBitmap(String imgUrl) throws Exception {  
        URL htmlUrl = new URL(imgUrl);  
        URLConnection connection = htmlUrl.openConnection();  
        HttpURLConnection conn = (HttpURLConnection) connection;  
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {  
            InputStream inputStream = conn.getInputStream();  
            byte[] bytes = changeToBytes(inputStream);  
            if (bytes.length < MAX_PIC_LENGTH) {  
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);  
            } else if (bytes.length < MAX_PIC_LENGTH * SAMPLE_SIZE) {  
                BitmapFactory.Options options = new BitmapFactory.Options();  
                options.inJustDecodeBounds = false;  
                options.inSampleSize = SAMPLE_SIZE;  
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);  
            }  
        }  
        return null;  
    }  
  
  
    /** 
     * 将流转换成字节数组 
     */  
  
    public byte[] changeToBytes(InputStream inputStream) throws Exception  
    {  
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();  
        byte[] buffer = new byte[1024];// 每次读取的字节长度  
        int len = 0;  
        while ((len = inputStream.read(buffer)) != -1)  
        {  
            outputStream.write(buffer, 0, len);  
        }  
        inputStream.close();  
        return outputStream.toByteArray();  
    }  
    /** 
     * 异步加载资源回调接口 
     */  
    public interface ImageCallback {  
        public void imageLoaded(Bitmap bitmap, ImageView imageView, String imageUrl);  
    }  
} 