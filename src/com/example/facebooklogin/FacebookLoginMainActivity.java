package com.example.facebooklogin;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.Facebook.ServiceListener;
import com.facebook.android.FacebookError;
import com.facebook.android.Util;

/**
 * 
 * @author angelcereijo
 * 
 */
public class FacebookLoginMainActivity extends Activity {
	
	private static final String TAG = FacebookLoginMainActivity.class.toString();
	
	private static final String PICTURE_LARGE_OP = "/picture?type=large";
	private static final String URL_GRAPH_FACEBOOK = "https://graph.facebook.com/";
	private static final String FACEBOOK_APP_ID = "YOUR_FACEBOOK_APP_ID";
	private static final String EMAIL_PERMISION = "email";
	private static final String REQUEST_PERSONAL_INFO = "me";
	private static final String FACEBOOK_ACCESS_TOKEN = "access_token";
	private static final String FACEBOOK_ACCESS_EXPIRES = "access_expires";
	private static final String DELTE_METHOD = "DELETE";
	private static final String REQUEST_PERSONAL_PERMISSIONS = "/me/permissions";

	private SharedPreferences privatePreferences;
	private Facebook facebook;
	private JSONObject personalInfo;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_facebook_login_main);
		initializeApp();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_facebook_login_main, menu);
		return true;
	}
	
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		facebook.authorizeCallback(requestCode, resultCode, data);
	}
	
	
	/**
	 * 
	 * @param v
	 */
	public void loginInFaceBook(View v) {
		facebook.authorize(this, new String[] { EMAIL_PERMISION },
				Facebook.FORCE_DIALOG_AUTH, new FacebookLoginListener());
	}

	
	public void clickUnAuthorize(View v){
	    Bundle params = new Bundle();
		/*
		 * this will revoke 'publish_stream' permission Note: If you don't
		 * specify a permission then this will de-authorize the application
		 * completely.
		 */
		// params.putString("permission", "publish_stream");
		try {
			facebook.request(REQUEST_PERSONAL_PERMISSIONS, params, DELTE_METHOD);
			resetViewElements();
			removeAccesAndExpiresToken();
			facebook.logout(this);
			Toast.makeText(this, getText(R.string.unauthorize_text), Toast.LENGTH_LONG).show();
		} catch (FileNotFoundException e) {
			Log.e(TAG,"clickUnAuthorize",e);
		} catch (MalformedURLException e) {
			Log.e(TAG,"clickUnAuthorize",e);
		} catch (IOException e) {
			Log.e(TAG,"clickUnAuthorize",e);
		}
	}

	private void resetViewElements() {
		((TextView) findViewById(R.id.text_unauthorize)).setVisibility(TextView.GONE);
		((TextView) findViewById(R.id.text_login)).setVisibility(TextView.VISIBLE);
		((TextView) findViewById(R.id.login_result)).setText("");
		((ImageView) findViewById(R.id.facebook_image)).setVisibility(ImageView.GONE);
	}
	
	private void  initializeApp(){
		facebook = new Facebook(FACEBOOK_APP_ID);
		privatePreferences = getPreferences(MODE_PRIVATE);
		if (isSavedValidAccessFacebookToken()) {
			try {
				facebook.setAccessToken(privatePreferences.getString(FACEBOOK_ACCESS_TOKEN,null));
				if(facebook.isSessionValid()){
					getFacebookInformation();
				}
				facebook.extendAccessTokenIfNeeded(this, new FacebookLoginServiceListener());
			} catch (MalformedURLException e) {
				Log.e(TAG,"",e);
			} catch (IOException e) {
				Log.e(TAG,"",e);
			} catch (JSONException e) {
				Log.e(TAG,"",e);
			} catch (FacebookError e) {
				Log.e(TAG,"",e);
			} 
		}
	}
	

	private Drawable getPictureForFacebookId(String facebookId) {
		Drawable picture = null;
		InputStream inputStream = null;
		try {
			inputStream = new URL(URL_GRAPH_FACEBOOK + facebookId
					+ PICTURE_LARGE_OP).openStream();
		} catch (Exception e) {
			Log.e(TAG,"",e);
			return null;

		}
		picture = Drawable.createFromStream(inputStream, "facebook-pictures");
		return picture;
	}


	private boolean isSavedValidAccessFacebookToken() {
		String access_token = privatePreferences.getString(FACEBOOK_ACCESS_TOKEN, null);
        long expires = privatePreferences.getLong(FACEBOOK_ACCESS_EXPIRES, 0);
        boolean savedValidToken = access_token != null && expires != 0;
		return savedValidToken;
	}

	
	private void removeAccesAndExpiresToken(){
		SharedPreferences.Editor editor = privatePreferences.edit();
		editor.remove(FACEBOOK_ACCESS_TOKEN);
		editor.remove(FACEBOOK_ACCESS_EXPIRES);
		editor.commit();
	}
	
	private void saveAccesAndExpiresToken() {
		SharedPreferences.Editor editor = privatePreferences.edit();
		editor.putString(FACEBOOK_ACCESS_TOKEN, facebook.getAccessToken());
		editor.putLong(FACEBOOK_ACCESS_EXPIRES, facebook.getAccessExpires());
		editor.commit();
	}

	private void getFacebookInformation() throws MalformedURLException,
			IOException, JSONException {
		getFacebookPersonalInfo();
		showLoggedText();
		showPersonalPicture();
	}

	private void showPersonalPicture() {
		String id = personalInfo.optString("id");
		Drawable faccebookImage = getPictureForFacebookId(id);

		ImageView facebookImage = (ImageView) findViewById(R.id.facebook_image);
		facebookImage.setImageDrawable(faccebookImage);
		facebookImage.setVisibility(ImageView.VISIBLE);
	}

	private void showLoggedText() {
		((TextView) findViewById(R.id.text_login)).setVisibility(TextView.GONE);
		String name = personalInfo.optString("name");
		((TextView) findViewById(R.id.login_result)).setText("Logged as "
				+ name);
		((TextView) findViewById(R.id.text_unauthorize)).setVisibility(TextView.VISIBLE);
	}

	private void getFacebookPersonalInfo() throws MalformedURLException,
			IOException, JSONException {
		String response = facebook.request(REQUEST_PERSONAL_INFO);
		personalInfo = Util.parseJson(response);
	}

	private class FacebookLoginListener implements DialogListener {

		public void onFacebookError(FacebookError e) {
			((TextView) findViewById(R.id.login_result)).setText("Loggin Error");
		}

		public void onError(DialogError e) {
			((TextView) findViewById(R.id.login_result)).setText("Loggin Error");
		}

		public void onCancel() {
			((TextView) findViewById(R.id.login_result))
					.setText("Loggin canceled");
		}

		public void onComplete(Bundle values) {// 0-accestoken, 1-expires millisec, 2-code]
			try {
				getFacebookInformation();
				saveAccesAndExpiresToken();
			} catch (MalformedURLException e) {
				Log.e(TAG,"",e);
			} catch (IOException e) {
				Log.e(TAG,"",e);
			} catch (FacebookError e) {
				Log.e(TAG,"",e);
			} catch (JSONException e) {
				Log.e(TAG,"",e);
			}
		}

	}
	
	
	private class FacebookLoginServiceListener implements ServiceListener{

		public void onComplete(Bundle values) {
			Log.i(TAG, "Facebook acces token extended");
		}

		public void onFacebookError(FacebookError e) {
			Log.e(TAG,"ServiceListener - ",e);
		}

		public void onError(Error e) {
			Log.e(TAG,"ServiceListener - ",e);
		}
		 
	}

}
