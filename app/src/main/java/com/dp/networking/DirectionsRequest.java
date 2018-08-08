package com.dp.networking;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.dp.R;

import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;

import java.util.concurrent.TimeUnit;

public class DirectionsRequest {

    private DirectionsTask mTask;

    public static DirectionsRequest getInstance(Context context){
        return new DirectionsRequest(context);
    }

    private DirectionsRequest(Context context){
        mTask = new DirectionsTask(getGeoContext(context));
    }

    public interface OnCompleteListener{
        void onSuccess(DirectionsResult result);
        void onFailed();
        void onError(Exception e);
    }

    private GeoApiContext getGeoContext(Context context) {
        GeoApiContext geoApiContext = new GeoApiContext();
        return geoApiContext.setQueryRateLimit(3)
                .setApiKey(context.getString(R.string.google_maps_key))
                .setConnectTimeout(1, TimeUnit.SECONDS)
                .setReadTimeout(1, TimeUnit.SECONDS)
                .setWriteTimeout(1, TimeUnit.SECONDS);
    }

    public DirectionsRequest mode(TravelMode mode){
        mTask.setTravelMode(mode);
        return this;
    }

    public DirectionsRequest origin(LatLng origin){
        mTask.setOrigin(origin);
        return this;
    }

    public DirectionsRequest destination(LatLng destination){
        mTask.setDestination(destination);
        return this;
    }

    public DirectionsTask execute(){
        mTask.execute();
        return mTask;
    }

    public static class DirectionsTask extends AsyncTask<Void, Void, DirectionsResult>{

        private static final String LOG_TAG = "DirectionsTask";

        private OnCompleteListener mListener;

        private GeoApiContext mContext;

        private TravelMode mTravelMode;
        private LatLng mOrigin;
        private LatLng mDestination;

        private Exception mThrownException;

        DirectionsTask(GeoApiContext context){
            mContext = context;
        }

        void setTravelMode(TravelMode travelMode){
            mTravelMode = travelMode;
        }

        void setOrigin(LatLng origin){
            mOrigin = origin;
        }

        void setDestination(LatLng destination){
            mDestination = destination;
        }

        public void addOnCompleteListener(OnCompleteListener listener){
            mListener = listener;
        }

        @Override
        protected DirectionsResult doInBackground(Void... voids) {
            DirectionsResult result;
            try{
                result = DirectionsApi.newRequest(mContext)
                        .origin(mOrigin)
                        .destination(mDestination)
                        .mode(mTravelMode)
                        .await();

            } catch (Exception e){
                Log.d(LOG_TAG, "Error while executing directions task; " + e.getMessage());
                e.printStackTrace();
                mThrownException = e;

                return null;
            }

            return result;
        }

        @Override
        protected void onPostExecute(DirectionsResult result) {
            if(mListener != null){
                if(result != null) {
                    if(result.routes.length > 0)
                        mListener.onSuccess(result);
                    else
                        mListener.onFailed();
                }
                else
                    mListener.onError(mThrownException);
            }
        }
    }
}
