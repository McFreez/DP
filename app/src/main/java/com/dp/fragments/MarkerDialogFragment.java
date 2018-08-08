package com.dp.fragments;

import android.app.Dialog;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import com.dp.R;
import com.dp.db.DbContract;
import com.dp.dto.MyMarker;
import com.google.firebase.firestore.FirebaseFirestore;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MarkerDialogFragment extends DialogFragment {

    private static final String TITLE_EDIT = "Edit marker";
    private static final String TITLE_NEW = "New marker";

    @BindView(R.id.fragment_dialog_marker_toolbar)
    Toolbar mToolbar;
    @BindView(R.id.et_marker_name_layout)
    TextInputLayout mMarkerNameLayout;
    @BindView(R.id.et_marker_name)
    TextInputEditText mMarkerNameEditText;
    @BindView(R.id.et_marker_address)
    TextInputEditText mMarkerAddressEditText;

    private Unbinder mUnbinder;

    private FirebaseFirestore mDb;

    private MyMarker mMarker;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dialog_marker, container, false);

        mUnbinder = ButterKnife.bind(this, view);

        if(mMarker.getId() == null)
            mToolbar.setTitle(TITLE_NEW);
        else
            mToolbar.setTitle(TITLE_EDIT);

        mToolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        setHasOptionsMenu(true);
        ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);

        return view;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        if(mMarker != null){
            if(mMarker.getName() != null)
                mMarkerNameEditText.setText(mMarker.getName());
            if(mMarker.getAddress() != null)
                mMarkerAddressEditText.setText(mMarker.getAddress());
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        getActivity().getMenuInflater().inflate(R.menu.fragment_marker_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if(id == android.R.id.home){
            InputMethodManager inputMethodManager = (InputMethodManager)
                    getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
            dismiss();
            return true;
        } else
            if(id == R.id.action_save){
                InputMethodManager inputMethodManager = (InputMethodManager)
                        getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                saveMarker();
                dismiss();
                return true;

            } else
                if(id == R.id.action_delete){
                    InputMethodManager inputMethodManager = (InputMethodManager)
                            getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                    deleteMarker();
                    dismiss();
                    return true;
                }

        return super.onOptionsItemSelected(item);
    }

    private void saveMarker(){
        mDb.collection(DbContract.addMarker(mMarker.getGroupID()))
                .add(DbContract.MarkerObject.updateMarker(mMarkerNameEditText.getText().toString(),
                        mMarker.getAddress(),
                        mMarker.getLatitude(),
                        mMarker.getLongitude()));
    }

    private void deleteMarker(){
        if(mMarker.getId() == null)
            return;

        mDb.document(DbContract.getMarker(mMarker.getGroupID(), mMarker.getId()))
                .delete();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    public MarkerDialogFragment setDb(FirebaseFirestore db){
        mDb = db;
        return this;
    }

    public MarkerDialogFragment setMarker(MyMarker marker){
        mMarker = marker;
        return this;
    }
}
