package com.example.culturespot;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.Algorithm;

import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.model.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapFragment extends Fragment {
    private MapFragment mapFragment;
    private GoogleMap googleMap;
    private MySearchView search, search2;
    private ListView list, list2;
    private View v;
    private Polyline line;
    private ClusterManager<MarkerItem> clusterManager;
    private Connection con;
    private ArrayList<String> listSuggestions, listSuggestions2, allSugestions;
    private ArrayAdapter<String> suggestionAdapter, suggestionAdapter2;
    private boolean[] checkedFilters, previousFilters, listFlags;
    private boolean directions;
    private FloatingActionButton filters;
    private String[] listFilters;
    private AlertDialog dialog;
    LocationManager mLocationManager;
    Location location;
    ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private final String URL="GRAPHDB_URL";
    FavoritesFragment favoritesFragment;
    View favoritesView;
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mapFragment = this;
    }
    void setFavoritesFragment(FavoritesFragment favoritesFragment,View favoritesView){
        this.favoritesFragment=favoritesFragment;
        this.favoritesView=favoritesView;
    }
    private OnMapReadyCallback callback = new OnMapReadyCallback() {
        //setup list adapters and initiliaze map,cluster ,markers etc
        public void onMapReady(GoogleMap googleMap1) {
            googleMap = googleMap1;
            directions = false;
            mAuth = FirebaseAuth.getInstance();
            googleMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
                @Override
                public void onCameraMoveStarted(int i) {
                    clearKeyboard(search);
                    clearKeyboard(search2);
                }
            });
            googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    clearKeyboard(search);
                    clearKeyboard(search2);
                }
            });
          //get permission
                 if (ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                } else {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                }
            }
            mLocationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    mapFragment.location = location;
                }
            });
            clusterManager = new ClusterManager<MarkerItem>(mapFragment.getContext(), googleMap);
            clusterManager.getMarkerCollection().setOnInfoWindowAdapter(new MyInfoWindow(LayoutInflater.from(getContext()), mapFragment));
            googleMap.setInfoWindowAdapter(clusterManager.getMarkerManager());
            clusterManager.setOnClusterItemInfoWindowClickListener(new ClusterManager.OnClusterItemInfoWindowClickListener<MarkerItem>() {
                private void setHeightZero(TextView tv){
                        ViewGroup.LayoutParams params = tv.getLayoutParams();
                        params.height=0;
                        tv.setLayoutParams(params);
                }
                private void setRedirect(TextView tv,String text){
                    tv.setText(Html.fromHtml(text));
                    tv.setMovementMethod(LinkMovementMethod.getInstance());
                }
                @Override
                public void onClusterItemInfoWindowClick(MarkerItem m) { // show popu window when clicking the info window , so we can clickable views
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
                    final View customLayout = getLayoutInflater().inflate(R.layout.place_details, null);
                    ((TextView) customLayout.findViewById(R.id.title)).setText(m.getTitle());

                    if (!m.getEmail().equals("")) setRedirect(customLayout.findViewById(R.id.email),"<a href='" + m.getEmail() + "'>Email<a/>");
                    else setHeightZero(customLayout.findViewById(R.id.email));
                    if (!m.getHours().equals(""))
                        ((TextView) customLayout.findViewById(R.id.hours)).setText("Opening Hours : " + m.getHours());
                    else setHeightZero(customLayout.findViewById(R.id.hours));
                    if (!m.getPhone().equals("")) {
                        String[] phones = m.getPhone().split(" , ");
                        String phoneText = "";
                        if (phones.length == 1)
                            phoneText += "<a href=\"tel:" + m.getPhone() + "\">Phone Number</a>"; // redirect to app
                        else {
                            for (int i = 0; i < phones.length; i++) {
                                if (i == 0)
                                    phoneText += "<a href=\"tel:" + phones[i] + "\">Phone Number " + i + "</a>"; // redirect to app
                                else
                                    phoneText += " , <a href=\"tel:" + phones[i] + "\">Phone Number " + i + "</a>"; // redirect to app
                            }
                        }
                        setRedirect(customLayout.findViewById(R.id.phone),phoneText);
                    }
                    else setHeightZero(customLayout.findViewById(R.id.phone));
                    if (!m.getWeb().equals("")) setRedirect(customLayout.findViewById(R.id.web),"<a href='" + m.getWeb() + "'>Website<a/>"); // redirect to app
                    else setHeightZero(customLayout.findViewById(R.id.web));
                    if (!m.getWiki().equals("")) setRedirect(customLayout.findViewById(R.id.wiki),"<a href='" + m.getWiki() + "'>Wikipedia<a/>"); // redirect to app
                    else setHeightZero(customLayout.findViewById(R.id.wiki));
                    alertDialog.setView(customLayout);
                    AlertDialog alert = alertDialog.create();
                    FirebaseUser user = mAuth.getCurrentUser();
                    DatabaseReference reference= FirebaseDatabase.getInstance().getReference("Users");
                    String userId=user.getUid();
                    reference.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            User userProfile1=snapshot.getValue(User.class);
                            if(userProfile1!=null){

                                if(!(userProfile1.favorites==null || !userProfile1.favorites.containsKey(m.getId()))){ // is favorite
                                    customLayout.findViewById(R.id.favorite).setBackgroundResource(R.drawable.ic_baseline_favorite_24);
                                }
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(getActivity(),"There was an error , try again!",Toast.LENGTH_LONG).show();
                        }
                    });
                    customLayout.findViewById(R.id.favorite).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            DatabaseReference reference= FirebaseDatabase.getInstance().getReference("Users");
                            String userId=user.getUid();
                            reference.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    User userProfile1=snapshot.getValue(User.class);
                                    if(userProfile1!=null){
                                        String first_name=userProfile1.first_name;
                                        String email=userProfile1.email;
                                        String surname=userProfile1.surname;
                                        String username=userProfile1.username;
                                        User userProfile=userProfile1.favorites==null ?new User(username,email,first_name,surname) : userProfile1;
                                        final boolean flag=!(userProfile1.favorites==null || !userProfile1.favorites.containsKey(m.getId()));
                                        if(!(userProfile1.favorites==null || !userProfile1.favorites.containsKey(m.getId()))){
                                            customLayout.findViewById(R.id.favorite).setBackgroundResource(R.drawable.ic_baseline_favorite_border_24); // is not favorite
                                            userProfile.removeFavorite(m.getId());
                                        }
                                        else {
                                            customLayout.findViewById(R.id.favorite).setBackgroundResource(R.drawable.ic_baseline_favorite_24); // is favorite
                                            userProfile.addFavorite(m.getId(),m.getTitle());
                                        }
                                        reference.child(userId).setValue(userProfile).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                try {
                                                    if(flag)FavoritesList.remove(new FavoritesList.Item(m.getId(),m.getTitle())); // remove favorite from list
                                                    else FavoritesList.ITEMS.add(new FavoritesList.Item(m.getId(),m.getTitle())); // add favorite from list
                                                    favoritesFragment.reset(favoritesView);
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                                    }
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(getActivity(),"There was an error , try again!",Toast.LENGTH_LONG).show();
                                }
                            });

                        }
                    });
                    customLayout.findViewById(R.id.nearby).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) { // show nearby places depending on km radius
                            AlertDialog.Builder alertDialog1 = new AlertDialog.Builder(getContext());
                            final View customLayout1 = getLayoutInflater().inflate(R.layout.distance, null);
                            alertDialog1.setView(customLayout1);
                            alertDialog1.setTitle("Kms Circle around this places of interest");
                            alertDialog1.setPositiveButton("Show Nearby Places", null);
                            alertDialog1.setNegativeButton("Dismiss", null);
                            AlertDialog alert1 = alertDialog1.create();
                            alert1.show();
                            alert1.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    return;
                                }
                            });
                            alert1.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    alert1.dismiss();
                                }
                            });
                            alert1.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                                @Override

                                public void onClick(View v) {
                                    String str="PREFIX geo: <http://www.opengis.net/ont/geosparql#>\n" +
                                            "PREFIX omgeo: <http://www.ontotext.com/owlim/geo#>\n"+
                                            "PREFIX geof: <http://www.opengis.net/def/function/geosparql/>\n" +
                                            "PREFIX : <http://www.ontotext.com/plugins/geosparql#>\n" +
                                            "prefix onto: <http://ai.di.uoa.gr/OSM/ontology#>\n" +
                                            "prefix rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                                            "SELECT ?id ?name ?web ?email ?phone ?hours ?type ?lon ?lat ?wiki\n" +
                                            "WHERE {\n" +
                                            "     ?place onto:hasNameEn ?name.\n" +
                                            "     OPTIONAL{?place onto:hasContactWebsite ?web}.\n" +
                                            "     OPTIONAL{?place onto:hasWebsite ?web}.\n" +
                                            "     OPTIONAL{?place onto:hasEmail ?email}.\n" +
                                            "     OPTIONAL{?place onto:hasContactEmail ?email}.\n" +
                                            "     OPTIONAL{?place onto:hasPhone ?phone}.\n" +
                                            "     OPTIONAL{?place onto:hasContactPhone ?phone}.\n" +
                                            "     OPTIONAL{?place onto:hasOpening_hours ?hours}.\n" +
                                            "     OPTIONAL{?place onto:hasWikipedia ?wiki}.\n" +
                                            "     ?place rdf:type ?type.\n" +
                                            "     ?place onto:hasId ?id.\n" +
                                            "     ?place onto:hasLatitude ?lat.\n" +
                                            "     ?place onto:hasLongitude ?lon.\n" +
                                            "    FILTER (?type IN (" + getCategories() + "))\n"+
                                            "FILTER ( omgeo:distance("+m.getPosition().latitude+"," +m.getPosition().longitude +",?lat, ?lon) <"+((EditText)customLayout1.findViewById(R.id.number)).getText().toString() +" ) \n";
                                    str += mapStr();
                                    str += "}";
                                    reset(str); //query GraphDb
                                    alert1.dismiss();
                                    alert.dismiss();
                                }
                            });

                        }
                    });
                    customLayout.findViewById(R.id.show).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) { // setup map for route directions
                            search2.setVisibility(View.VISIBLE);
                            list2.setVisibility(View.VISIBLE);
                            search2.setIconified(false);
                            String str1="\""+((TextView) customLayout.findViewById(R.id.title)).getText().toString()+"\"";
                            search2.setQuery(((TextView) customLayout.findViewById(R.id.title)).getText().toString(), false);
                            search2.clearFocus();
                            directions = true;
                            ((BottomNavigationActivity)getActivity()).setBackButton(true);
                            search.setQuery("My Location", false);search.setIconified(false);
                            listFlags[1] = true;
                            listFlags[0] = true;
                            alert.dismiss();
                            con = new Connection(URL, mapFragment);
                            String str = "PREFIX geo: <http://www.opengis.net/ont/geosparql#>\n" +
                                    "PREFIX geof: <http://www.opengis.net/def/function/geosparql/>\n" +
                                    "PREFIX : <http://www.ontotext.com/plugins/geosparql#>\n" +
                                    "prefix onto: <http://ai.di.uoa.gr/OSM/ontology#>\n" +
                                    "prefix rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                                    "SELECT ?id ?name ?web ?email ?phone ?hours ?type ?lon ?lat ?wiki\n" +
                                    "WHERE {\n" +
                                    "     ?place onto:hasNameEn ?name.\n" +
                                    "    FILTER (?name IN (" + str1 + "))\n"+
                                    "     OPTIONAL{?place onto:hasContactWebsite ?web}.\n" +
                                    "     OPTIONAL{?place onto:hasWebsite ?web}.\n" +
                                    "     OPTIONAL{?place onto:hasEmail ?email}.\n" +
                                    "     OPTIONAL{?place onto:hasContactEmail ?email}.\n" +
                                    "     OPTIONAL{?place onto:hasPhone ?phone}.\n" +
                                    "     OPTIONAL{?place onto:hasContactPhone ?phone}.\n" +
                                    "     OPTIONAL{?place onto:hasOpening_hours ?hours}.\n" +
                                    "     OPTIONAL{?place onto:hasWikipedia ?wiki}.\n" +
                                    "     ?place rdf:type ?type.\n" +
                                    "     ?place onto:hasId ?id.\n" +
                                    "     ?place onto:hasLatitude ?lat.\n" +
                                    "     ?place onto:hasLongitude ?lon.\n" +
                                    "}";
                            progressBar.setVisibility(View.VISIBLE);
                            con.execute(str, "map2"); //query GraphDb
                            listSuggestions.clear(); //clear list
                            suggestionAdapter.notifyDataSetChanged();
                        }
                    });
                    alert.show();
                }
            });
            clusterManager
                    .setOnClusterClickListener(new ClusterManager.OnClusterClickListener<MarkerItem>() {
                        @Override
                        public boolean onClusterClick(Cluster<MarkerItem> cluster) { //zoom whenever clicking the cluster
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                    cluster.getPosition(), (float) Math.floor(googleMap
                                            .getCameraPosition().zoom + 2)), 300,
                                    null);
                            return true;
                        }
                    });
            googleMap.setOnCameraIdleListener(clusterManager);
            googleMap.setOnMarkerClickListener(clusterManager);
            googleMap.setOnInfoWindowClickListener(clusterManager);
            con = new Connection(URL, mapFragment);
            con.execute("PREFIX geo: <http://www.opengis.net/ont/geosparql#>\n" +
                    "PREFIX geof: <http://www.opengis.net/def/function/geosparql/>\n" +
                    "PREFIX : <http://www.ontotext.com/plugins/geosparql#>\n" +
                    "prefix onto: <http://ai.di.uoa.gr/OSM/ontology#>\n" +
                    "prefix rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                    "SELECT ?name \n" +
                    "WHERE {\n" +
                    "     ?place onto:hasNameEn ?name.\n" +
                    "}", "autocomplete"
            ); // view all names for autocomplete
            con = new Connection(URL, mapFragment);
            progressBar.setVisibility(View.VISIBLE);
            con.execute("PREFIX geo: <http://www.opengis.net/ont/geosparql#>\n" +
                    "PREFIX geof: <http://www.opengis.net/def/function/geosparql/>\n" +
                    "PREFIX : <http://www.ontotext.com/plugins/geosparql#>\n" +
                    "prefix onto: <http://ai.di.uoa.gr/OSM/ontology#>\n" +
                    "prefix rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                    "SELECT ?id ?name ?web ?email ?phone ?hours ?type ?lon ?lat ?wiki\n" +
                    "WHERE {\n" +
                    "     ?place onto:hasNameEn ?name.\n" +
                    "     OPTIONAL{?place onto:hasContactWebsite ?web}.\n" +
                    "     OPTIONAL{?place onto:hasWebsite ?web}.\n" +
                    "     OPTIONAL{?place onto:hasEmail ?email}.\n" +
                    "     OPTIONAL{?place onto:hasContactEmail ?email}.\n" +
                    "     OPTIONAL{?place onto:hasPhone ?phone}.\n" +
                    "     OPTIONAL{?place onto:hasContactPhone ?phone}.\n" +
                    "     OPTIONAL{?place onto:hasOpening_hours ?hours}.\n" +
                    "     OPTIONAL{?place onto:hasWikipedia ?wiki}.\n" +
                    "     ?place rdf:type ?type.\n" +
                    "     ?place onto:hasId ?id.\n" +
                    "     ?place onto:hasLatitude ?lat.\n" +
                    "     ?place onto:hasLongitude ?lon.\n" +
                    "}", "map");
        } // initialize map and query GraphDb
    };
    public void reset(String query) { // go from route directions on map to showing all points of interest
        search2.setVisibility(View.INVISIBLE);
        list2.setVisibility(View.INVISIBLE);
        search.clearFocus();
        directions = false;
        ((BottomNavigationActivity)getActivity()).setBackButton(false);
        if(line!=null) line.remove();
        line=null;
        con = new Connection(URL, mapFragment);
        progressBar.setVisibility(View.VISIBLE);
        con.execute(query, "map");
        search.setQuery("", false);
        listFlags[1] = false;
        listFlags[0] = false;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_map, container, false);
        listFilters = getResources().getStringArray(R.array.filters);
        checkedFilters = new boolean[listFilters.length];
        previousFilters = new boolean[listFilters.length];
        listFlags = new boolean[2];
        line=null;
        progressBar=(ProgressBar) v.findViewById(R.id.progressBar);
        filters = (FloatingActionButton) v.findViewById(R.id.filters);
        for (int i = 0; i < checkedFilters.length; i++) { // initiliaze filters
            checkedFilters[i] = true;
            previousFilters[i] = true;
        }
        filters.setOnClickListener(new View.OnClickListener() { // show filters
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                clearKeyboard(search);
                clearKeyboard(search2);
                setHeightListView(true, list);
                setHeightListView(true, list2);
                builder.setTitle("Filters");
                builder.setMultiChoiceItems(listFilters, checkedFilters, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) { // one of the filters was changed
                        AlertDialog d = (AlertDialog) dialog;
                        ListView v = d.getListView();
                        if (isChecked) {
                            if (which == 1) { // all categories was marked so mark all categories
                                for (int i = which + 1; i < checkedFilters.length; i++) {
                                    v.setItemChecked(i, true);
                                    checkedFilters[i] = true;
                                }
                            }
                        } else {
                            if (which == 1) { // all categories was unmarked so unmark all categories
                                for (int i = which + 1; i < checkedFilters.length; i++) {
                                    v.setItemChecked(i, false);
                                    checkedFilters[i] = false;
                                }
                            }
                        }
                    }
                });
                builder.setPositiveButton("Apply Changes", null);
                builder.setNegativeButton("Dismiss", null);
                builder.setNeutralButton("Reset", null);
                dialog = builder.create();
                dialog.show();
                AlertDialog dialog1 = dialog;
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        AlertDialog d = (AlertDialog) dialog;
                        ListView v = d.getListView();
                        for (int i = 0; i < checkedFilters.length; i++) { // reset filters to previous values
                            checkedFilters[i] = previousFilters[i];
                            v.setItemChecked(i, previousFilters[i]);
                        }
                    }
                });
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog d = (AlertDialog) dialog;
                        d.dismiss();
                        ListView v1 = d.getListView();
                        for (int i = 0; i < checkedFilters.length; i++) { // reset filters to previous values
                            checkedFilters[i] = previousFilters[i];
                            v1.setItemChecked(i, previousFilters[i]);
                        }
                    }
                });
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkFilters(dialog);
                    }
                });
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog d = (AlertDialog) dialog;
                        ListView v1 = d.getListView();
                        for (int i = 0; i < checkedFilters.length; i++) { // reset filters to initialized values
                            checkedFilters[i] = true;
                            previousFilters[i] = true;
                            v1.setItemChecked(i, true);
                        }

                    }
                });
            }
        });
        search = (MySearchView) v.findViewById(R.id.search);
        search2 = (MySearchView) v.findViewById(R.id.search2);
        list = (ListView) v.findViewById(R.id.list);
        list2 = (ListView) v.findViewById(R.id.list2);
        listSuggestions = new ArrayList<String>();
        listSuggestions2 = new ArrayList<String>();
        allSugestions = new ArrayList<String>();
        suggestionAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, listSuggestions);
        suggestionAdapter2 = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, listSuggestions2);
        list.setAdapter(suggestionAdapter);
        list2.setAdapter(suggestionAdapter2);
        list2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String str1=listSuggestions2.get(position)+"\",\""+search.getQuery().toString()+"\"";
                search2.setQuery(listSuggestions2.get(position), false);
                listFlags[1] = true;
                if(listFlags[0]){ // find route between two new  points
                    con = new Connection(URL, mapFragment);
                    String str = "PREFIX geo: <http://www.opengis.net/ont/geosparql#>\n" +
                            "PREFIX geof: <http://www.opengis.net/def/function/geosparql/>\n" +
                            "PREFIX : <http://www.ontotext.com/plugins/geosparql#>\n" +
                            "prefix onto: <http://ai.di.uoa.gr/OSM/ontology#>\n" +
                            "prefix rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                            "SELECT ?id ?name ?web ?email ?phone ?hours ?type ?lon ?lat ?wiki\n" +
                            "WHERE {\n" +
                            "     ?place onto:hasNameEn ?name.\n" +
                            "    FILTER (?name IN (" + "\""+str1+ "))\n"+
                            "     OPTIONAL{?place onto:hasContactWebsite ?web}.\n" +
                            "     OPTIONAL{?place onto:hasWebsite ?web}.\n" +
                            "     OPTIONAL{?place onto:hasEmail ?email}.\n" +
                            "     OPTIONAL{?place onto:hasContactEmail ?email}.\n" +
                            "     OPTIONAL{?place onto:hasPhone ?phone}.\n" +
                            "     OPTIONAL{?place onto:hasContactPhone ?phone}.\n" +
                            "     OPTIONAL{?place onto:hasOpening_hours ?hours}.\n" +
                            "     OPTIONAL{?place onto:hasWikipedia ?wiki}.\n" +
                            "     ?place rdf:type ?type.\n" +
                            "     ?place onto:hasId ?id.\n" +
                            "     ?place onto:hasLatitude ?lat.\n" +
                            "     ?place onto:hasLongitude ?lon.\n" +
                            "}";
                    progressBar.setVisibility(View.VISIBLE);
                    con.execute(str, "map2");
                    setHeightListView(true, list2);
                    clearKeyboard(search2);
                    listSuggestions2.clear(); // clear list suggestins
                    suggestionAdapter2.notifyDataSetChanged();
                }
                return;
            }
        });
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String str1 =null;
                if (directions) { // set text and clear list
                    str1="\""+listSuggestions.get(position)+"\",\""+search2.getQuery().toString()+"\"";
                    search.setQuery(listSuggestions.get(position), false);
                    setHeightListView(true, list);
                    clearKeyboard(search);
                    listFlags[0] = true;
                    if(!listFlags[1]) return;
                }
                else { // setup map for route directions
                    search2.setVisibility(View.VISIBLE);
                    list2.setVisibility(View.VISIBLE);
                    search2.setIconified(false);
                    str1="\""+listSuggestions.get(position)+"\"";
                    search2.setQuery(listSuggestions.get(position), false);
                    search2.clearFocus();
                    directions = true;
                    ((BottomNavigationActivity)getActivity()).setBackButton(true);
                    search.setQuery("My Location", false);
                    listFlags[1] = true;
                    listFlags[0] = true;
                }
                con = new Connection(URL, mapFragment);
                String str = "PREFIX geo: <http://www.opengis.net/ont/geosparql#>\n" +
                        "PREFIX geof: <http://www.opengis.net/def/function/geosparql/>\n" +
                        "PREFIX : <http://www.ontotext.com/plugins/geosparql#>\n" +
                        "prefix onto: <http://ai.di.uoa.gr/OSM/ontology#>\n" +
                        "prefix rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "SELECT ?id ?name ?web ?email ?phone ?hours ?type ?lon ?lat ?wiki\n" +
                        "WHERE {\n" +
                        "     ?place onto:hasNameEn ?name.\n" +
                        "    FILTER (?name IN (" + str1 + "))\n"+
                        "     OPTIONAL{?place onto:hasContactWebsite ?web}.\n" +
                        "     OPTIONAL{?place onto:hasWebsite ?web}.\n" +
                        "     OPTIONAL{?place onto:hasEmail ?email}.\n" +
                        "     OPTIONAL{?place onto:hasContactEmail ?email}.\n" +
                        "     OPTIONAL{?place onto:hasPhone ?phone}.\n" +
                        "     OPTIONAL{?place onto:hasContactPhone ?phone}.\n" +
                        "     OPTIONAL{?place onto:hasOpening_hours ?hours}.\n" +
                        "     OPTIONAL{?place onto:hasWikipedia ?wiki}.\n" +
                        "     ?place rdf:type ?type.\n" +
                        "     ?place onto:hasId ?id.\n" +
                        "     ?place onto:hasLatitude ?lat.\n" +
                        "     ?place onto:hasLongitude ?lon.\n" +
                        "}";
                progressBar.setVisibility(View.VISIBLE);
                con.execute(str, "map2"); // query GraphDb
                listSuggestions.clear();
                suggestionAdapter.notifyDataSetChanged();
            }
        });

        setSearch(search, list, 0, listSuggestions, suggestionAdapter);
        setSearch(search2, list2, 1, listSuggestions2, suggestionAdapter2); // set text for search boxes
        return v;
    }

    void setSearch(MySearchView search, ListView list, int i, ArrayList<String> listSuggestions, ArrayAdapter<String> suggestionAdapter) {
        search.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                setHeightListView(!hasFocus, list);
            }
        });
        search.setOnQueryTextListener(new MySearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { // on search button pressed
                if (!directions) {
                    con = new Connection(URL, mapFragment);
                    String str = "PREFIX geo: <http://www.opengis.net/ont/geosparql#>\n" +
                            "PREFIX geof: <http://www.opengis.net/def/function/geosparql/>\n" +
                            "PREFIX : <http://www.ontotext.com/plugins/geosparql#>\n" +
                            "prefix onto: <http://ai.di.uoa.gr/OSM/ontology#>\n" +
                            "prefix rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                            "SELECT ?id ?name ?web ?email ?phone ?hours ?type ?lon ?lat ?wiki\n" +
                            "WHERE {\n" +
                            "     ?place onto:hasNameEn ?name.\n" +
                            "     OPTIONAL{?place onto:hasContactWebsite ?web}.\n" +
                            "     OPTIONAL{?place onto:hasWebsite ?web}.\n" +
                            "     OPTIONAL{?place onto:hasEmail ?email}.\n" +
                            "     OPTIONAL{?place onto:hasContactEmail ?email}.\n" +
                            "     OPTIONAL{?place onto:hasPhone ?phone}.\n" +
                            "     OPTIONAL{?place onto:hasContactPhone ?phone}.\n" +
                            "     OPTIONAL{?place onto:hasOpening_hours ?hours}.\n" +
                            "     OPTIONAL{?place onto:hasWikipedia ?wiki}.\n" +
                            "     ?place rdf:type ?type.\n" +
                            "     ?place onto:hasId ?id.\n" +
                            "     ?place onto:hasLatitude ?lat.\n" +
                            "   FILTER regex(str(?name), \"" + query + "\",\"i\").\n" +
                            "     ?place onto:hasLongitude ?lon.\n" +
                            "    FILTER (?type IN (" + getCategories() + "))\n";
                    str += mapStr();
                    str += "}";
                    progressBar.setVisibility(View.VISIBLE);
                    con.execute(str, "map"); // query graphdb
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String text) { // whenever text on search box ix changed
                listFlags[i] = false;
                if (text.isEmpty()) { // if text is empty clear list
                    listSuggestions.clear();
                    setHeightListView(true, list);
                    clearKeyboard(search);
                    suggestionAdapter.notifyDataSetChanged();
                    return false;
                } else { // find suggestions from all names
                    listSuggestions.clear();
                    text = text.toLowerCase();
                    for (String str : allSugestions) {
                        String str2 = str.toLowerCase();
                        if (str2.contains(text)) listSuggestions.add(str);
                    }
                    String str = "my location";
                    if (directions) if (str.contains(text)) listSuggestions.add("My Location");
                    Collections.sort(listSuggestions);
                    suggestionAdapter.notifyDataSetChanged();
                    setHeightListView(false, list);
                }
                return false;
            }


        });
    }

    public void checkFilters(DialogInterface dialog) { // check filters if they are ok(at least one category must be chosen)
        boolean flag = false;
        for (int i = 1; i < checkedFilters.length; i++) {
            if (checkedFilters[i] == true) {
                flag = true;
                break;
            }
        }
        if (!flag)
            Toast.makeText(getContext(), "You have to select at least one category !", Toast.LENGTH_LONG).show();
        else {
            for (int i = 0; i < checkedFilters.length; i++) {
                previousFilters[i] = checkedFilters[i];
                dialog.dismiss();
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
    }

    public String getCategories() {
        String s = "";
        boolean flag = false;
        for (int i = 2; i < checkedFilters.length; i++) {
            if (checkedFilters[i]) {
                if (!flag) {
                    s += "onto:" + listFilters[i].substring(0, listFilters[i].length() - 1).toLowerCase();
                    flag = true;
                } else
                    s += ",onto:" + listFilters[i].substring(0, listFilters[i].length() - 1).toLowerCase();
            }
        }
        return s; // setup cateogories string for query
    }

    public void clearKeyboard(MySearchView search) {

        InputMethodManager inputMethodManager =
                (InputMethodManager) getActivity().getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        search.clearFocus();
        if (getActivity().getCurrentFocus() == null) return;
        inputMethodManager.hideSoftInputFromWindow(
                getActivity().getCurrentFocus().getWindowToken(), 0);

    }
    public void getResults(String flag, HashMap<Value, HashMap<String, Value>> results) { // get results for connection thread
        if (!flag.contains("autocomplete"))progressBar.setVisibility(View.GONE);
        if (flag.contains("map")) {
            clusterManager.clearItems();
            clusterManager.cluster();
            int i=0;
            if(search.getQuery().toString().equals("My Location") || search2.getQuery().toString().equals("My Location")) i++; // my location was in one of the search boxes
            LatLng latLng2 =null;
            for (Map.Entry<Value, HashMap<String, Value>> resultEntry : results.entrySet()) { // get all data available
                HashMap<String, Value> node = resultEntry.getValue();
                String id=resultEntry.getKey().toString();
                id = id.substring(id.indexOf("\"") + 1, id.lastIndexOf("\"")).replace("/","");
                String lon = node.get("lon").toString();
                lon = lon.substring(lon.indexOf("\"") + 1, lon.lastIndexOf("\""));
                String lat = node.get("lat").toString();
                lat = lat.substring(lat.indexOf("\"") + 1, lat.lastIndexOf("\""));
                String name = node.get("name").toString();
                name = name.substring(name.indexOf("\"") + 1, name.lastIndexOf("\""));
                while(name.endsWith(" "))  name = name.substring(0, name.length() - 1);
                while(name.startsWith(" ")) name = name.substring(1, name.length());
                String hours = "";
                if (node.get("hours") != null) {
                    hours = node.get("hours").toString();
                    hours = hours.substring(hours.indexOf("\"") + 1, hours.lastIndexOf("\""));
                }
                String email = "";
                if (node.get("email") != null) {
                    email = node.get("email").toString();
                    email = email.substring(email.indexOf("\"") + 1, email.lastIndexOf("\""));
                }
                String phone = "";
                if (node.get("phone") != null) {
                    phone = node.get("phone").toString();
                    phone = phone.substring(phone.indexOf("\"") + 1, phone.lastIndexOf("\"")).replace(";", " , ");
                }
                String web = "";
                if (node.get("web") != null) {
                    web = node.get("web").toString();
                    web = web.substring(web.indexOf("\"") + 1, web.lastIndexOf("\""));
                }
                String wiki = "";
                if (node.get("wiki") != null) {
                    wiki = node.get("wiki").toString();
                    if(wiki.contains("www.w3.org")){
                        wiki = wiki.substring(wiki.indexOf("\"") + 1, wiki.lastIndexOf("\""));
                        wiki="https://"+wiki.substring(0,2)+".wikipedia.org/wiki/"+wiki.substring(3,wiki.length()); // setup wiki page
                    }
                }
                LatLng latLng = new LatLng(Double.parseDouble(lat), Double.parseDouble(lon));
                if ((!directions) || (i <= 1 && directions && (name.equals(search.getQuery().toString()) || name.equals(search2.getQuery().toString())))) { // add to cluster
                    if (name.equals(search.getQuery().toString())) {
                        i++;
                        location.setLatitude(Double.parseDouble(lat));
                        location.setLongitude(Double.parseDouble(lon));
                        clusterManager.addItem(new MarkerItem(id,latLng, name, resultEntry.getKey().toString().substring(resultEntry.getKey().toString().indexOf("\"") + 1, resultEntry.getKey().toString().lastIndexOf("\"")), phone, email, web, hours, wiki));
                    }
                    if (name.equals(search2.getQuery().toString())) {
                        i++;
                        latLng2 = new LatLng(Double.parseDouble(lat), Double.parseDouble(lon));
                        clusterManager.addItem(new MarkerItem(id,latLng, name, resultEntry.getKey().toString().substring(resultEntry.getKey().toString().indexOf("\"") + 1, resultEntry.getKey().toString().lastIndexOf("\"")), phone, email, web, hours, wiki));
                    }
                    if(!directions) clusterManager.addItem(new MarkerItem(id,latLng, name, resultEntry.getKey().toString().substring(resultEntry.getKey().toString().indexOf("\"") + 1, resultEntry.getKey().toString().lastIndexOf("\"")), phone, email, web, hours, wiki));
                }
            }
            if (flag.equals("map2")) { // if we need route directions  setup query string
                List<String> providers = mLocationManager.getAllProviders();
                for (String provider : providers) { // find last location on gps
                    if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        break;
                    }
                    Location loc = mLocationManager.getLastKnownLocation(provider);
                    if(loc!=null && search.getQuery().toString().equals("My Location")) location=loc;
                    else if (loc!=null && search2.getQuery().toString().equals("My Location")) latLng2 = new LatLng(loc.getLatitude(),loc.getLongitude());

                }
                // htttp get request to google maps api ,depening on the locations found previously
                String url="https://maps.googleapis.com/maps/api/directions/json?origin="+ location.getLatitude()+","+location.getLongitude()+"&destination="+latLng2.latitude+","+latLng2.longitude+"&mode=driving&key="+"YOUR_GOOGLE_MAPS_API_KEY";
                Connection con=new Connection(url,mapFragment);
                progressBar.setVisibility(View.VISIBLE);
                con.execute(url,"directions");
            }
            clusterManager.cluster();
            clearKeyboard(search);
            clearKeyboard(search2);
            setHeightListView(true,list);
            setHeightListView(true,list2);
        }
        else if(flag.contains("directions")){ // response from google maps api
            try {
                if(line!=null) line.remove();
                for (Map.Entry<Value, HashMap<String, Value>> resultEntry : results.entrySet()){ // draw line, code from : https://github.com/snufflesrea/GoogleDirectionExample/blob/master/app/src/main/java/com/andreasgift/googledirectionexample/MapsActivity.java
                    HashMap<String,Value>hashMap=resultEntry.getValue();
                    for (Map.Entry<String, Value> res : hashMap.entrySet()){
                        JSONObject myObject = new JSONObject(res.getKey());
                        JsonParser parser = new JsonParser();
                        List<List<HashMap<String, String>>> routes= parser.parse(myObject);
                        ArrayList points = null;
                        PolylineOptions lineOptions = null;
                        for (int i = 0; i < routes.size(); i++) {
                            points = new ArrayList();
                            lineOptions = new PolylineOptions();

                            List<HashMap<String, String>> path = routes.get(i);

                            for (int j = 0; j < path.size(); j++) {
                                HashMap<String, String> point = path.get(j);

                                double lat = Double.parseDouble(point.get("lat"));
                                double lng = Double.parseDouble(point.get("lng"));
                                LatLng position = new LatLng(lat, lng);

                                points.add(position);
                            }

                            lineOptions.addAll(points);
                            lineOptions.width(12);
                            lineOptions.color(Color.BLUE);
                            lineOptions.geodesic(true);

                        }

// Drawing polyline in the Google Map for the i-th route
                        line=googleMap.addPolyline(lineOptions);
                    }
                }


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            for (Map.Entry<Value, HashMap<String, Value>> resultEntry : results.entrySet()) {
                String name = resultEntry.getKey().toString();
                name = name.substring(name.indexOf("\"") + 1, name.lastIndexOf("\""));
                allSugestions.add(name);
            }
        }
    }
    void setHeightListView(boolean flag,ListView list) { // set height list depending on items available
        ListAdapter listadp = list.getAdapter();
        if(flag) {
            ViewGroup.LayoutParams params = list.getLayoutParams();
            params.height = 0;
            list.setLayoutParams(params);
            list.requestLayout();

            return;
        }
        if (listadp != null) {
            int totalHeight = 0;
            for (int i = 0; i < listadp.getCount(); i++) {
                View listItem = listadp.getView(i, null, list);
                listItem.measure(0, 0);
                totalHeight += listItem.getMeasuredHeight();
            }
            ViewGroup.LayoutParams params = list.getLayoutParams();
            params.height = totalHeight>=400 ? 400: totalHeight;
            list.setLayoutParams(params);
            list.requestLayout();
        }
    }
    public String mapStr(){ // setup bounding box for points of interest(string for query to GRAPHDB)
        LatLngBounds curScreen = googleMap.getProjection().getVisibleRegion().latLngBounds;
        return !checkedFilters[0] ? "    FILTER(geof:sfContains( \n" +
                "    '''POLYGON((" + curScreen.northeast.longitude + " " + curScreen.northeast.latitude + "," + curScreen.northeast.longitude + " " + curScreen.southwest.latitude + "," +
                curScreen.southwest.longitude + " " + curScreen.southwest.latitude + "," + curScreen.southwest.longitude + " " + curScreen.northeast.latitude
                + "," + curScreen.northeast.longitude + " " + curScreen.northeast.latitude + ")) '''^^geo:wktLiteral,STRDT((CONCAT(\"POINT(\" ,?lon, \" \",?lat,\")\")),geo:wktLiteral)))\n" : "";
    }
    public MarkerItem showDetails(Marker marker) { // show details of the marker
        Algorithm<MarkerItem> al = clusterManager.getAlgorithm();
        Collection<MarkerItem> col = al.getItems();
        MarkerItem mark=null;
        for (MarkerItem m : col) {
            if (marker.getSnippet().equals(m.getSnippet())) {
                mark=m;
                break;
            }
        }
        return mark;
    }
}