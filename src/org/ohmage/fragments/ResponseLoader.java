package org.ohmage.fragments;

import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbProvider.Qualified;
import org.ohmage.ui.OhmageFilterable.FilterableFragmentLoader;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;

/**
 * The {@link ResponseLoader} makes it easy for a {@link FilterableFragment} to get the CursorLoader it
 * needs to query for a set of responses
 * @author cketcham
 *
 */
public class ResponseLoader {

	private final FilterableFragmentLoader mFragment;
	private final String[] mProjection;
	private final String mSelection;

	public ResponseLoader(FilterableFragmentLoader fragment, String[] projection) {
		this(fragment, projection, null);
	}

	public ResponseLoader(FilterableFragmentLoader fragment, String[] projection, String selection) {
		mFragment = fragment;
		mProjection = projection;
		mSelection = selection;
	}

	public CursorLoader onCreateLoader(int arg0, Bundle arg1) {

		Uri uri = Responses.CONTENT_URI;

		StringBuilder selection = new StringBuilder();
		String[] selectionArgs = null;

		// Set the filter selection
		if(mFragment.getCampaignUrn() != null && mFragment.getSurveyId() != null) {
			uri = Campaigns.buildResponsesUri(mFragment.getCampaignUrn(), mFragment.getSurveyId());
		} else if(mFragment.getCampaignUrn() != null) {
			uri = Campaigns.buildResponsesUri(mFragment.getCampaignUrn());
		} else if(mFragment.getSurveyId() != null) {
			selection.append(Qualified.RESPONSES_SURVEY_ID +"=? AND ");
			selectionArgs = new String[] { mFragment.getSurveyId() };
		}

		// Set the date filter selection
		selection.append(Responses.RESPONSE_TIME + " >= " + mFragment.getStartBounds() + " AND ");
		selection.append(Responses.RESPONSE_TIME + " <= " + mFragment.getEndBounds());

		if(mSelection != null)
			selection.append(" AND " + mSelection);

		return new CursorLoader(mFragment.getActivity(), uri, mProjection, selection.toString(), selectionArgs, Responses.RESPONSE_TIME + " DESC");
	}
}