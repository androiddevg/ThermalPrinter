package com.example.thermalprinter;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interceptors.HttpLoggingInterceptor;
import com.androidnetworking.interfaces.AnalyticsListener;
import com.androidnetworking.interfaces.BitmapRequestListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;
import java.io.ByteArrayOutputStream;


/**
 * This class is responsible to generate a static sales receipt and to print that receipt
 */
public class PrintPosReceipt {

	private static String TAG="KOT_LOGS";
	static Target mTarget = null;
	static Dialog dialog;
	static Context mContext;
	public static final int BITMAP_ZOOM_NONE   = 0;
	public static final int BITMAP_ZOOM_WIDTH  = 1;
	public static final int BITMAP_ZOOM_HEIGHT = 2;
	public static final int BITMAP_ZOOM_BOTH   = 3;
	public static final int TYPE_PAPER_WIDTH_58MM = 0;
	public static final int TYPE_PAPER_WIDTH_76MM = 1;
	public static final int TYPE_PAPER_WIDTH_80MM = 2;

	private static int mPaperWidthType = TYPE_PAPER_WIDTH_58MM;
	private static final int PRINTER_BUFFER_LEN = 32576;
	private static final byte code_page[][] = {
			{(byte)0x1D,(byte)0xFE},
			{(byte)0x1C,(byte)0xFF},
	};

	private static final int[] bmp_byte_width = {
			48,//58mm
			50,//76mm
			72,//80mm
	};

	private static final int[] dots_per_line = {
			384,//58mm
			400,//76mm
			576,//80mm
	};

	static public byte[] cmdGSv0pwLwHhLhHd(int p, int wL, int wH, int hL, int hH, byte[] d) {
		int i;
		byte[] cmd = new byte[d.length+8];
		cmd [0] =  (byte) 0x1D;
		cmd [1] =  (byte) 0x76;
		cmd [2] =  (byte) 0x30;
		cmd [3] =  (byte) p;
		cmd [4] =  (byte) wL;
		cmd [5] =  (byte) wH;
		cmd [6] =  (byte) hL;
		cmd [7] =  (byte) hH;
		System.arraycopy(d, 0, cmd, 8, d.length);
		return cmd;
	}

	public static boolean  printPOSReceipt(Context context){
		mContext=context;
		if(MainActivity.BLUETOOTH_PRINTER.IsNoConnection()){
			return false;
		}

		if(MainActivity.BLUETOOTH_PRINTER.IsNoConnection()){
			return false;
		}

		byte[] cc = new byte[]{0x1B,0x21,0x00};  // 0- normal size text
		byte[] bb = new byte[]{0x1B,0x21,0x08};  // 1- only bold text
		byte[] bb2 = new byte[]{0x1B,0x21,0x20}; // 2- bold with medium text
		byte[] bb3 = new byte[]{0x1B,0x21,0x10}; // 3- bold with large text

		//LF = Line feed
		MainActivity.BLUETOOTH_PRINTER.Begin();
		MainActivity.BLUETOOTH_PRINTER.LF();
		MainActivity.BLUETOOTH_PRINTER.LF();
		MainActivity.BLUETOOTH_PRINTER.SetAlignMode((byte) 1);//CENTER
		MainActivity.BLUETOOTH_PRINTER.SetLineSpacing((byte) 30);	//30 * 0.125mm
		MainActivity.BLUETOOTH_PRINTER.SetFontEnlarge((byte) 0x1B);//normal
		MainActivity.BLUETOOTH_PRINTER.BT_Write(bb2);
		MainActivity.BLUETOOTH_PRINTER.BT_Write("Friends Book");

		MainActivity.BLUETOOTH_PRINTER.SetAlignMode((byte) 1);//CENTER
		MainActivity.BLUETOOTH_PRINTER.SetLineSpacing((byte) 30);	//30 * 0.125mm
		MainActivity.BLUETOOTH_PRINTER.SetFontEnlarge((byte) 0x00);//normal
		MainActivity.BLUETOOTH_PRINTER.BT_Write("\nName : Hailie trembl");


		MainActivity.BLUETOOTH_PRINTER.LF();
		MainActivity.BLUETOOTH_PRINTER.SetAlignMode((byte) 1);
		MainActivity.BLUETOOTH_PRINTER.SetLineSpacing((byte) 30);
		MainActivity.BLUETOOTH_PRINTER.SetFontEnlarge((byte) 0x00);

		//BT_Write() method will initiate the printer to start printing.
		MainActivity.BLUETOOTH_PRINTER.BT_Write("\n\nBirth Date: " + "1/11/1953");

		MainActivity.BLUETOOTH_PRINTER.LF();
		MainActivity.BLUETOOTH_PRINTER.SetAlignMode((byte) 0);//LEFT
		MainActivity.BLUETOOTH_PRINTER.BT_Write("\n"+context.getResources().getString(R.string.print_line)+"\n");
		MainActivity.BLUETOOTH_PRINTER.LF();

		MainActivity.BLUETOOTH_PRINTER.SetLineSpacing((byte) 30);	//50 * 0.125mm
		MainActivity.BLUETOOTH_PRINTER.SetFontEnlarge((byte) 0x00);//normal font
        //BT_Write() method will initiate the printer to start printing.
		MainActivity.BLUETOOTH_PRINTER.BT_Write("\nMobile No : 801-900-2540");
		MainActivity.BLUETOOTH_PRINTER.BT_Write("\nAddress : 2590 Walton Street,Provo, Utah(UT), 84606");
		MainActivity.BLUETOOTH_PRINTER.BT_Write("\nEmail Address : hailie_trembl@gmail.com");
		MainActivity.BLUETOOTH_PRINTER.BT_Write("\nSocial Security Number : 646-10-2258");
		MainActivity.BLUETOOTH_PRINTER.BT_Write("\nFavorite Color: " + "White" +
				"\nFavorite Movie: " + "U Turn(1997)"+
				"\nFavorite Music:" + "Rock music"+
				"\nFavorite Song:" + "Old Friends(by Jasmine Thompson)"+
				"\nFavorite Song:" + "Soccer");

		MainActivity.BLUETOOTH_PRINTER.LF();
		MainActivity.BLUETOOTH_PRINTER.SetAlignMode((byte) 0);//LEFT
		MainActivity.BLUETOOTH_PRINTER.BT_Write(context.getResources().getString(R.string.print_line)+"\n");
		MainActivity.BLUETOOTH_PRINTER.CR();
		MainActivity.BLUETOOTH_PRINTER.SetFontEnlarge((byte) 0x9);
		MainActivity.BLUETOOTH_PRINTER.BT_Write("\nKeep in touch..!!!");
        // Print Image Directly from Url
		printImageFromUrl();
		MainActivity.BLUETOOTH_PRINTER.LF();
		MainActivity.BLUETOOTH_PRINTER.LF();
		MainActivity.BLUETOOTH_PRINTER.LF();
		MainActivity.BLUETOOTH_PRINTER.LF();

		return true;
	}

	public static void printImageFromUrl(){
		//LF = Line feed
		MainActivity.BLUETOOTH_PRINTER.LF();

		MainActivity.BLUETOOTH_PRINTER.LF();
		MainActivity.BLUETOOTH_PRINTER.SetDefaultLineSpacing();
		MainActivity.BLUETOOTH_PRINTER.SetAlignMode((byte) 0);//Left
		MainActivity.BLUETOOTH_PRINTER.SetFontEnlarge((byte) 0x00);//normal font
		startprint();
	}

	public static void startprint() {

		try {
			System.gc();
		} catch (Exception e) {

		}
		if (mTarget != null) {
			Picasso.get().cancelRequest(mTarget);
		}
		try {
			if (dialog!=null) {
				dialog.dismiss();
				dialog = null;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		Picasso.get().cancelTag("loadImage");


		mTarget = new Target() {
			@Override
			public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
				if (bitmap == null) {
					Log.d(TAG, "Bitmap is Null");
					getBitmap();
				} else {
					Log.d(TAG, "Worked");
					Log.d(TAG, "in onBitmapLoaded : ");
					try {
						getBytes(bitmap);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			@Override
			public void onBitmapFailed(Exception e, Drawable errorDrawable) {
				Log.d(TAG, "failed");
				getBitmap();
			}

			@Override
			public void onPrepareLoad(Drawable placeHolderDrawable) {
				Log.d(TAG, "Prepare");
			}
		};
		String userImageUrl="https://i.picsum.photos/id/652/200/300.jpg?hmac=yJT5T1Ugojp0HlslsxDN_nNnIIk4lsFXcV_5rq9FCTw"; // Replace Your url
		RequestCreator imageLoadRequestCreator = Picasso.get().load(userImageUrl);

		imageLoadRequestCreator.fetch(new Callback() {
			@Override
			public void onSuccess() {

				Log.d(TAG, "onSuccess");

			}

			@Override
			public void onError(Exception e) {
				Log.d(TAG, "onError of url load " + e.getMessage());
				getBitmap();

			}
		});
		imageLoadRequestCreator.networkPolicy(NetworkPolicy.NO_CACHE).tag("loadImage").into(mTarget);

	}


	private static void getBitmap() {
		String userImageUrl="https://i.picsum.photos/id/652/200/300.jpg?hmac=yJT5T1Ugojp0HlslsxDN_nNnIIk4lsFXcV_5rq9FCTw"; //replace your url
		String tag = "imageRequestTag";
		AndroidNetworking.forceCancel(tag);
		AndroidNetworking.initialize(mContext);
		AndroidNetworking.enableLogging(HttpLoggingInterceptor.Level.BASIC);
		AndroidNetworking.get(userImageUrl)
				.setTag(tag)
				.setPriority(Priority.HIGH)
				.setImageScaleType(null)
				.doNotCacheResponse()
				.setBitmapConfig(Bitmap.Config.ARGB_8888)
				.build()
				.setAnalyticsListener(new AnalyticsListener() {
					@Override
					public void onReceived(long timeTakenInMillis, long bytesSent, long bytesReceived, boolean isFromCache) {
						Log.d(TAG, " timeTakenInMillis : " + timeTakenInMillis);
						Log.d(TAG, " bytesSent : " + bytesSent);
						Log.d(TAG, " bytesReceived : " + bytesReceived);
						Log.d(TAG, " isFromCache : " + isFromCache);
					}
				})
				.getAsBitmap(new BitmapRequestListener() {

					@Override
					public void onResponse(Bitmap bitmap) {
						Log.d(TAG, "in getAsBitmap : ");
						if (bitmap != null) {
							try {
								getBytes(bitmap);
							} catch (Exception e) {
								e.printStackTrace();
							}
							AndroidNetworking.evictAllBitmap(); // clear LruCache
						} else {
							if (dialog != null) {
								dialog.dismiss();
								dialog = null;
							}

							Toast.makeText(mContext,"Oops..! We are facing some problem with printing.Kindly do the same process in a while.",Toast.LENGTH_LONG).show();
						}

					}

					@Override
					public void onError(ANError error) {
						// handle error
						error.printStackTrace();
						if (dialog != null) {
							dialog.dismiss();
							dialog = null;
						}
						if (error.getErrorCode() != 0) {
							// received error from server
							// error.getErrorCode() - the error code from server
							// error.getErrorBody() - the error body from server
							// error.getErrorDetail() - just an error detail
							Log.d(TAG, "onError errorCode : " + error.getErrorCode());
							Log.d(TAG, "onError errorBody : " + error.getErrorBody());
							Log.d(TAG, "onError errorDetail : " + error.getErrorDetail());
							// get parsed error object (If ApiError is your class)
						} else {
							// error.getErrorDetail() : connectionError, parseError, requestCancelledError
							Log.d(TAG, "onError errorDetail : " + error.getErrorDetail());
							Log.d(TAG, "onError errorDetail : " + error.getMessage());
						}


						Toast.makeText(mContext,"Oops..! We are facing some problem with printing.Kindly do the same process in a while.",Toast.LENGTH_LONG).show();

					}
				});


	}

	private static void getBytes(Bitmap bitmap) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
//		Bitmap resized = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getWidth(), true);
		bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream);
		byte[] byteArray = stream.toByteArray();
		Bitmap compressedBitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.length);
		int x_offset = (dots_per_line[mPaperWidthType] - compressedBitmap.getWidth()) >> 1;//horizontal centre
		int y_offset = 0;
		try {
			cmdBitmapPrint(bitmap2Binary(compressedBitmap), BITMAP_ZOOM_NONE, x_offset, y_offset, 0);
			Toast.makeText(mContext, "ByteArray created..", Toast.LENGTH_SHORT).show();
		}catch (Exception e)
		{
			Log.d(TAG,"Exception is"+e.getMessage());
			e.printStackTrace();
		}
	}
	public static Bitmap bitmap2Binary(Bitmap src) {
		int w, h;
		h = src.getHeight();
		w = src.getWidth();
		int[] pixels = new int[w*h];
		src.getPixels(pixels, 0, w, 0, 0, w, h);
		int alpha = 0xff<<24;
		for(int y=0;y<h;y++) {
			for(int x=0;x<w;x++) {
				int gray = pixels[w*y+x];
				int red = ((gray&0x00ff0000) >> 16);
				int green = ((gray&0x0000ff00) >> 8);
				int blue = ((gray&0x000000ff) >> 8);
				gray = (red + green + blue) / 3;
				gray = alpha | (gray << 16) | (gray << 8) | gray;
				pixels[w*y+x] = gray;
			}
		}
		Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		result.setPixels(pixels, 0, w, 0, 0, w, h);
		return result;
	}
	public static void cmdBitmapPrint(Bitmap bitmap, int zoom, int left, int top, int delay)  {
		byte[] result = null;

		if (bitmap == null) {
			Log.d(TAG,"bitmap is null");
			return;
		}
		if (((bitmap.getWidth() + left) > dots_per_line[mPaperWidthType]) /*|| ((bitmap.getHeight() + top) > dots_per_line[mPaperWidthType]*/) {
			Log.d(TAG,"bitmap dosen't match");
			return;
		}
		//limits width
		int lines = bitmap.getHeight() + top;
		int w = bitmap.getWidth();
		int h = bitmap.getHeight();
		result = new byte[lines * bmp_byte_width[mPaperWidthType]];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int color=bitmap.getPixel(x, y);
				//int alpha=Color.alpha(color);
				int red= Color.red(color);
				int green=Color.green(color);
				int blue=Color.blue(color);
				if (red < 128) {
					int bitX =  x + left;
					int byteX = bitX >> 3;
					int byteY = y + top;
					result[byteY * bmp_byte_width[mPaperWidthType] + byteX] |= (0x80 >> (bitX - (byteX << 3)));
				}
			}
		}
		sendCmd(cmdGSv0pwLwHhLhHd(zoom, bmp_byte_width[mPaperWidthType], 0, lines&0xff, (lines>>8)&0xff, result), delay);
//		byte[] data=cmdGSv0pwLwHhLhHd(zoom, bmp_byte_width[mPaperWidthType], 0, lines&0xff, (lines>>8)&0xff, result);
//		MainActivity.BLUETOOTH_PRINTER.BT_Write(data, data.length);
	}

	public static void sendCmd(byte[] cmd, int delay){
		//PT486 serial print need delay
		boolean needDelay = true;
		int wait = ((delay > 0) ? (delay) : (50));

		if (cmd.length > PRINTER_BUFFER_LEN) {
			int i=0;
			int count = (int)(cmd.length / PRINTER_BUFFER_LEN);
			int end = cmd.length - (count * PRINTER_BUFFER_LEN);
			byte[] tmp = new byte[PRINTER_BUFFER_LEN];
			byte[] last = new byte[end];
			while(count-- > 0) {
				System.arraycopy(cmd, i, tmp, 0, PRINTER_BUFFER_LEN);
				MainActivity.BLUETOOTH_PRINTER.BT_Write(tmp);
				if (needDelay) {
					try {
						Thread.sleep(wait);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				i+=PRINTER_BUFFER_LEN;
			}
			System.arraycopy(cmd, i, last, 0, end);
			MainActivity.BLUETOOTH_PRINTER.BT_Write(last);
		} else {
			MainActivity.BLUETOOTH_PRINTER.BT_Write(cmd);
		}

		if (needDelay) {
			try {
				Thread.sleep(wait);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
