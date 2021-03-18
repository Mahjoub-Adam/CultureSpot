package com.example.culturespot;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FavoritesList {
    public static final List<Item> ITEMS = new ArrayList<Item>();
    private static FirebaseAuth mAuth;
    public static class Item{
        public final String id;
        public final String name;
        public Item(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
    public static void remove(Item item){
        Item temp=null;
        for(Item i : ITEMS){ // find and remove favorite
            if(i.id.equals(item.id)){
                temp=i;
                break;
            }
        }
        ITEMS.remove(temp);
    }
    public static void getItems() {
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        DatabaseReference reference= FirebaseDatabase.getInstance().getReference("Users");
        String userId=user.getUid();
        reference.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User userProfile1 = snapshot.getValue(User.class); // get favorites from firebase
                if (userProfile1 != null) {
                    String first_name = userProfile1.first_name;
                    String email = userProfile1.email;
                    String surname = userProfile1.surname;
                    String username = userProfile1.username;
                    User userProfile = userProfile1.favorites == null ? new User(username, email, first_name, surname) : userProfile1;
                    ITEMS.clear();
                    for (Map.Entry<String,String> entry : userProfile.favorites.entrySet()) { // get all new favorite
                        ITEMS.add(new Item(entry.getKey(),entry.getValue()));
                    }
                    reference.child(userId).setValue(userProfile).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                        }
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}
