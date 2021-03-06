package at.ac.fhstp.sonicontrol.detetion_fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
//import android.support.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
//import androidx.core.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

import java.io.File;
import java.util.ArrayList;

import at.ac.fhstp.sonicontrol.GPSTracker;
import at.ac.fhstp.sonicontrol.JSONManager;
import at.ac.fhstp.sonicontrol.MainActivity;
import at.ac.fhstp.sonicontrol.R;

public class RulesOnMapFragment extends Fragment implements MapEventsReceiver {
    MapView map = null;
    ImageButton infoBtn;
    AlertDialog alertMapInfo = null;
    RadiusMarkerClusterer detectionMarkers;
    FolderOverlay circleMarker;
    Polygon lastCircle;
    GPSTracker locationData;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        Context ctx = getActivity();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setOsmdroidBasePath(new File(ctx.getCacheDir().getAbsolutePath(), "osmdroid"));
        Configuration.getInstance().setOsmdroidTileCache(new File(Configuration.getInstance().getOsmdroidBasePath().getAbsolutePath(), "tile"));

        View rootView = inflater.inflate(R.layout.rules_on_map_fragment, container, false);

        map = (MapView) rootView.findViewById(R.id.map);
        final ITileSource tileSource = TileSourceFactory.MAPNIK;
        map.setTileSource(tileSource);
        map.setUseDataConnection(true);
        map.setVerticalMapRepetitionEnabled(false);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        IMapController mapController = map.getController();
        mapController.setZoom(10);
        GeoPoint startPoint = null;
        int status = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        if(status == 0) {
            if (locationData == null) {
                locationData = new GPSTracker(getActivity());
            }
            startPoint = new GeoPoint(locationData.getLatitude(), locationData.getLongitude());
        }else{
            startPoint = new GeoPoint(48.212602, 15.6352079); //Default BIZ St. Pölten
        }

        mapController.setCenter(startPoint);

        circleMarker = new FolderOverlay(getActivity());
        map.getOverlays().add(circleMarker);

        detectionMarkers = new RadiusMarkerClusterer(getActivity());
        map.getOverlays().add(detectionMarkers);
        Drawable clusterIconD = getResources().getDrawable(R.drawable.cluster_circle);
        Bitmap clusterIcon = ((BitmapDrawable)clusterIconD).getBitmap();
        detectionMarkers.setIcon(clusterIcon);

        addMapMarkers();

        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(getActivity(), this);
        map.getOverlays().add(0, mapEventsOverlay);

        infoBtn = (ImageButton) rootView.findViewById(R.id.infoBtn);

        infoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMapLegend();
            }
        });

        //just change the fragment_dashboard
        //with the fragment you want to inflate
        //like if the class is HomeFragment it should have R.layout.home_fragment
        //if it is DashboardFragment it should have R.layout.fragment_dashboard
        return rootView;
    }

    public void openMapLegend(){
        final AlertDialog.Builder activateMapInfoDialog = new AlertDialog.Builder(getContext());
        LayoutInflater mapInfoAlertInflater = LayoutInflater.from(getContext());
        final View alertMapInfoView = mapInfoAlertInflater.inflate(R.layout.map_info, null);
        activateMapInfoDialog.setView(alertMapInfoView);
        activateMapInfoDialog
                .setTitle(getString(R.string.map_dialog_legend_title))
                .setCancelable(true)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                   alertMapInfo.cancel();
                    }
                });
        alertMapInfo = activateMapInfoDialog.show();
    }

    public void addMapMarkers(){
        MainActivity main = new MainActivity();
        MainActivity nextMain = main.getMainIsMain();
        JSONManager jsonMan = JSONManager.getInstanceJSONManager();//new JSONManager(nextMain);
        ArrayList<String[]> locationData = jsonMan.getJsonData();
        locationData.addAll(jsonMan.getImportJsonData());

        ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();

        for(final String[] sArray : locationData){
            Marker marker = new Marker(map);
            marker.setPosition(new GeoPoint(Double.valueOf(sArray[1]),Double.valueOf(sArray[0])));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            switch (sArray[4]){
                case "0":
                    marker.setIcon(getResources().getDrawable(R.drawable.baseline_place_green_48));
                    break;
                case "1":
                    marker.setIcon(getResources().getDrawable(R.drawable.baseline_place_red_48));
                    break;
                case "2":
                    marker.setIcon(getResources().getDrawable(R.drawable.baseline_place_green_48));
                    break;
                case "3":
                    marker.setIcon(getResources().getDrawable(R.drawable.baseline_place_red_48));
                    break;
                case "4":
                    marker.setIcon(getResources().getDrawable(R.drawable.baseline_place_orange_48));
                    break;
                default:
                    marker.setIcon(getResources().getDrawable(R.drawable.baseline_place_orange_48));
                    break;
            }
            marker.setTitle(sArray[2]);
            marker.setSubDescription(getActivity().getString(R.string.map_marker_description_last_detection) +sArray[3]);
            marker.setRelatedObject(sArray[4]);
            MarkerInfoWindow infoWindow = new MarkerInfoWindow(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, map);
            marker.setInfoWindow(infoWindow);
            marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker, MapView mapView) {
                    MarkerInfoWindow.closeAllInfoWindowsOn(map);
                    marker.showInfoWindow();
                    Polygon circle = new Polygon(map);
                    double radius = Double.valueOf(sArray[9]) * 1000.0 * 20.0;
                    circle.setPoints(Polygon.pointsAsCircle(marker.getPosition(), radius));
                    switch ((String)marker.getRelatedObject()){
                        case "0":
                            circle.setFillColor(0x3300FF00);
                            circle.setStrokeColor(Color.GREEN);
                            break;
                        case "1":
                            circle.setFillColor(0x33FF0000);
                            circle.setStrokeColor(Color.RED);
                            break;
                        case "2":
                            circle.setFillColor(0x3300FF00);
                            circle.setStrokeColor(Color.GREEN);
                            break;
                        case "3":
                            circle.setFillColor(0x33FF0000);
                            circle.setStrokeColor(Color.RED);
                            break;
                        case "4":
                            circle.setFillColor(0x33FF6D00);
                            circle.setStrokeColor(0xFFFF6D00);
                            break;
                        default:
                            circle.setFillColor(0x33FF6D00);
                            circle.setStrokeColor(0xFFFF6D00);
                            break;
                    }
                    circle.setStrokeWidth(7);
                    circle.setInfoWindow(null);
                    if(lastCircle!=null) {
                        circleMarker.remove(lastCircle);
                        map.invalidate();
                    }
                    circleMarker.add(circle);
                    map.invalidate();
                    lastCircle = circle;
                    Log.d("RulesOnMap", "onClickMarker");
                    return true;
                }
            });
            detectionMarkers.add(marker);
        }
    }

    public void onResume(){
        super.onResume();
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    public void onPause(){
        super.onPause();
    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        MarkerInfoWindow.closeAllInfoWindowsOn(map);
        InfoWindow.closeAllInfoWindowsOn(map);
        if(lastCircle!=null) {
            circleMarker.remove(lastCircle);
            map.invalidate();
        }
        return true;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        return false;
    }
}
