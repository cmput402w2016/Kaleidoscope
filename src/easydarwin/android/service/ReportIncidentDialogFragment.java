package easydarwin.android.service;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.videolan.vlc.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;

public class ReportIncidentDialogFragment extends DialogFragment {
	
	String serviceTrafficUrl = "http://199.116.235.225:8000/traffic";
	LocationManager lm;
	Location location;
	int severity_level = 1;
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// get location of user
		lm = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);
 	    location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Report Traffic Incident In Your Location")
               .setSingleChoiceItems(R.array.traffic_incident_severity_levels, -1, new DialogInterface.OnClickListener() {
				
            	   @Override
            	   public void onClick(DialogInterface dialog, int which) {
            		   severity_level = which + 1;
            		   Log.d("OPTION", String.valueOf(which));
            		   
            	   }
			    })
               .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   // user reported incident
                	   if (location != null) {
                		   double longitude = location.getLongitude();
                    	   double latitude = location.getLatitude();
                    	   Long tsLong = System.currentTimeMillis()/1000;

                    	   JSONObject dataObject = new JSONObject();
                    	   try {
                    		   JSONObject locationObj = new JSONObject();
                    		   locationObj.put("lat", latitude);
                    		   locationObj.put("lon", longitude);
                    		   dataObject.put("key", "TRAFFIC_INCIDENT");
                    		   dataObject.put("value", severity_level);
                    		   dataObject.put("from", locationObj);
                    		   dataObject.put("to", locationObj);
                    		   dataObject.put("timestamp", tsLong);
                    		   
                    		   new ReportIncidentAsyncTask().execute(dataObject.toString());
                    	   } catch (JSONException e) {
                    		   e.printStackTrace();
                    	   }
                	   }
                   }
               })
               .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // User cancelled the dialog
                   }
               });
        // Create the AlertDialog object and return it
        return builder.create();
	}
	
	public class ReportIncidentAsyncTask extends AsyncTask<String, Integer, Double>{
		 
		@Override
		protected Double doInBackground(String... params) {
			postData(params[0]);
			return null;
		}
 
		public void postData(String data) {
			// Create a new HttpClient and Post Header
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(serviceTrafficUrl);

			try {
				JSONObject trafficIncidentInfo = new JSONObject(data);

	            httppost.setHeader("Accept", "application/json");
	            httppost.setHeader("Content-type", "application/json");
	            httppost.setEntity(new StringEntity(trafficIncidentInfo.toString()));
	            
				// Execute HTTP Post Request
				HttpResponse response = httpclient.execute(httppost);
				String result = EntityUtils.toString(response.getEntity());
				Log.i("response", result);
				
			} catch (JSONException | IOException e) {
				e.printStackTrace();
			} 
			
		}
 
	}
}
