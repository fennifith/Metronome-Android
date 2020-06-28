package me.jfenn.metronome.billing;

import androidx.appcompat.app.AppCompatActivity;

public interface BillingInterface {

    void onCreateActivity();
    void onDestroyActivity();
    String getPrice();
    String getSku();
    boolean isPremium();
    void onPremium(AppCompatActivity activity);
    void buyPremium(AppCompatActivity activity);

}
