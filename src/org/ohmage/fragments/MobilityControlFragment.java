

package org.ohmage.fragments;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import edu.ucla.cens.mobility.glue.IMobility;
import edu.ucla.cens.mobility.glue.MobilityInterface;
import edu.ucla.cens.systemlog.Analytics;
import edu.ucla.cens.systemlog.Log;

import org.ohmage.MobilityHelper;
import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.service.UploadService;
import org.ohmage.ui.BaseActivity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class MobilityControlFragment extends Fragment implements LoaderCallbacks<Cursor> {

	private static final String TAG = "MobilityFeedbackActivity";

	private static final int RECENT_LOADER = 0;
	private static final int ALL_LOADER = 1;
	private static final int UPLOAD_LOADER = 2;

	private ListView mMobilityList;
	private TextView mTotalCountText;
	private TextView mUploadCountText;
	private TextView mLastUploadText;
	private Button mUploadButton;
	private RadioButton mOffRadio;
	private RadioButton mInterval1Radio;
	private RadioButton mInterval5Radio;

	private SimpleCursorAdapter mAdapter;

	private SharedPreferencesHelper mPrefHelper;
	private IMobility mMobility = null;
	private boolean isBound = false;

	private final String emptyValue = "-";

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mPrefHelper = new SharedPreferencesHelper(getActivity());

		long lastMobilityUploadTimestamp = mPrefHelper.getLastMobilityUploadTimestamp();
		if (lastMobilityUploadTimestamp == 0) {
			mLastUploadText.setText(emptyValue);
		} else {
			mLastUploadText.setText(DateFormat.format("yyyy-MM-dd kk:mm:ss", lastMobilityUploadTimestamp));
		}

		getLoaderManager().initLoader(RECENT_LOADER, null, this);
		getLoaderManager().initLoader(ALL_LOADER, null, this);
		getLoaderManager().initLoader(UPLOAD_LOADER, null, this);

		getActivity().bindService(new Intent(IMobility.class.getName()), mConnection, Context.BIND_AUTO_CREATE);
		isBound = true;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.mobility_control_layout, container, false);

		mMobilityList = (ListView) view.findViewById(R.id.mobility_list);
		mTotalCountText = (TextView) view.findViewById(R.id.mobility_total);
		mUploadCountText = (TextView) view.findViewById(R.id.mobility_count);
		mLastUploadText = (TextView) view.findViewById(R.id.last_upload);
		mUploadButton = (Button) view.findViewById(R.id.upload_button);
		mOffRadio = (RadioButton) view.findViewById(R.id.radio_off);
		mInterval1Radio = (RadioButton) view.findViewById(R.id.radio_1min);
		mInterval5Radio = (RadioButton) view.findViewById(R.id.radio_5min);

		TextView emptyView = (TextView) view.findViewById(R.id.empty);
		mMobilityList.setEmptyView(emptyView);

		String [] from = new String[] {MobilityInterface.KEY_MODE, MobilityInterface.KEY_TIME};
		int [] to = new int[] {R.id.text1, R.id.text2};

		mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.mobility_list_item, null, from, to, 0);

		mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {

			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				switch (view.getId()) {
					case R.id.text1:
						((TextView)view).setText(cursor.getString(columnIndex));
						return true;

					case R.id.text2:
						long time = cursor.getLong(columnIndex);
						((TextView)view).setText(DateFormat.format("h:mmaa", time));
						return true;
				}
				return false;
			}
		});

		mMobilityList.setAdapter(mAdapter);

		mTotalCountText.setText(emptyValue);
		mUploadCountText.setText(emptyValue);

		mOffRadio.setChecked(true);

		mUploadButton.setOnClickListener(mUploadListener);

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().registerReceiver(mMobilityUploadReceiver, new IntentFilter(UploadService.MOBILITY_UPLOAD_STARTED));
		getActivity().registerReceiver(mMobilityUploadReceiver, new IntentFilter(UploadService.MOBILITY_UPLOAD_FINISHED));
	}

	@Override
	public void onStop() {
		super.onStop();
		getActivity().unregisterReceiver(mMobilityUploadReceiver);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (isBound) {
			getActivity().unbindService(mConnection);
		}
	}

	private final BroadcastReceiver mMobilityUploadReceiver = new BroadcastReceiver() {

		private Long mLastMobilityUploadTimestamp;

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if(getActivity() instanceof BaseActivity)
				((BaseActivity) getActivity()).getActionBar().setProgressVisible(UploadService.MOBILITY_UPLOAD_STARTED.equals(action));

			mUploadButton.setEnabled(!UploadService.MOBILITY_UPLOAD_STARTED.equals(action));

			if (UploadService.MOBILITY_UPLOAD_STARTED.equals(action)) {
				mUploadButton.setText("Uploading...");
				mLastMobilityUploadTimestamp = mPrefHelper.getLastMobilityUploadTimestamp();
			} else if (UploadService.MOBILITY_UPLOAD_FINISHED.equals(action)) {
				mUploadButton.setText("Upload Now");
				Long newUploadTimestamp = mPrefHelper.getLastMobilityUploadTimestamp();

				// There must have been a problem with the upload since the upload timestamp didn't change
				if(newUploadTimestamp.equals(mLastMobilityUploadTimestamp)) {
					Toast.makeText(getActivity(), R.string.mobility_upload_error_message, Toast.LENGTH_SHORT).show();
				}
				mLastMobilityUploadTimestamp = newUploadTimestamp;

				if(mLastMobilityUploadTimestamp != 0) {
					mLastUploadText.setText(DateFormat.format("yyyy-MM-dd kk:mm:ss", mLastMobilityUploadTimestamp));
				}
				getLoaderManager().restartLoader(UPLOAD_LOADER, null, MobilityControlFragment.this);
			}
		}
	};

	private final OnClickListener mUploadListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Analytics.widget(v);
			Intent intent = new Intent(getActivity(), UploadService.class);
			intent.setData(Responses.CONTENT_URI);
			intent.putExtra(UploadService.EXTRA_UPLOAD_MOBILITY, true);
			WakefulIntentService.sendWakefulWork(getActivity(), intent);
		}
	};

	private final ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {

			mMobility = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {

			mMobility = IMobility.Stub.asInterface(service);

			int interval = -1;
			boolean isMobilityOn = false;

			try {
				isMobilityOn = mMobility.isMobilityOn();
				interval = mMobility.getMobilityInterval();
			} catch (RemoteException e) {
				Log.e(TAG, "Unable to read mobility state", e);
			}

			if (! isMobilityOn) {
				mOffRadio.setChecked(true);
			} else if (interval == 60) {
				mInterval1Radio.setChecked(true);
			} else if (interval == 300) {
				mInterval5Radio.setChecked(true);
			}

			mOffRadio.setOnClickListener(mRadioListener);			
			mInterval1Radio.setOnClickListener(mRadioListener);
			mInterval5Radio.setOnClickListener(mRadioListener);
		}

		OnClickListener mRadioListener = new OnClickListener() {

			@Override
			public void onClick(final View v) {
				Analytics.widget(v);

				// do the update in an asynctask so the UI doesn't block
				(new AsyncTask<Void, Void, Void>() {
					@Override
					protected void onPreExecute() {
						mOffRadio.setEnabled(false);
						mInterval1Radio.setEnabled(false);
						mInterval5Radio.setEnabled(false);
					};

					@Override
					protected Void doInBackground(Void... params) {
						int newInterval = -1;
						int oldInterval = -1;

						switch (v.getId()) {
							case R.id.radio_off:
								try {
									mMobility.stopMobility();
								} catch (RemoteException e) {
									Log.e(TAG, "Unable to stop mobility", e);
								}
								return null;

							case R.id.radio_1min:
								newInterval = 60;
								break;

							case R.id.radio_5min:
								newInterval = 300;
								break;
						}

						try {
							oldInterval = mMobility.getMobilityInterval();
							if (newInterval != oldInterval) {
								mMobility.changeMobilityRate(newInterval);
							}

							if (! mMobility.isMobilityOn()) {
								mMobility.startMobility();
							}
						} catch (RemoteException e) {
							Log.e(TAG, "Unable to change mobility interval", e);
						}

						return null;
					};

					@Override
					protected void onPostExecute(Void result) {
						mOffRadio.setEnabled(true);
						mInterval1Radio.setEnabled(true);
						mInterval5Radio.setEnabled(true);
					};
				}).execute();
			}
		};
	};

	public interface MobilityQuery {
		String[] PROJECTION = { 
				MobilityInterface.KEY_ROWID, 
				MobilityInterface.KEY_MODE, 
				MobilityInterface.KEY_TIME
		};
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		long loginTimestamp = mPrefHelper.getLoginTimestamp();
		String username = mPrefHelper.getUsername();

		// Filters the user by username and login time (in case they just upgraded mobility)
		final String filterUser = " (" + MobilityInterface.KEY_USERNAME + "=? OR " + MobilityInterface.KEY_TIME + " > " + loginTimestamp + ") ";
		final String[] filterUserParams = new String[] { MobilityHelper.getMobilityUsername(username) };

		switch (id) {
			case RECENT_LOADER:
				return new CursorLoader(getActivity(), MobilityInterface.CONTENT_URI,
						MobilityQuery.PROJECTION, MobilityInterface.KEY_TIME
								+ " > strftime('%s','now','-20 minutes') AND " + filterUser,
						filterUserParams, MobilityInterface.KEY_TIME + " DESC");

			case ALL_LOADER:
				return new CursorLoader(getActivity(), MobilityInterface.CONTENT_URI,
						MobilityQuery.PROJECTION, filterUser, filterUserParams, null);

			case UPLOAD_LOADER:
				long uploadAfterTimestamp = mPrefHelper.getLastMobilityUploadTimestamp();
				if (uploadAfterTimestamp == 0) {
					uploadAfterTimestamp = mPrefHelper.getLoginTimestamp();
				}
				return new CursorLoader(getActivity(), MobilityInterface.CONTENT_URI,
						MobilityQuery.PROJECTION, MobilityInterface.KEY_TIME + " > " + uploadAfterTimestamp + " AND "
								+ filterUser, filterUserParams, null);

			default:
				return null;
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

		switch (loader.getId()) {
			case RECENT_LOADER:
				mAdapter.swapCursor(data);
				break;

			case ALL_LOADER:
				if(data != null)
					mTotalCountText.setText(String.valueOf(data.getCount()));
				break;

			case UPLOAD_LOADER:
				if(data != null)
					mUploadCountText.setText(String.valueOf(data.getCount()));
				break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {

		switch (loader.getId()) {
			case RECENT_LOADER:
				mAdapter.swapCursor(null);
				break;

			case ALL_LOADER:
				mTotalCountText.setText(emptyValue);
				break;

			case UPLOAD_LOADER:
				mUploadCountText.setText(emptyValue);
				break;
		}
	}
}