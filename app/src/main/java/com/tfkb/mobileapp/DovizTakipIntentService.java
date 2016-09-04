package com.tfkb.mobileapp;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class DovizTakipIntentService extends IntentService {
	
	private static final String TAG = "DovizTakipIntentService";
	private static final String PARA_BIRIMI_USD = "USD";
	private static final Double USD_EURO_UYARI_ORAN = 1.2;
	private NotificationManager notificationManager;

	public DovizTakipIntentService() {
		super("DovizTakipIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
		String dovizTakipUrl = getResources().getString(R.string.doviz_takip_url);
		
		if(isDovizOranSinirlarinDisinda(dovizTakipUrl)) {
			
			notificationGoster();
			
		}
		
	}
	
	private boolean isDovizOranSinirlarinDisinda(String dovizTakipUrl) {

		HttpURLConnection urlConnection = null;

		try {

			URL url = new URL(dovizTakipUrl);
			urlConnection = (HttpURLConnection) url.openConnection();

			int sonucKodu = urlConnection.getResponseCode();
			if (sonucKodu == HttpURLConnection.HTTP_OK) {
				BufferedInputStream stream = new BufferedInputStream(urlConnection.getInputStream());
				return isDovizOranSinirlarinDisindaInputStream(stream);
			}

		} catch (Exception e) {
			Log.d(TAG, "HTTP baglantisi kurulurken hata olustu", e);
		} finally {
			if (urlConnection != null)
				urlConnection.disconnect();
		}

		return false;

	}
	
	private boolean isDovizOranSinirlarinDisindaInputStream(BufferedInputStream stream) {

		try {

			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document document = docBuilder.parse(stream);
			Element firstCube = (Element) document.getElementsByTagName("Tarih_Date").item(0);
			//Element secondCube = (Element) firstCube.getElementsByTagName("Currency").item(0);

			NodeList dovizOranNodeList = firstCube.getElementsByTagName("Currency");
			int dovizOranNodeListLength = dovizOranNodeList.getLength();
			
			for (int i = 0; i < dovizOranNodeListLength; i++) {
				
				Element dovizOranElement = (Element) dovizOranNodeList.item(i);
				String paraBirimi = dovizOranElement.getAttribute("CurrencyCode");
				String euroyaOrani = dovizOranElement.getAttribute("ForexBuying".toString());
				
				if(PARA_BIRIMI_USD.equals(paraBirimi) && Double.parseDouble(euroyaOrani) > USD_EURO_UYARI_ORAN)
					return true;
			}

		} catch (Exception e) {
			Log.d(TAG, "XML parse edilirken hata olustu", e);
		}

		return false;
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void notificationGoster() {
		
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		Intent intent = new Intent(this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
							| Intent.FLAG_ACTIVITY_CLEAR_TASK);

		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

		Notification.Builder builder = new Notification.Builder(this)
				.setSmallIcon(R.drawable.ic_notification)
				.setContentTitle("Döviz Takip Uyarı")
				.setContentText("Dolar Euro oranı " + USD_EURO_UYARI_ORAN + "\'ı aştı")
				.setContentIntent(pendingIntent);
		
		notificationManager.notify(0, builder.build());
		
	}

}
