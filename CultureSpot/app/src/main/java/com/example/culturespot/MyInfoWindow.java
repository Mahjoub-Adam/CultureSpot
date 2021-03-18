package com.example.culturespot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class MyInfoWindow implements GoogleMap.InfoWindowAdapter { // custom info window with all the information for the point of interest (if it exists)
        private  LayoutInflater mInflater;
        private MapFragment mapFragment;
        public MyInfoWindow(LayoutInflater inflater,MapFragment mapFragment) {
                this.mInflater=inflater;
                this.mapFragment = mapFragment;
        }
        private void tvSetup(String first,String text,TextView tv){
                if(!text.equals("")) tv.setText(first.concat(text));
                else {
                        ViewGroup.LayoutParams params = tv.getLayoutParams();
                        params.height=0;
                        tv.setLayoutParams(params);
                }
        }
        private View setup(MarkerItem m){
                View popup = mInflater.inflate(R.layout.place_details, null);
                ((TextView) popup.findViewById(R.id.title)).setText(m.getTitle());
                tvSetup("Opening hours : ",m.getHours(),popup.findViewById(R.id.hours));
                tvSetup("Phone number(s) : ",m.getPhone(),popup.findViewById(R.id.phone));
                tvSetup("Website : ",m.getWeb(),popup.findViewById(R.id.web));
                tvSetup("Wikipedia : ",m.getWiki(),popup.findViewById(R.id.wiki));
                tvSetup("Email : ",m.getEmail(),popup.findViewById(R.id.email));
                tvSetup("","",popup.findViewById(R.id.show));
                tvSetup("","",popup.findViewById(R.id.nearby));
                popup.findViewById(R.id.favorite).setVisibility(View.GONE);
                return  popup;
        }
        @Override
        public View getInfoWindow(Marker marker) {
                 return setup(mapFragment.showDetails(marker));
        }
        @Override
        public View getInfoContents(Marker marker) {
                return setup(mapFragment.showDetails(marker));
        }
}