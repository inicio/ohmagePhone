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
package org.ohmage.prompt.photo;

import java.io.File;
import java.util.UUID;

import org.ohmage.R;
import org.ohmage.Utilities;
import org.ohmage.activity.SurveyActivity;
import org.ohmage.db.Models.Response;
import org.ohmage.prompt.AbstractPrompt;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import edu.ucla.cens.systemlog.Log;

public class PhotoPrompt extends AbstractPrompt {

	private static final String TAG = "PhotoPrompt";

	String mResolution;
	String uuid;
	private SurveyActivity mContext;
	
	//ImageView imageView;
	//Bitmap mBitmap;

	public PhotoPrompt() {
		super();
	}
	
	public void setResolution(String res) {
		this.mResolution = res;
	}
	
	/**
	 * Deletes the image from the file system if it was taken
	 */
	public void clearImage() {
		if(isPromptAnswered()) {
			getImageFile().delete();
		}
	}

	@Override
	protected void clearTypeSpecificResponseData() {
		// Delete the old file
		clearImage();
		uuid = null;
	}
	
	/**
	 * Returns true if the UUID is not null meaning that we have at least some
	 * image that we are referencing.
	 */
	@Override
	public boolean isPromptAnswered() {
		return(uuid != null);
	}

	@Override
	protected Object getTypeSpecificResponseObject() {
		return uuid;
	}
	
	/**
	 * The text to be displayed to the user if the prompt is considered
	 * unanswered.
	 */
	@Override
	public String getUnansweredPromptText() {
		return("Please take a picture of something before continuing.");
	}
	
	@Override
	protected Object getTypeSpecificExtrasObject() {
		return null;
	}
	
	@Override
	public void handleActivityResult(Context context, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {

			// Give the image two tries to resize... and then we just use the
			// original size? there is not much we can do here
			if(!Utilities.resizeImage(getImageFile(), 800)) {
				System.gc();
				Log.d(TAG, "First image resize failed. Trying again.");
				if(!Utilities.resizeImage(getImageFile(), 800)) {
					Log.d(TAG, "Second image resize failed. Using original size image");
				}
			}

			((SurveyActivity) context).reloadCurrentPrompt();
		} 
	}

	@Override
	public View getView(Context context) {
		
		mContext = (SurveyActivity) context;

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.prompt_photo, null);
		
		ImageButton button = (ImageButton) layout.findViewById(R.id.photo_button);
		ImageView imageView = (ImageView) layout.findViewById(R.id.image_view);
		
		if (isPromptAnswered()) {
			imageView.setImageBitmap(BitmapFactory.decodeFile(getImageFile().getAbsolutePath()));
		}

		final Activity act = (Activity) context;
		
		button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(getImageFile()));
				act.startActivityForResult(intent, REQUEST_CODE);

			}
		});
		
		return layout;
	}
	
	private File getImageFile() {
		if(uuid == null)
			uuid = UUID.randomUUID().toString();
		return Response.getTemporaryResponsesImage(mContext, uuid);
	}

	public void saveImageFile(String responseId) {
		File image = getImageFile();
		if(image != null && image.exists()) {
			image.renameTo(Response.getTemporaryResponsesImage(mContext, uuid));
		}
	}

	/**
	 * Recycles the image if it was set
	 * @param view
	 */
	public static void clearView(ViewGroup view) {
		ImageView imageView = (ImageView) view.findViewById(R.id.image_view);
		// If there is an old BitmapDrawable we have to recycle it
		if(imageView != null && imageView.getDrawable() instanceof BitmapDrawable) {
			Bitmap b = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
			if(b != null) {
				b.recycle();
				b = null;
			}
		}
	}
}
