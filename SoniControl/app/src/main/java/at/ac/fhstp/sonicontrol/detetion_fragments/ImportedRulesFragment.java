package at.ac.fhstp.sonicontrol.detetion_fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
//import android.support.annotation.Nullable;
//import android.support.design.widget.FloatingActionButton;
//import android.support.design.widget.Snackbar;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
//import androidx.core.app.Fragment;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;


import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import at.ac.fhstp.sonicontrol.ConfigConstants;
import at.ac.fhstp.sonicontrol.GPSTracker;
import at.ac.fhstp.sonicontrol.JSONManager;
import at.ac.fhstp.sonicontrol.Location;
import at.ac.fhstp.sonicontrol.MainActivity;
import at.ac.fhstp.sonicontrol.R;
import at.ac.fhstp.sonicontrol.StoredLocations;
import at.ac.fhstp.sonicontrol.Stored_Adapter;
import at.ac.fhstp.sonicontrol.Technology;
import at.ac.fhstp.sonicontrol.rest.RESTController;
import at.ac.fhstp.sonicontrol.rest.SoniControlAPI;
import at.ac.fhstp.sonicontrol.utils.LocationSuggestion;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class ImportedRulesFragment extends Fragment implements Stored_Adapter.OnItemClickListener{
    MainActivity main = new MainActivity();
    private static StoredLocations instanceStoredLoc;
    JSONManager jsonMan;
    ArrayList<String[]> data;
    Stored_Adapter stored_adapter;
    ListView lv;
    MainActivity nextMain;
    AlertDialog alertDelete = null;

    AdapterView<?> parentLongClick;
    int positionLongClick;

    TextView txtNothingDiscovered;
    FloatingActionButton fabImportDetections;

    View rootView;
    AlertDialog filterDialog;
    View view;
    AlertDialog dateTimeDialog;
    View viewDateTime;

    AlertDialog filterImportDialog;

    Button btnFindPlace;

    Button btnTimestampFrom;
    Button btnTimestampTo;
    Button btnImport;
    Button btnCancel;

    Button btnDateTimeSet;
    Button btnDateTimeCancel;

    Button btnResetTimestampFrom;
    Button btnResetTimestampTo;

    //EditText edtLocation;
    EditText edtRange;
    Spinner spnTechnology;

    TextView txtPlace;
    TextView txtTimestampFrom;
    TextView txtTimestampTo;

    Long timeFrom;
    Long timeTo;
    DatePicker datePicker;
    AutoCompleteTextView actPosition;

    private Handler textChangedHandler;
    Runnable textChangedRun;
    private Runnable inputFinishChecker;
    long delay = 100;
    AtomicLong lastTextEdit = new AtomicLong(0);

    LocationSuggestion locationSuggestion;

    ImportedRulesFragment localContext;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.imported_rules_fragment, null);

        nextMain = main.getMainIsMain();
        jsonMan = JSONManager.getInstanceJSONManager();//new JSONManager(nextMain);

        data = jsonMan.getImportJsonData();

        localContext = this;

        txtNothingDiscovered = (TextView) rootView.findViewById(R.id.txtNoDetectionsYet);
        fabImportDetections = (FloatingActionButton) rootView.findViewById(R.id.fabImportDetections);
        fabImportDetections.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterDialog.show();

            }
        });

        if(data.size()==0){
            txtNothingDiscovered.setVisibility(View.VISIBLE);
        }else {
            txtNothingDiscovered.setVisibility(View.INVISIBLE);
        }

        lv = (ListView) rootView.findViewById(R.id.storedListView);
        lv.setAdapter(null);
        final Context listContext = getActivity();
        stored_adapter = new Stored_Adapter(getActivity(), data);
        stored_adapter.addOnItemClickListener(localContext);


        final AlertDialog.Builder deleteJsonDialog = new AlertDialog.Builder(getActivity());
        deleteJsonDialog.setTitle(R.string.DeleteJsonAlertTitle)
                .setMessage(R.string.DeleteJsonAlertMessage)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String[] singleArrayItem = (String[]) parentLongClick.getItemAtPosition(positionLongClick);
                        double[] positionSignal = new double[2];
                        positionSignal[0] = Double.valueOf(singleArrayItem[0]);
                        positionSignal[1] = Double.valueOf(singleArrayItem[1]);
                        jsonMan = JSONManager.getInstanceJSONManager();
                        if(data.size()==0){
                            txtNothingDiscovered.setVisibility(View.VISIBLE);
                        }else {
                            txtNothingDiscovered.setVisibility(View.INVISIBLE);
                        }
                        data.remove(positionLongClick);
                        jsonMan.deleteImportEntry(positionSignal,singleArrayItem[2]);
                        stored_adapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        alertDelete.cancel();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert);

        lv.setAdapter(stored_adapter);

        lv.setOnItemClickListener(
                new AdapterView.OnItemClickListener(){
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    }
                }
        );

        lv.setOnItemLongClickListener(
                new AdapterView.OnItemLongClickListener(){
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        parentLongClick = parent;
                        positionLongClick = position;
                        alertDelete = deleteJsonDialog.show();
                        return true;
                    }
                }
        );


        final AlertDialog.Builder openFilter = new AlertDialog.Builder(getActivity());
        openFilter.setCancelable(true);
        LayoutInflater inflaterImport = getLayoutInflater();
        final ViewGroup viewGroup = (ViewGroup) rootView.findViewById(android.R.id.content);
        view = inflaterImport.inflate(R.layout.detection_import_filter, viewGroup , false);
        openFilter.setView(view);
        filterDialog = openFilter.create();

        actPosition = (AutoCompleteTextView) view.findViewById(R.id.actPosition);
        actPosition.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textChangedHandler.removeCallbacks(inputFinishChecker);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) { //TODO Asynctask
                    lastTextEdit.set(System.currentTimeMillis());
                    textChangedHandler.postDelayed(inputFinishChecker, delay);
                }
            }
        });
        textChangedHandler = new Handler();

        inputFinishChecker = new Runnable() {
            public void run() {
                if(locationSuggestion != null){
                    locationSuggestion.cancel(true);
                }
                locationSuggestion = new LocationSuggestion(lastTextEdit, actPosition, getContext());
                locationSuggestion.execute();
            }
        };

        txtTimestampFrom = (TextView) view.findViewById(R.id.txtTimestampFrom);
        txtTimestampTo = (TextView) view.findViewById(R.id.txtTimestampTo);
        edtRange = (EditText) view.findViewById(R.id.edtRange);

        txtPlace = (TextView) view.findViewById(R.id.txtPlace);

        btnFindPlace = (Button) view.findViewById(R.id.btnFindPlace);
        btnFindPlace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        btnTimestampFrom = (Button) view.findViewById(R.id.btnTimestampFrom);
        btnTimestampFrom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDateTimePickerFrom();
            }
        });

        btnTimestampTo = (Button) view.findViewById(R.id.btnTimestampTo);
        btnTimestampTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDateTimePickerTo();
            }
        });

        btnResetTimestampFrom = (Button) view.findViewById(R.id.btnResetTimestampFrom);
        btnResetTimestampFrom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtTimestampFrom.setText("");
                btnResetTimestampFrom.setVisibility(View.INVISIBLE);
                timeFrom = null;
            }
        });
        btnResetTimestampTo = (Button) view.findViewById(R.id.btnResetTimestampTo);
        btnResetTimestampTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtTimestampTo.setText("");
                btnResetTimestampTo.setVisibility(View.INVISIBLE);
                timeTo = null;
            }
        });

        btnImport = (Button) view.findViewById(R.id.btnImport);
        btnImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double[] position = new double[2];
                int range = 0;
                int technology;
                String timestampFrom = null;
                String timestampTo = null;
                Location location = Location.getInstanceLoc();
                GPSTracker gpsTracker = location.getGPSTracker();
                if(!txtPlace.getText().toString().matches("")){
                    position = gpsTracker.getLocationFromAddress(txtPlace.getText().toString(), getActivity());
                }else{
                    position[0] = ConfigConstants.NO_VALUES_ENTERED;
                    position[1] = ConfigConstants.NO_VALUES_ENTERED;
                }
                if(!edtRange.getText().toString().matches("")){
                    range = Integer.valueOf(edtRange.getText().toString());
                }else{
                    range = ConfigConstants.NO_VALUES_ENTERED;
                }
                if(spnTechnology.getSelectedItem().toString().equals("All")){
                    technology = ConfigConstants.ALL_TECHNOLOGIES;
                }else{
                    technology = Technology.fromString(spnTechnology.getSelectedItem().toString()).getId();
                }

                if(jsonMan.returnDateStringFromAlert(timeFrom) != null){
                    timestampFrom = jsonMan.returnDateStringFromAlert(timeFrom);
                }
                if(jsonMan.returnDateStringFromAlert(timeTo) != null){
                    timestampTo = jsonMan.returnDateStringFromAlert(timeTo);
                }
                if(!txtPlace.getText().toString().matches("")) {
                    getNumberOfDetections((position[0] / 1000000), (position[1] / 1000000), range, technology, timestampFrom, timestampTo, openLoadingDialogImport());
                }else{
                    getNumberOfDetections(position[0], position[1], range, technology, timestampFrom, timestampTo, openLoadingDialogImport());
                }
                filterDialog.cancel();
            }
        });

        btnCancel = (Button) view.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterDialog.cancel();
            }
        });

        spnTechnology = (Spinner) view.findViewById(R.id.spnTechnology);
        String[] numberOfFrequencyElements = new String[] {
                "All", Technology.UNKNOWN.toString(), Technology.GOOGLE_NEARBY.toString(), Technology.PRONTOLY.toString(), Technology.SONARAX.toString(), Technology.SIGNAL360.toString(),
                    Technology.SHOPKICK.toString(), Technology.SILVERPUSH.toString(), Technology.LISNR.toString(), Technology.SONITALK.toString()
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, numberOfFrequencyElements);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnTechnology.setAdapter(adapter);
        spnTechnology.setSelection(0);
        //just change the fragment_dashboard
        //with the fragment you want to inflate
        //like if the class is HomeFragment it should have R.layout.home_fragment
        //if it is DashboardFragment it should have R.layout.fragment_dashboard
        return rootView;
    }

    public AlertDialog openLoadingDialogImport(){
        final AlertDialog.Builder loadingImport = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = this.getLayoutInflater();
        View loadingView = inflater.inflate(R.layout.loading_screen, null);
        loadingImport.setView(loadingView);
        loadingImport.setTitle(getString(R.string.loading_dialog_import_detections))
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_alert);
        return loadingImport.show();
    }

    public void getDetections(final double longitude, final double latitude, final int range, final int technologyid, final String timestampfrom, final String timestampto, final AlertDialog loadingDialog) {
        main.threadPool.execute(new Runnable() {

            JSONArray jArray;

            @Override
            public void run() {
                final SoniControlAPI restService = RESTController.getRetrofitInstance().create(SoniControlAPI.class);

                restService.importDetections(longitude, latitude, range, technologyid, timestampfrom, timestampto).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        String detailsString = getStringFromRetrofitResponse(response.body());
                        try {
                            jArray = new JSONArray(detailsString);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        double[] position = new double[2];
                        String technology = null;
                        int spoof = -1;
                        String address = null;
                        String timestamp = null;
                        int technologyid = -1;
                        float amplitude = 0f;
                        Location location = Location.getInstanceLoc();
                        GPSTracker gpsTracker = location.getGPSTracker();

                        for (int i = 0; i < jArray.length(); i++) {
                            try {
                                position[0] = jArray.getJSONObject(i).getJSONObject("detection").getJSONObject("location").getJSONArray("coordinates").getDouble(0);
                                position[1] = jArray.getJSONObject(i).getJSONObject("detection").getJSONObject("location").getJSONArray("coordinates").getDouble(1);
                                technology = jArray.getJSONObject(i).getJSONObject("detection").getString("technology");
                                address = gpsTracker.getAddressFromPoint(position[1], position[0], getActivity());
                                spoof = jArray.getJSONObject(i).getJSONObject("detection").getInt("spoofDecision");
                                timestamp = jArray.getJSONObject(i).getJSONObject("detection").getString("timestamp");
                                technologyid = jArray.getJSONObject(i).getJSONObject("detection").getInt("technologyid");
                                amplitude = jArray.getJSONObject(i).getJSONObject("detection").getInt("amplitude");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if(position[0]!=0 && position[1]!=0 && spoof!=-1 && address!=null && timestamp!=null){
                                jsonMan.addImportedJsonObject(position, technology, spoof, address, timestamp, technologyid, amplitude);
                            }
                        }
                        jArray = null;
                        data = jsonMan.getImportJsonData();
                        if(data.size()==0){
                            txtNothingDiscovered.setVisibility(View.VISIBLE);
                        }else {
                            txtNothingDiscovered.setVisibility(View.INVISIBLE);
                        }
                        lv.setAdapter(null);
                        stored_adapter = new Stored_Adapter(getActivity(), data);
                        lv.setAdapter(stored_adapter);
                        loadingDialog.cancel();

                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        loadingDialog.cancel();
                        Log.e("StoredDetections", "Unable to submit post to API." + t);
                        Snackbar importSnackbar = Snackbar.make(rootView, R.string.import_filtered_detections_failure_snackbar,
                                Snackbar.LENGTH_INDEFINITE)
                                .setAction("OK", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                    }
                                });
                        View importSnackbarView = importSnackbar.getView();
                        TextView importSnackbarTextView = (TextView) importSnackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
                        importSnackbarTextView.setMaxLines(4);
                        importSnackbar.show();
                    }
                });
            }
        });
    }
    public void getNumberOfDetections(final double longitude, final double latitude, final int range, final int technologyid, final String timestampfrom, final String timestampto, final AlertDialog loadingDialog) {
        main.threadPool.execute(new Runnable() {
            JSONArray jArray;
            int numberOfFiles;

            @Override
            public void run() {
                final SoniControlAPI restService = RESTController.getRetrofitInstance().create(SoniControlAPI.class);

                restService.getNumberOfImportDetections(longitude, latitude, range, technologyid, timestampfrom, timestampto).enqueue(new Callback<ResponseBody>() {

                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        loadingDialog.cancel();
                        String detailsString = getStringFromRetrofitResponse(response.body());

                        numberOfFiles = Integer.valueOf(detailsString);
                        openImportDialogWithCount(numberOfFiles, longitude, latitude, range, technologyid, timestampfrom, timestampto);
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.e("StoredDetections", "Unable to get numberOfDetections." + t);
                        loadingDialog.cancel();
                        Snackbar importSnackbar = Snackbar.make(rootView, R.string.import_filtered_detections_failure_snackbar,
                                Snackbar.LENGTH_INDEFINITE)
                                .setAction("OK", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                    }
                                });
                        View importSnackbarView = importSnackbar.getView();
                        TextView importSnackbarTextView = (TextView) importSnackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
                        importSnackbarTextView.setMaxLines(4);
                        importSnackbar.show();
                    }
                });
            }
        });
    }

    public static String getStringFromRetrofitResponse(ResponseBody response) {
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();

        reader = new BufferedReader(new InputStreamReader(response.byteStream()));

        String line;

        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();

    }

    public void openDateTimePicker(){
        final AlertDialog.Builder openDateTime = new AlertDialog.Builder(getActivity());
        openDateTime.setCancelable(true);
        LayoutInflater inflaterD = getLayoutInflater();
        final ViewGroup viewGroupD = (ViewGroup) getView().findViewById(android.R.id.content);
        viewDateTime = inflaterD.inflate(R.layout.date_time_picker, viewGroupD , false);
        openDateTime.setView(viewDateTime);
        dateTimeDialog = openDateTime.create();

        btnDateTimeSet = (Button) viewDateTime.findViewById(R.id.btnDateTimeSet);
        btnDateTimeCancel = (Button) viewDateTime.findViewById(R.id.btnDateTimeCancel);
        datePicker = (DatePicker) viewDateTime.findViewById(R.id.date_picker);
    }

    public void openDateTimePickerFrom() {
        openDateTimePicker();
        btnDateTimeSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Calendar calendar = new GregorianCalendar(datePicker.getYear(),
                        datePicker.getMonth(),
                        datePicker.getDayOfMonth()
                        );

                timeFrom = calendar.getTimeInMillis();
                dateTimeDialog.dismiss();
                txtTimestampFrom.setText(jsonMan.returnDateStringFromAlert(timeFrom).replace("Z", "").replace("T", " \n"));
                btnResetTimestampFrom.setVisibility(View.VISIBLE);
            }
        });

        btnDateTimeCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dateTimeDialog.cancel();
            }
        });
        dateTimeDialog.setView(viewDateTime);
        dateTimeDialog.show();
    }

    public void openDateTimePickerTo() {
        openDateTimePicker();
        btnDateTimeSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Calendar calendar = new GregorianCalendar(datePicker.getYear(),
                        datePicker.getMonth(),
                        datePicker.getDayOfMonth()
                );

                timeTo = calendar.getTimeInMillis();
                dateTimeDialog.dismiss();
                txtTimestampTo.setText(jsonMan.returnDateStringFromAlert(timeTo).replace("Z", "").replace("T", " \n"));
                btnResetTimestampTo.setVisibility(View.VISIBLE);
            }
        });
        dateTimeDialog.setView(viewDateTime);
        dateTimeDialog.show();
    }

    private void openImportDialogWithCount(int count, final double longitude, final double latitude, final int range, final int technologyid, final String timestampfrom, final String timestampto){
        final AlertDialog.Builder filterImport = new AlertDialog.Builder(getActivity());
        filterImport.setTitle(getString(R.string.filter_import_dialog_title))
                .setMessage(String.format(getString(R.string.filter_import_dialog_message), String.valueOf(count)))
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        getDetections(longitude, latitude, range, technologyid, timestampfrom, timestampto, openLoadingDialogImport());
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        filterImportDialog.cancel();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert);
        filterImportDialog = filterImport.show();
    }

    @Override
    public void onAllowedClick(String[] singleArrayItem, int spoofingStatus) {
        changeRuleItem(singleArrayItem, spoofingStatus);
    }

    @Override
    public void onBlockedClick(String[] singleArrayItem, int spoofingStatus) {
        changeRuleItem(singleArrayItem, spoofingStatus);
    }

    @Override
    public void onAskAgainClick(String[] singleArrayItem, int spoofingStatus) {
        changeRuleItem(singleArrayItem, spoofingStatus);
    }

    private void changeRuleItem(String[] singleArrayItem, int spoofingStatus){
        double[] positionSignal = new double[2];
        positionSignal[0] = Double.valueOf(singleArrayItem[0]);
        positionSignal[1] = Double.valueOf(singleArrayItem[1]);
        jsonMan.setShouldBeSpoofedInImportedLoc(positionSignal,singleArrayItem[2], spoofingStatus);
        jsonMan = JSONManager.getInstanceJSONManager();//new JSONManager(nextMain);
        for(String[] listitem : data){
            if(positionSignal[0] == Double.valueOf(listitem[0]) && positionSignal[1] == Double.valueOf(listitem[1]) && singleArrayItem[2].equals(listitem[2])){
                listitem[4] = String.valueOf(spoofingStatus);
            }
        }
        stored_adapter.notifyDataSetChanged();
    }
}
