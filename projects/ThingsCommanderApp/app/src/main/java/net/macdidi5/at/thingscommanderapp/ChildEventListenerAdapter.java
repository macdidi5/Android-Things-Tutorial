package net.macdidi5.at.thingscommanderapp;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

// Firebase ChildEventListener 包裝類別
public class ChildEventListenerAdapter implements ChildEventListener {

    @Override
    public void onChildAdded(DataSnapshot ds, String previousChildKey) { }

    @Override
    public void onChildChanged(DataSnapshot ds, String previousChildKey) { }

    @Override
    public void onChildRemoved(DataSnapshot ds) { }

    @Override
    public void onChildMoved(DataSnapshot ds, String previousChildKey) { }

    @Override
    public void onCancelled(DatabaseError de) { }
    
}
