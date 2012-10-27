package com.example.facebooklogin;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.android.Facebook;
import com.facebook.android.Facebook.ServiceListener;
import com.facebook.android.FacebookError;
import com.facebook.android.Util;

/**
 * 
 * @author angelcereijo
 *
 */
public class FacebookLoginAsyncTask extends AsyncTask<FacebookLoginAsyncTask.Operations, Integer, Void> {
	
	private static final String TAG = FacebookLoginAsyncTask.class.toString();
	
	public enum Operations{
		getInfoFirst,
		getInfoLogged,
		unAuthotize
	}
	
	private static final String FACEBOOK_ACCESS_TOKEN = "access_token";
	private static final String FACEBOOK_ACCESS_EXPIRES = "access_expires";
	private static final String REQUEST_PERSONAL_INFO = "me";
	private static final String PICTURE_LARGE_OP = "/picture?type=large";
	private static final String URL_GRAPH_FACEBOOK = "https://graph.facebook.com/";
	private static final String DELTE_METHOD = "DELETE";
	private static final String REQUEST_PERSONAL_PERMISSIONS = "/me/permissions";
	
	private boolean success = false;
	private boolean error = false;
	private Operations operation;
	private Facebook facebook;
	private JSONObject personalInfo;
	private Drawable faccebookImage;
	private Activity activity;
	private ProgressDialog progressDialog;
	
	
	public FacebookLoginAsyncTask(Activity activity,Facebook facebook) {
		this.activity = activity;
		this.facebook = facebook;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		progressDialog = ProgressDialog.show(activity, activity.getText(R.string.progress_dialog_title), "");
	}
	
	
	@Override
	protected Void doInBackground(Operations... params) {
		operation = params[0];
		updateMessage();
		switch (operation) {
			case getInfoFirst:
				getInfoFirst();
				break;
			case getInfoLogged:
				getInfoLogged();
				break;
			case unAuthotize:
				unAuthorize();
				break;
		}
		return null;
	}
	

	@Override
	protected void onPostExecute(Void result) {
		progressDialog.cancel();
		if(error){
			if(operation ==  Operations.unAuthotize){
				Toast.makeText(activity, activity.getText(R.string.unauthorize_text_error), Toast.LENGTH_LONG).show();
			}else{
				((TextView) activity.findViewById(R.id.login_result)).setText(R.string.login_cancel);
			}
		}
		if(success){
			if(operation == Operations.unAuthotize){
				removeAccesAndExpiresToken();
			    resetViewElements();
				Toast.makeText(activity, activity.getText(R.string.unauthorize_text), Toast.LENGTH_LONG).show();
			}else{
				showLoggedText();	
			}
		}
	}
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		progressDialog.setMessage(activity.getText(values[0]));
	}
	
	private void updateMessage() {
		switch (operation) {
			case getInfoFirst:
			case getInfoLogged:
				publishProgress(R.string.progress_dialog_in);
				break;
			case unAuthotize:
				publishProgress(R.string.progress_dialog_out);
				break;
		}
	}
	
	
	private void removeAccesAndExpiresToken(){
		SharedPreferences.Editor editor = activity.getPreferences(Activity.MODE_PRIVATE).edit();
		editor.remove(FACEBOOK_ACCESS_TOKEN);
		editor.remove(FACEBOOK_ACCESS_EXPIRES);
		editor.commit();
	}
	
	private void resetViewElements() {
		((TextView) activity.findViewById(R.id.text_unauthorize)).setVisibility(TextView.GONE);
		((TextView) activity.findViewById(R.id.text_login)).setVisibility(TextView.VISIBLE);
		((TextView) activity.findViewById(R.id.login_result)).setText("");
		((ImageView) activity.findViewById(R.id.facebook_image)).setVisibility(ImageView.GONE);
	}
	
	
	private void unAuthorize(){
		final Bundle params = new Bundle();
		/*
		 * this will revoke 'publish_stream' permission Note: If you don't
		 * specify a permission then this will de-authorize the application
		 * completely.
		 */
		// params.putString("permission", "publish_stream");
		try {
			facebook.request(REQUEST_PERSONAL_PERMISSIONS, params, DELTE_METHOD);
			
			facebook.logout(activity);
			success = true;
		} catch (FileNotFoundException e) {
			Log.e(TAG,"clickUnAuthorize",e);
			error = true;
		} catch (MalformedURLException e) {
			Log.e(TAG,"clickUnAuthorize",e);
			error = true;
		} catch (IOException e) {
			Log.e(TAG,"clickUnAuthorize",e);
			error = true;
		}
	}
	
	
	
	private void getInfoLogged(){
		try {
			
			if(facebook.isSessionValid()){
				getFacebookInformation();
				success = true;
				facebook.extendAccessTokenIfNeeded(activity, new FacebookLoginServiceListener());
			}else{
				//TODO
			}
			saveAccesAndExpiresToken();
		} catch (MalformedURLException e) {
			Log.e(TAG,"",e);
			error = true;
		} catch (IOException e) {
			Log.e(TAG,"",e);
			error = true;
		} catch (JSONException e) {
			Log.e(TAG,"",e);
			error = true;
		} catch (FacebookError e) {
			Log.e(TAG,"",e);
			error = true;
		} 
	}
	
	
	final private void showLoggedText() {
		((TextView) activity.findViewById(R.id.text_login)).setVisibility(TextView.GONE);
		String name = personalInfo.optString("name");
		String text = String.format(activity.getText(R.string.login_logged).toString(), name);
		((TextView) activity.findViewById(R.id.login_result)).setText(text);
		((TextView) activity.findViewById(R.id.text_unauthorize)).setVisibility(TextView.VISIBLE);
		
		ImageView facebookImage = (ImageView) activity.findViewById(R.id.facebook_image);
		facebookImage.setImageDrawable(faccebookImage);
		facebookImage.setVisibility(ImageView.VISIBLE);
	}
	
	private void getInfoFirst(){// 0-accestoken, 1-expires millisec, 2-code]
		try {
			getFacebookInformation();
			saveAccesAndExpiresToken();
			success = true;
		} catch (MalformedURLException e) {
			Log.e(TAG,"",e);
			error = true;
		} catch (IOException e) {
			Log.e(TAG,"",e);
			error = true;
		} catch (FacebookError e) {
			Log.e(TAG,"",e);
			error = true;
		} catch (JSONException e) {
			Log.e(TAG,"",e);
			error = true;
		}
	}
	
	
	private void saveAccesAndExpiresToken() {
		SharedPreferences.Editor editor =  activity.getPreferences(Activity.MODE_PRIVATE).edit();
		editor.putString(FACEBOOK_ACCESS_TOKEN, facebook.getAccessToken());
		editor.putLong(FACEBOOK_ACCESS_EXPIRES, facebook.getAccessExpires());
		editor.commit();
	}
	
	
	final private void getFacebookPersonalInfo() throws MalformedURLException,
			IOException, JSONException {
		String response = facebook.request(REQUEST_PERSONAL_INFO);
		personalInfo = Util.parseJson(response);
	}
	
	final private void getFacebookInformation() throws MalformedURLException,
			IOException, JSONException {
		getFacebookPersonalInfo();
		getPersonalPicture();	
	}
	
	final private void getPersonalPicture() {
		String id = personalInfo.optString("id");
		faccebookImage = getPictureForFacebookId(id);
	}
	
	final private Drawable getPictureForFacebookId(String facebookId) {
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
