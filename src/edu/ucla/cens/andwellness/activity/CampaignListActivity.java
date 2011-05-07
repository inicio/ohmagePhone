package edu.ucla.cens.andwellness.activity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import jbcrypt.BCrypt;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import edu.ucla.cens.andwellness.AndWellnessApi;
import edu.ucla.cens.andwellness.AndWellnessApi.ReadResponse;
import edu.ucla.cens.andwellness.AndWellnessApi.Result;
import edu.ucla.cens.andwellness.campaign.Campaign;
import edu.ucla.cens.andwellness.db.DbHelper;
import edu.ucla.cens.andwellness.triggers.glue.LocationTriggerAPI;
import edu.ucla.cens.andwellness.triggers.glue.TriggerFramework;
import edu.ucla.cens.andwellness.AndWellnessApplication;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.SharedPreferencesHelper;
import edu.ucla.cens.andwellness.Survey;
import edu.ucla.cens.mobility.glue.MobilityInterface;

public class CampaignListActivity extends ListActivity {

	private static final String TAG = "CampaignListActivity";

	private static final int DIALOG_DOWNLOAD_PROGRESS = 0;
	private static final int DIALOG_INTERNAL_ERROR = 1;
	private static final int DIALOG_NETWORK_ERROR = 2;
	private static final int DIALOG_USER_DISABLED = 3;
	private static final int DIALOG_AUTH_ERROR = 4;
	
	private CampaignListUpdateTask mTask;
	//private List<HashMap<String, String>> mData;
	private List<Campaign> mAvailable;
	private List<Campaign> mUnavailable;
	private LayoutInflater mInflater;
	private View mFooter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//setContentView(R.layout.campaign_list);
		
		final SharedPreferencesHelper preferencesHelper = new SharedPreferencesHelper(this);
		
		if (preferencesHelper.isUserDisabled()) {
        	((AndWellnessApplication) getApplication()).resetAll();
        }
		
		if (!preferencesHelper.isAuthenticated()) {
			Log.i(TAG, "no credentials saved, so launch Login");
			startActivity(new Intent(this, LoginActivity.class));
			finish();
			
		} else {
			
			String [] from = new String [] {"name", "urn"};
	        int [] to = new int [] {android.R.id.text1, android.R.id.text2};
	        
	        //mData = new ArrayList<HashMap<String,String>>();
	        
	        mAvailable = new ArrayList<Campaign>();
	        mUnavailable = new ArrayList<Campaign>();
	        
	        loadCampaigns();
	        
	        mInflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
	        mFooter = mInflater.inflate(R.layout.campaign_list_footer, null);
	        mFooter.setVisibility(View.GONE);
	        getListView().addFooterView(mFooter);
			
			setListAdapter(new CampaignListAdapter(this, mAvailable, mUnavailable, R.layout.list_item_with_image, R.layout.list_header));
			//setListAdapter(new SimpleAdapter(this, mData , android.R.layout.simple_list_item_2, from, to));
			
			Object retained = getLastNonConfigurationInstance();
	        
	        if (retained instanceof CampaignListUpdateTask) {
	        	Log.i(TAG, "creating after configuration changed, restored CampaignListUpdateTask instance");
	        	mTask = (CampaignListUpdateTask) retained;
	        	mTask.setActivity(this);
	        } else {
	        	Log.i(TAG, "no tasks in progress");
	        	
	        	updateCampaignList();
	        }
		}
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		Log.i(TAG, "configuration change");
		if (mTask != null) {
			Log.i(TAG, "retaining AsyncTask instance");
			mTask.setActivity(null);
			return mTask;
		}
		return null;
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		if (((CampaignListAdapter)getListAdapter()).getItemGroup(position) == CampaignListAdapter.GROUP_AVAILABLE) {
			//launch surveylistactivity
			
			Intent intent = new Intent(this, SurveyListActivity.class);
			intent.putExtra("campaign_urn", ((Campaign) getListView().getItemAtPosition(position)).mUrn);
			intent.putExtra("campaign_name", ((Campaign) getListView().getItemAtPosition(position)).mName);
			startActivity(intent);
		} else {
			//download campaign
			CampaignDownloadTask task = new CampaignDownloadTask(CampaignListActivity.this);
			task.execute("xerox.gimp", "mama.quanta", ((Campaign) getListView().getItemAtPosition(position)).mUrn);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.campaign_list_menu, menu);
	  	return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.mobility_settings:
			MobilityInterface.showMobilityOptions(this);
			//Toast.makeText(this, "Mobility is not available.", Toast.LENGTH_SHORT).show();
			return true;
			
		case R.id.status:
			//WakefulIntentService.sendWakefulWork(this, UploadService.class);
			Intent intent = new Intent(this, StatusActivity.class);
			startActivityForResult(intent, 1);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == 1) {
			if (resultCode == 123) {
				finish();
			}
		}
	}

	private void loadCampaigns() {
		
		mAvailable.clear();
		
		DbHelper dbHelper = new DbHelper(this);
        List<Campaign> campaigns = dbHelper.getCampaigns();
        
        for (Campaign c : campaigns) {
        	mAvailable.add(c);
        }
	}
	
	private void updateCampaignList() {
		
		mTask = new CampaignListUpdateTask(CampaignListActivity.this);
		mTask.execute("xerox.gimp", "mama.quanta");
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = super.onCreateDialog(id);
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		switch (id) {
		case DIALOG_AUTH_ERROR:
        	dialogBuilder.setTitle("Error")
        				.setMessage("Unable to authenticate. Please check username and re-enter password.")
        				.setCancelable(true)
        				.setPositiveButton("OK", null)
        				/*.setNeutralButton("Help", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								startActivity(new Intent(LoginActivity.this, HelpActivity.class));
								//put extras for specific help on login error
							}
						})*/;
        	//add button for contact
        	dialog = dialogBuilder.create();        	
        	break;
        	
		case DIALOG_USER_DISABLED:
        	dialogBuilder.setTitle("Error")
        				.setMessage("This user account has been disabled.")
        				.setCancelable(true)
        				.setPositiveButton("OK", null)
        				/*.setNeutralButton("Help", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								startActivity(new Intent(LoginActivity.this, HelpActivity.class));
								//put extras for specific help on login error
							}
						})*/;
        	//add button for contact
        	dialog = dialogBuilder.create();        	
        	break;
        	
		case DIALOG_NETWORK_ERROR:
        	dialogBuilder.setTitle("Error")
        				.setMessage("Unable to communicate with server. Please try again later.")
        				.setCancelable(true)
        				.setPositiveButton("OK", null)
        				/*.setNeutralButton("Help", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								startActivity(new Intent(LoginActivity.this, HelpActivity.class));
								//put extras for specific help on http error
							}
						})*/;
        	//add button for contact
        	dialog = dialogBuilder.create();
        	break;
        
		case DIALOG_INTERNAL_ERROR:
        	dialogBuilder.setTitle("Error")
        				.setMessage("The server returned an unexpected response. Please try again later.")
        				.setCancelable(true)
        				.setPositiveButton("OK", null)
        				/*.setNeutralButton("Help", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								startActivity(new Intent(LoginActivity.this, HelpActivity.class));
								//put extras for specific help on http error
							}
						})*/;
        	//add button for contact
        	dialog = dialogBuilder.create();
        	break;
        	
		case DIALOG_DOWNLOAD_PROGRESS:
			ProgressDialog pDialog = new ProgressDialog(this);
			pDialog.setMessage("Downloading campaign configuration...");
			pDialog.setCancelable(false);
			//pDialog.setIndeterminate(true);
			dialog = pDialog;
        	break;
		}
		
		return dialog;
	}
	
	private void onCampaignListUpdated(ReadResponse response) {
		
		mTask = null;
		
		if (response.getResult() == Result.SUCCESS) {
			//mData.clear();
			mUnavailable.clear();
			
			mFooter.setVisibility(View.GONE);
			
			// parse response
			try {
				JSONArray jsonItems = response.getMetadata().getJSONArray("items");
				for(int i = 0; i < jsonItems.length(); i++) {
					Campaign c = new Campaign();
					c.mUrn = jsonItems.getString(i); 
					c.mName = response.getData().getJSONObject(c.mUrn).getString("name");
					c.mCreationTimestamp = response.getData().getJSONObject(c.mUrn).getString("creation_timestamp");
					
//					HashMap<String, String> map = new HashMap<String, String>();
//					map.put("name", name);
//					map.put("urn", urn);
//					mData.add(map);
					boolean isAlreadyAvailable = false;
					for (Campaign availableCampaign : mAvailable) {
						if (c.mUrn.equals(availableCampaign.mUrn)) {
							isAlreadyAvailable = true;
						}
					}
					
					if (!isAlreadyAvailable) {
						mUnavailable.add(c);
					}
				}
			} catch (JSONException e) {
				Log.e(TAG, "Error parsing response json", e);
			}
			// update listview
//			((SimpleAdapter) getListAdapter()).notifyDataSetChanged();
			((CampaignListAdapter) getListAdapter()).notifyDataSetChanged();
		} else {
			//show error
			mFooter.setVisibility(View.VISIBLE);
			mFooter.findViewById(R.id.progress_bar).setVisibility(View.GONE);
			mFooter.findViewById(R.id.error_text).setVisibility(View.VISIBLE);
		}
	}
	
	private void onCampaignDownloaded(String campaignUrn, ReadResponse response) {
		
		mTask = null;
		
		//close progress dialog
		dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
		
		if (response.getResult() == Result.SUCCESS) {
			
			// parse response
			try {
				JSONObject campaignJson = response.getData().getJSONObject(campaignUrn);
				String name = campaignJson.getString("name");
				String creationTimestamp = campaignJson.getString("creation_timestamp");
				String xml = campaignJson.getString("xml");
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String downloadTimestamp = dateFormat.format(new Date());
				//String downloadTimestamp = DateFormat.format("yyyy-MM-dd kk:mm:ss", System.currentTimeMillis());
				
				DbHelper dbHelper = new DbHelper(this);
				dbHelper.addCampaign(campaignUrn, name, creationTimestamp, downloadTimestamp, xml);
				
			} catch (JSONException e) {
				Log.e(TAG, "Error parsing response json", e);
			}
			// update listview
			loadCampaigns();
			updateCampaignList();
			((CampaignListAdapter) getListAdapter()).notifyDataSetChanged();
		} else if (response.getResult() == Result.FAILURE) {
			Log.e(TAG, "auth failure");
			
			for (String s : response.getErrorCodes()) {
				Log.e(TAG, "error code: " + s);
			}
			
			if (Arrays.asList(response.getErrorCodes()).contains("0201")) {
				new SharedPreferencesHelper(this).setUserDisabled(true);
				showDialog(DIALOG_USER_DISABLED);
			} else {
				showDialog(DIALOG_AUTH_ERROR);
			}
			
		} else if (response.getResult() == Result.HTTP_ERROR) {
			Log.e(TAG, "http error");
			
			showDialog(DIALOG_NETWORK_ERROR);
		} else {
			Log.e(TAG, "internal error");
			
			showDialog(DIALOG_INTERNAL_ERROR);
		}
	}

	private static class CampaignListUpdateTask extends AsyncTask<String, Void, ReadResponse>{
		
		private CampaignListActivity mActivity;
		private boolean mIsDone = false;
		private ReadResponse mResponse = null;
		
		private CampaignListUpdateTask(CampaignListActivity activity) {
			this.mActivity = activity;
		}
		
		public void setActivity(CampaignListActivity activity) {
			this.mActivity = activity;
			if (mIsDone) {
				notifyTaskDone();
			}
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			mActivity.mFooter.setVisibility(View.VISIBLE);
			mActivity.mFooter.findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
			mActivity.mFooter.findViewById(R.id.error_text).setVisibility(View.GONE);
		}

		@Override
		protected ReadResponse doInBackground(String... params) {
			String username = params[0];
			String password = params[1];
			String hashedPassword = new SharedPreferencesHelper(mActivity).getHashedPassword();
			AndWellnessApi api = new AndWellnessApi(mActivity);
			return api.campaignRead("https://dev1.andwellness.org/", username, hashedPassword, "android", "short", null);
		}
		
		@Override
		protected void onPostExecute(ReadResponse response) {
			super.onPostExecute(response);
			
			mResponse = response;
			mIsDone = true;
			notifyTaskDone();			
		}
		
		private void notifyTaskDone() {
			if (mActivity != null) {
				mActivity.onCampaignListUpdated(mResponse);
			}
		}
	}
	
	private static class CampaignDownloadTask extends AsyncTask<String, Void, ReadResponse>{
		
		private CampaignListActivity mActivity;
		private boolean mIsDone = false;
		private ReadResponse mResponse = null;
		private String mCampaignUrn;
		
		private CampaignDownloadTask(CampaignListActivity activity) {
			this.mActivity = activity;
		}
		
		public void setActivity(CampaignListActivity activity) {
			this.mActivity = activity;
			if (mIsDone) {
				notifyTaskDone();
			}
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			//show progress dialog
			mActivity.showDialog(DIALOG_DOWNLOAD_PROGRESS);
		}

		@Override
		protected ReadResponse doInBackground(String... params) {
			String username = params[0];
			String password = params[1];
			mCampaignUrn = params[2];
			String hashedPassword = new SharedPreferencesHelper(mActivity).getHashedPassword();
			AndWellnessApi api = new AndWellnessApi(mActivity);
			return api.campaignRead("https://dev1.andwellness.org/", username, hashedPassword, "android", "long", mCampaignUrn);
		}
		
		@Override
		protected void onPostExecute(ReadResponse response) {
			super.onPostExecute(response);
			
			mResponse = response;
			mIsDone = true;
			notifyTaskDone();			
		}
		
		private void notifyTaskDone() {
			if (mActivity != null) {
				mActivity.onCampaignDownloaded(mCampaignUrn, mResponse);
			}
		}
	}
}