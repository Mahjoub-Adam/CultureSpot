package com.example.culturespot;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class MarkerItem implements ClusterItem { // custom MarketItem to keep all information
    LatLng position;
    String title,snippet,phone,email,web,hours,wiki,id;
    public MarkerItem(String id,LatLng position,String title,String snippet,String phone,String email,String web,String hours,String wiki){
        this.position=position;
        this.title=title;
        this.snippet=snippet;
        this.hours=hours;
        this.email=email;
        this.phone=phone;
        this.web=web;
        this.wiki=wiki;
        this.id=id;
    }
    @Override
    public LatLng getPosition(){
        return this.position;
    }
    @Override
    public String getTitle(){
        return this.title;
    }
    @Override
    public  String getSnippet(){
        return this.snippet;
    }
    public  String getId(){
        return this.id;
    }
    public  String getHours(){
        return this.hours;
    }
    public  String getWeb(){
        return this.web;
    }
    public  String getEmail(){
        return this.email;
    }
    public  String getPhone(){ return this.phone; }
    public  String getWiki(){ return this.wiki; }
}
