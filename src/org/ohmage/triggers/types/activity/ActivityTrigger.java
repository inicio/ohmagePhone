/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

/*
 * Authored by: Ankit Gupta. for questions, please email: agupta423@gmail.com
 * Adapted from Location Triggers and Time Triggers by: Kannan Parameswaran
 * 
 */

package org.ohmage.triggers.types.activity;

import org.json.JSONObject;
import org.ohmage.R;
import org.ohmage.triggers.base.TriggerBase;
import org.ohmage.triggers.types.location.LocTrigEditActivity;
import org.ohmage.triggers.ui.TriggerListActivity;

import android.content.Context;
import android.content.Intent;

public class ActivityTrigger extends TriggerBase	{

	private static final String TAG = "LocationTrigger";
	
	//This string must be unique across all trigger types 
	//registered with the framework.
	private static final String TRIGGER_TYPE = "ActivityTrigger";
	
	
	
	@Override
	public String getTriggerType() {
		
		return TRIGGER_TYPE;
	}

	@Override
	public int getIcon() {
		return R.drawable.img_mobility_run;
		
	}

	@Override
	public String getTriggerTypeDisplayName(Context context) {
		return context.getString(R.string.trigger_activity_display_name);
	}

	@Override
	public String getDisplayTitle(Context context, String trigDesc) {
		ActTrigDesc actDesc = new ActTrigDesc();
		if (!actDesc.loadString(trigDesc)){
			return null;
		}
		
		String state = actDesc.getStateString();
		String result = state + " - ";
		int hours = actDesc.getDurationHours();
		int min = actDesc.getDurationMin();
		if (hours != 0){
			result += hours + "h ";
		}
		if (min != 0){
			result += min + "m";
		}
		return result;
	}

	@Override
	public String getDisplaySummary(Context context, String trigDesc) {
		ActTrigDesc desc = new ActTrigDesc();
		if (!desc.loadString(trigDesc)){
			return null;
		}
		if (!desc.isRangeEnabled()){
			return "Evaluate Always";
		}
		else return "Evaluate " + desc.getStartTime() + " - " + desc.getEndTime();
		
	}

	@Override
	public JSONObject getPreferences(Context context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void startTrigger(Context context, int trigId, String trigDesc) {
		Intent i = new Intent(context , ActTrigService.class);
		i.setAction(ActTrigService.ACTION_START_TRIGGER);
		i.putExtra(ActTrigService.KEY_TRIG_ID , trigId);
		i.putExtra(ActTrigService.KEY_TRIG_DESC , trigDesc);
		context.startService(i);
		
	}

	@Override
	public void resetTrigger(Context context, int trigId, String trigDesc) {
		Intent i = new Intent(context , ActTrigService.class);
		i.setAction(ActTrigService.ACTION_RESET_TRIGGER);
		i.putExtra(ActTrigService.KEY_TRIG_ID , trigId);
		i.putExtra(ActTrigService.KEY_TRIG_DESC , trigDesc);
		context.startService(i);
		
	}

	@Override
	public void stopTrigger(Context context, int trigId, String trigDesc) {
		Intent i = new Intent(context , ActTrigService.class);
		i.setAction(ActTrigService.ACTION_REMOVE_TRIGGER);
		i.putExtra(ActTrigService.KEY_TRIG_ID , trigId);
		i.putExtra(ActTrigService.KEY_TRIG_DESC , trigDesc);
		context.startService(i);
		
	}

	@Override
	public void launchTriggerCreateActivity(Context context, final String campaignUrn, String[] mActions, String[] mPreselActions,
			boolean adminMode) {
		//Register a listener with the editor
				//activity to listen for the 'done' event
				ActTrigEditActivity.setOnExitListener(
							new ActTrigEditActivity.ExitListener() {
					
					@Override
					public void onDone(Context context, int trigId, 
									   String trigDesc, String actDesc) {
						
						//When done, save the trigger to the db
						addNewTrigger(context, campaignUrn, trigDesc, actDesc);
					}
				});
				
				
				Intent i = new Intent(context, ActTrigEditActivity.class);
				i.putExtra(ActTrigEditActivity.KEY_ADMIN_MODE, adminMode);
				i.putExtra(TriggerListActivity.KEY_ACTIONS, mActions);
				if (mPreselActions != null)
					i.putExtra(TriggerListActivity.KEY_PRESELECTED_ACTIONS, mPreselActions);
				context.startActivity(i);
		
	}

	@Override
	public void launchTriggerEditActivity(Context context, int trigId,
			String trigDesc, String actDesc, String[] mActions,
			boolean adminMode) {

		ActTrigEditActivity.setOnExitListener(
					new ActTrigEditActivity.ExitListener() {
			
			@Override
			public void onDone(Context context, int trigId, 
							   String trigDesc, String actDesc) {
				
				//When done, save the trigger to the db
				updateTrigger(context, trigId, trigDesc, actDesc);
			}
		});

		//Pass the trigger details to the editor
				Intent i = new Intent(context, ActTrigEditActivity.class);
				i.putExtra(ActTrigEditActivity.KEY_TRIG_ID, trigId);
				i.putExtra(ActTrigEditActivity.KEY_TRIG_DESC, trigDesc);
				i.putExtra(ActTrigEditActivity.KEY_ACT_DESC, actDesc);
				i.putExtra(ActTrigEditActivity.KEY_ADMIN_MODE, adminMode);
				i.putExtra(TriggerListActivity.KEY_ACTIONS, mActions);
				context.startActivity(i);	
		
	}

	@Override
	public boolean hasSettings() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void launchSettingsEditActivity(Context context, boolean adminMode) {
		Intent i = new Intent(context , ActTrigSettingsActivity.class);
		context.startActivity(i);
		
	}

	@Override
	public void resetSettings(Context context) {
		// TODO Auto-generated method stub
		
	}
	
}