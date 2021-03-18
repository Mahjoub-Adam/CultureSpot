package com.example.culturespot;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BottomNavigationActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigation;
    private Fragment mapFragment, favoritesFragment, profileFragment, fragment;
    private FragmentManager fm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bottom_navigation);
        mapFragment = new MapFragment();
        favoritesFragment = new FavoritesFragment((MapFragment) mapFragment);
        profileFragment = new ProfileFragment();
        fm = getSupportFragmentManager();
        fragment = mapFragment;
        fm.beginTransaction().add(R.id.fragment_container, favoritesFragment, "3").hide(favoritesFragment).commit();
        fm.beginTransaction().add(R.id.fragment_container, profileFragment, "2").hide(profileFragment).commit();
        fm.beginTransaction().add(R.id.fragment_container, mapFragment, "1").commit();
        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    @Override
    public void onBackPressed() {
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) { // user pressed back from routes so we must show him all (depending on filters) points of interest
        switch (item.getItemId()) {
            case android.R.id.home:
                String str="PREFIX geo: <http://www.opengis.net/ont/geosparql#>\n" +
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
                        "    FILTER (?type IN (" + ((MapFragment) mapFragment).getCategories() + "))\n"+
                        "     ?place onto:hasLatitude ?lat.\n" +
                        "     ?place onto:hasLongitude ?lon.\n";
                str+=((MapFragment) mapFragment).mapStr();
                str+="}";
                ((MapFragment)mapFragment).reset( str); // reset map
                setBackButton(false);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    public void setBackButton(boolean b){
        getSupportActionBar().setDisplayHomeAsUpEnabled(b);
    } // set back button when showing a route
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            Fragment previous = fragment; // fragment selection
            switch (item.getItemId()) {
                case R.id.favorites:
                    fragment = favoritesFragment;
                    break;
                case R.id.profile:
                    fragment = profileFragment;
                    break;
                default:
                    fragment = mapFragment;
                    break;
            }
            fm.beginTransaction().hide(previous).show(fragment).commit();
            return true;
        }
    };

}