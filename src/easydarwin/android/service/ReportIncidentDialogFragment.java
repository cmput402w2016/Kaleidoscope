package easydarwin.android.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
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
import android.widget.Toast;

public class ReportIncidentDialogFragment extends DialogFragment {
	
	String serviceTrafficUrl = "199.116.235.225:8000/traffic";
	LocationManager lm;
	Location location;
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		lm = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);
 	    location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Report Traffic Incident")
               .setSingleChoiceItems(R.array.traffic_incident_severity_levels, -1, new DialogInterface.OnClickListener() {
				
            	   @Override
            	   public void onClick(DialogInterface dialog, int which) {
            		   // TODO Auto-generated method stub
            		   Log.d("OPTION", String.valueOf(which));
            		   
            	   }
			    })
               .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   // user reported incident
                	   
                	   if (location != null) {
                		   Log.d("LOC", "LOCATION NOT NULL");
                		   double longitude = location.getLongitude();
                    	   double latitude = location.getLatitude();
                    	   Long tsLong = System.currentTimeMillis()/1000;
                    	   JSONObject dataObject = new JSONObject();
                    	   try {
                    		   JSONObject locationObj = new JSONObject();
                    		   locationObj.put("lat", latitude);
                    		   locationObj.put("lon", longitude);
                    		   dataObject.put("key", "TRAFFIC_INCIDENT");
                    		   dataObject.put("value", "1");
                    		   dataObject.put("from", locationObj);
                    		   dataObject.put("to", locationObj);
                    		   dataObject.put("timestamp", tsLong);
                    		   Log.d("JSON", dataObject.toString());
                    		   
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
//			HttpClient httpclient = new DefaultHttpClient();
//			HttpPost httppost = new HttpPost(serviceTrafficUrl);
			
			try {
				JSONObject trafficIncidentInfo = new JSONObject(data);
				
				List<NameValuePair> params = new ArrayList<NameValuePair>();
	            params.add(new BasicNameValuePair("key", trafficIncidentInfo.get("key").toString()));
	            params.add(new BasicNameValuePair("value", trafficIncidentInfo.get("value").toString()));

//	            httppost.setEntity(new UrlEncodedFormEntity(params));
	            
				// Execute HTTP Post Request
//				HttpResponse response = httpclient.execute(httppost);
//				Toast.makeText(getActivity(), "Traffic Incident Reported!", Toast.LENGTH_SHORT).show();
//				String result = EntityUtils.toString(response.getEntity());
//				Log.i("response", result);
				
			} catch (JSONException e) {
				//e.printStackTrace();
			} 
			
		}
 
	}
}
