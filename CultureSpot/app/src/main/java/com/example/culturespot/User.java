package com.example.culturespot;

import java.util.HashMap;
// user data class
public class User {
    public String username,email,first_name,surname;
    public HashMap<String,String> favorites;
    public User(String username,String email,String first_name,String surname){
        this.username=username;
        this.email=email;
        this.first_name=first_name;
        this.surname=surname;
        this.favorites=new HashMap<>();
    }
    public void  addFavorite(String id,String name){
        this.favorites.put(id,name);
    }
    public void removeFavorite(String id){
        this.favorites.remove(id);
    }
    public User(){

    }
}
