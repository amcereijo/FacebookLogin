package com.example.facebooklogin;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import com.example.facebooklogin.FacebookLoginAsyncTask.Operations;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

/**
 * 
 * @author angelcereijo
 * 
 */
public class FacebookLoginMainActivity extends Activity {
	
	private static final String TAG = FacebookLoginMainActivity.class.toString();
	
	
	private static final String FACEBOOK_APP_ID = "YOUR_FACEBOOK_ID";
	private static final String EMAIL_PERMISION = "email";
	
	private static final String FACEBOOK_ACCESS_TOKEN = "access_token";
	private static final String FACEBOOK_ACCESS_EXPIRES = "access_expires";
	

	private SharedPreferences privatePreferences;
	private Facebook facebook;
	
	

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
		if(facebook ==null || facebook.getAccessToken()==null){
			facebook = new Facebook(FACEBOOK_APP_ID);
		}
		facebook.authorize(this, new String[] { EMAIL_PERMISION },
				Facebook.FORCE_DIALOG_AUTH, new FacebookLoginListener(this));	
	}

	/**
	 * 
	 * @param v
	 */
	public void clickUnAuthorize(View v){
	    new FacebookLoginAsyncTask(this, facebook).execute(Operations.unAuthotize);	    
	}

	
	/**
	 * 
	 * @param v
	 */
	public void clickLogout(View v){
	    new FacebookLoginAsyncTask(this, facebook).execute(Operations.logout);	    
	}
	
	
	private void  initializeApp(){
		privatePreferences = getPreferences(MODE_PRIVATE);
		if (isSavedValidAccessFacebookToken()) {
			facebook = new Facebook(FACEBOOK_APP_ID);
			facebook.setAccessToken(privatePreferences.getString(FACEBOOK_ACCESS_TOKEN,null));
			new FacebookLoginAsyncTask(this,facebook).execute(Operations.getInfoLogged);
		}
	}


	private boolean isSavedValidAccessFacebookToken() {
		String access_token = privatePreferences.getString(FACEBOOK_ACCESS_TOKEN, null);
        long expires = privatePreferences.getLong(FACEBOOK_ACCESS_EXPIRES, 0);
        boolean savedValidToken = access_token != null && expires != 0;
		return savedValidToken;
	}



	private class FacebookLoginListener implements DialogListener {
		private Activity activity;
		
		public FacebookLoginListener(Activity activity) {
			this.activity = activity;
		}
		
		public void onFacebookError(FacebookError e) {
			((TextView) findViewById(R.id.login_result)).setText(R.string.login_error);
		}

		public void onError(DialogError e) {
			((TextView) findViewById(R.id.login_result)).setText(R.string.login_error);
		}

		public void onCancel() {
			((TextView) findViewById(R.id.login_result)).setText(R.string.login_cancel);
		}

		public void onComplete(Bundle values) {// 0-accestoken, 1-expires millisec, 2-code]
			
			new FacebookLoginAsyncTask(activity,facebook).execute(Operations.getInfoFirst);
			
		}
	}
	
	
	


}
