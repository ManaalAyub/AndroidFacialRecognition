package com.lordjoe.identifier.android;


import android.content.Context;
import android.graphics.Bitmap;
import android.media.UnsupportedSchemeException;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.lordjoe.identifier.R;
import com.lordjoe.identifier.RegisteredPerson;
import com.lordjoe.identifier.RegisteredPersonSet;

import java.io.File;


/**
 * Created by Steve on 4/11/2017.
 */

public class PersonSelectorAdapter extends ArrayAdapter<RegisteredPerson> {
    private final Context ctx;
    private RegisteredPerson[] people;
    private int resource;

    public PersonSelectorAdapter(Context context , int resource,RegisteredPerson[] objects) {
        super(context, resource , objects);
        this.ctx = context;
        this.resource = resource;
        people = objects;
     }



    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Log.e("Drop","pos " + position);
        return super.getDropDownView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = inflater.inflate(resource, null);

        RegisteredPerson person = people[position];
        TextView textView = (TextView) convertView.findViewById(R.id.spinnerTextView);
        textView.setText(person.getName());

        ImageView imageView = (ImageView) convertView.findViewById(R.id.spinnerImages);
        Bitmap image = bitmapFromPerson(person);
        imageView.setImageBitmap(image);

        return convertView;

    }

    public static final int DESIRED_WIDTH = 100;
    public static final int DESIRED_HEIGHT = 100;

    public Bitmap bitmapFromPerson(RegisteredPerson person) {
        File exemplar = person.getExemplar();
        return AndroidUtilities.fromFile(exemplar,DESIRED_WIDTH,DESIRED_HEIGHT);
    }
}
