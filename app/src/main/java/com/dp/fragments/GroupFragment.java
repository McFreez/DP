package com.dp.fragments;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.dp.GroupActivity;
import com.dp.R;
import com.dp.db.DbContract;
import com.dp.dto.Group;
import com.dp.dto.MyMarker;
import com.dp.dto.User;
import com.dp.networking.DirectionsRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.maps.android.PolyUtil;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GroupFragment extends BaseMapFragment
        implements OnUsersDataChangeListener {

    private static final String LOG_TAG = "GroupFragment";

    private FloatingActionButton mRouteFab;

    private Group mGroup;

    private FirebaseFirestore mDb;
    private FirebaseUser mUser;

    private Map<String, Marker> groupMembersMarkers = new HashMap<>();
    private Map<String, Marker> groupMarkers = new HashMap<>();
    
    private Polyline mRoute;

    private String mSelectedMarkerID = null;

    public GroupFragment(){
        super();
    }

    public interface OnRouteListener{
        boolean onClearRoute();
    }

    public interface OnMarkersChangeListener{
        void onAdded(MyMarker marker);
        void onUpdated(MyMarker marker);
        void onRemoved(String markerID);
        void onClear();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getActivity().setTitle(mGroup.getName());
        mUser = FirebaseAuth.getInstance().getCurrentUser();

        mRouteFab = view.findViewById(R.id.fab_route);
        mRouteFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(groupMarkers.containsKey(mSelectedMarkerID)) {
                    buildRoute(groupMarkers.get(mSelectedMarkerID).getPosition());
                } else
                    if(groupMembersMarkers.containsKey(mSelectedMarkerID)){
                        buildRoute(groupMembersMarkers.get(mSelectedMarkerID).getPosition());
                    }
            }
        });

        ((GroupActivity)getActivity()).setOnRouteListener(new OnRouteListener() {
            @Override
            public boolean onClearRoute() {
                return clearRoute();
            }
        });

        ((GroupActivity)getActivity()).setOnMarkersChangeListener(new OnMarkersChangeListener() {
            @Override
            public void onAdded(MyMarker marker) {
                addMarker(marker);
            }

            @Override
            public void onUpdated(MyMarker marker) {
                updateMarker(marker);
            }

            @Override
            public void onRemoved(String markerID) {
                removeMarker(markerID);
            }

            @Override
            public void onClear() {
                for(Map.Entry<String, Marker> entry : groupMarkers.entrySet()){
                    Marker marker = entry.getValue();
                    marker.remove();
                }
                groupMarkers.clear();
            }
        });

        mEventListener = new EventListener() {
            @Override
            public void onLocationChanged(final LatLng latLng) {
                mDb.collection(DbContract.getUserGroups(mUser.getUid()))
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot documentSnapshots) {

                                WriteBatch batch = mDb.batch();

                                Map<String, Object> user = DbContract.UserObject.updateUserData(mUser.getDisplayName(), mUser.getEmail(), latLng.latitude, latLng.longitude);

                                DocumentReference userProfile = mDb.document(DbContract.addOrUpdateUser(mUser.getUid()));
                                        /*.set(user, SetOptions.merge());*/
                                batch.set(userProfile, user, SetOptions.merge());

                                for(DocumentSnapshot doc : documentSnapshots.getDocuments()){
                                    userProfile = mDb.document(DbContract.addOrUpdateGroupUser(doc.getId(), mUser.getUid()));
                                    /*.set(user, SetOptions.merge());*/
                                    batch.set(userProfile, user, SetOptions.merge());
                                }

                                batch.commit()
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful())
                                            Log.d(LOG_TAG, "My location updated to lat: " + latLng.latitude + " lng: " + latLng.longitude);
                                        else
                                            Log.d(LOG_TAG, "My location update failed");
                                    }
                                });

                            }
                        });
            }
        };

    }

    private void addMarker(MyMarker marker){
        if(mMap != null){
            LatLng position = new LatLng(marker.getLatitude(), marker.getLongitude());
            Marker mapMarker = mMap.addMarker(new MarkerOptions()
                    .position(position));

            mapMarker.setTag(marker.getId());

            if(marker.getName() != null && !marker.getName().equals("")){
                mapMarker.setTitle(marker.getName());
                if(marker.getAddress() != null && !marker.getAddress().equals(""))
                    mapMarker.setSnippet(marker.getAddress());
            } else
                if(marker.getAddress() != null && !marker.getAddress().equals("")){
                    mapMarker.setTitle(marker.getAddress());
                }
            groupMarkers.put(marker.getId(), mapMarker);
        }
    }

    private void updateMarker(MyMarker marker){
        if(mMap != null){
            if(!groupMarkers.containsKey(marker.getId()))
                addMarker(marker);
            else{
                Marker mapMarker = groupMarkers.get(marker.getId());
                mapMarker.setTag(marker.getId());
                if(marker.getName() != null && !marker.getName().equals("")){
                    mapMarker.setTitle(marker.getName());
                    if(marker.getAddress() != null && !marker.getAddress().equals(""))
                        mapMarker.setSnippet(marker.getAddress());
                } else
                if(marker.getAddress() != null && !marker.getAddress().equals("")){
                    mapMarker.setTitle(marker.getAddress());
                }
                groupMarkers.put(marker.getId(), mapMarker);
            }
        }
    }

    private void removeMarker(String markerID){
        if(groupMarkers.containsKey(markerID)){
            Marker mapMarker = groupMarkers.get(markerID);
            mapMarker.remove();
            groupMarkers.remove(markerID);
        }
    }

    private void addUserMarker(User user){
        if(mMap != null){
            if(user.getLatitude() != 0 && user.getLongitude() != 0) {
                LatLng position = new LatLng(user.getLatitude(), user.getLongitude());
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .icon(bitmapDescriptorFromVector(R.drawable.ic_ally))
                        .position(position)
                        .title(user.getName()));
                marker.setTag(user.getId());
                groupMembersMarkers.put(user.getId(), marker);
            }
        }
    }

    private void updateUserMarker(User user){
        if(mMap != null){
            if(!groupMembersMarkers.containsKey(user.getId()))
                addUserMarker(user);
            else
            {
                Marker marker = groupMembersMarkers.get(user.getId());
                if(user.getLatitude() != 0 && user.getLongitude() != 0) {
                    LatLng newPosition = new LatLng(user.getLatitude(), user.getLongitude());
                    marker.setPosition(newPosition);
                    groupMembersMarkers.put(user.getId(), marker);
                }
            }
        }
    }

    private void removeUserMarker(String userGoogleID){
        if(groupMembersMarkers.containsKey(userGoogleID)){
            Marker marker = groupMembersMarkers.get(userGoogleID);
            marker.remove();
            groupMembersMarkers.remove(userGoogleID);
        }
    }

    private BitmapDescriptor bitmapDescriptorFromVector(int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(getContext(), vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        super.onMapReady(googleMap);

        for(Map.Entry<String, User> userEntry : mGroup.getUsers().entrySet()){
            if(userEntry.getKey().equals(mUser.getUid()))
                continue;

            updateUserMarker(userEntry.getValue());
        }

        for(Map.Entry<String, MyMarker> markerEntry : mGroup.getMarkers().entrySet()){
            updateMarker(markerEntry.getValue());
        }

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng position) {

                Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                List<Address> addresses = null;
                try {
                    addresses = geocoder.getFromLocation(position.latitude, position.longitude, 1);
                } catch (IOException e){
                    Log.d(LOG_TAG, "Address not found: " + e.getMessage());
                }

                StringBuilder address = new StringBuilder();

                if(addresses != null && addresses.size() > 0){
                    int maxLineIndex = addresses.get(0).getMaxAddressLineIndex();
                    for(int i = 0; i <= maxLineIndex; i++){
                        if(i > 0 && maxLineIndex > 0)
                            address.append(", ");

                        address.append(addresses.get(0).getAddressLine(i));
                    }
                }

                MyMarker marker = new MyMarker(address.toString(), mGroup.getId(), position.latitude, position.longitude);

                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                MarkerDialogFragment fragment = new MarkerDialogFragment()
                        .setDb(mDb)
                        .setMarker(marker);

                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                transaction.add(R.id.drawer_layout, fragment)
                        .addToBackStack(null)
                        .commit();
                //final Marker marker = mMap.addUserMarker(new MarkerOptions().icon(bitmapDescriptorFromVector(R.drawable.ic_enemy)).position(position).title(enemyTitleAlive));

                /*Map<String, Object> markerObject = new HashMap<>();
                markerObject.put("lat", position.latitude);
                markerObject.put("lon", position.longitude);
                markerObject.put("dead", false);*/



/*                mDb.collection(DbContract.getGameMarkersPath(mGameID, mTeamID))
                        .add(markerObject);
*//*                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference doc) {
                                marker.setTag(doc.getId());
                                enemyMarkers.add(marker);
                            }
                        });*/
            }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                clearFocus();
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker mapMarker) {
                if(mSelectedMarkerID == null) {
                    mapMarker.showInfoWindow();
                    updateFocus((String)mapMarker.getTag());
                    return true;
                } else
                    if(!mSelectedMarkerID.equals(mapMarker.getTag())){
                        mapMarker.showInfoWindow();
                        updateFocus((String)mapMarker.getTag());
                        return true;
                    }

                MyMarker marker = mGroup.getMarker((String)mapMarker.getTag());

                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                MarkerDialogFragment fragment = new MarkerDialogFragment()
                        .setDb(mDb)
                        .setMarker(marker);

                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                transaction.add(R.id.drawer_layout, fragment)
                        .addToBackStack(null)
                        .commit();

                clearFocus();

                return true;
            }
        });
    }

    private void clearFocus(){
        mSelectedMarkerID = null;
        mRouteFab.setVisibility(View.INVISIBLE);
    }

    private void updateFocus(String focusedMarkerID){
        mRouteFab.setVisibility(View.VISIBLE);
        mSelectedMarkerID = focusedMarkerID;
    }

    private void buildRoute(LatLng position){
        if(mLastKnownLocation == null){
            Toast.makeText(getContext(), "Unknown location", Toast.LENGTH_SHORT).show();
            return;
        }

        clearRoute();

        DirectionsRequest.getInstance(getContext())
                .origin(new com.google.maps.model.LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()))
                .destination(new com.google.maps.model.LatLng(position.latitude, position.longitude))
                .mode(TravelMode.DRIVING)
                .execute().addOnCompleteListener(new DirectionsRequest.OnCompleteListener() {
            @Override
            public void onSuccess(DirectionsResult result) {
                /*mRouteFinish = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(result.routes[0].legs[0].endLocation.lat, result.routes[0].legs[0].endLocation.lng))
                        .title(result.routes[0].legs[0].endAddress));*/
                mRoute = mMap.addPolyline(new PolylineOptions().addAll(PolyUtil.decode(result.routes[0].overviewPolyline.getEncodedPath())));
            }

            @Override
            public void onFailed() {
                Toast.makeText(getContext(), "Route not found", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Log.d(LOG_TAG, "Error while searching route: " + e.getMessage());
            }
        });
    }

    private boolean clearRoute(){
        if(mRoute != null){
            mRoute.remove();
            mRoute = null;
            return true;
        }

        return false;
    }

    public void setDb(FirebaseFirestore db){
        mDb = db;
    }

    public void setGroup(Group group){
        mGroup = group;
    }

    @Override
    public void onUserAdded(User user) {
        addUserMarker(user);
    }

    @Override
    public void onUserRemoved(String userGoogleID) {
        removeUserMarker(userGoogleID);
    }

    @Override
    public void onUserModified(User user) {
        updateUserMarker(user);
    }

    @Override
    public void onClear() {
        for(Map.Entry<String, Marker> entry : groupMembersMarkers.entrySet()){
            Marker marker = entry.getValue();
            marker.remove();
        }
        groupMembersMarkers.clear();
    }
}
