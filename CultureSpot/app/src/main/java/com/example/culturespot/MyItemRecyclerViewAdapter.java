package com.example.culturespot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.culturespot.FavoritesList.Item;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Item}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MyItemRecyclerViewAdapter extends RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder> { // custom recyclerview to get items from firebase

    private final List<Item> mValues;
    FavoritesFragment favoritesFragment;
    View view;
    private FirebaseAuth mAuth;
    public MyItemRecyclerViewAdapter(List<Item> items,FavoritesFragment favoritesFragment,View view) {
        mValues = items;
        this.favoritesFragment=favoritesFragment;
        mAuth = FirebaseAuth.getInstance();
        this.view=view;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_favorites, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mIdView.setText(mValues.get(position).name);
        holder.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseUser user = mAuth.getCurrentUser();
                DatabaseReference reference= FirebaseDatabase.getInstance().getReference("Users");
                String userId=user.getUid();
                reference.child(userId).addListenerForSingleValueEvent(new ValueEventListener() { // get favorites from document in user collection
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User userProfile1=snapshot.getValue(User.class);
                        if(userProfile1!=null){
                            String first_name=userProfile1.first_name;
                            String email=userProfile1.email;
                            String surname=userProfile1.surname;
                            String username=userProfile1.username;
                            User userProfile=userProfile1.favorites==null ?new User(username,email,first_name,surname) : userProfile1;
                            userProfile.removeFavorite(mValues.get(position).id); // remove favorite
                            reference.child(userId).setValue(userProfile).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    try {
                                        FavoritesList.ITEMS.remove(holder.mItem); // remove favorite from list
                                        favoritesFragment.reset(view); // reset favorites
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });

            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mIdView;
        public final TextView mContentView;
        public final ImageButton button;
        public Item mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;

            button=(ImageButton) view.findViewById(R.id.favorite);
            mIdView = (TextView) view.findViewById(R.id.item_number);
            mContentView = (TextView) view.findViewById(R.id.content);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}