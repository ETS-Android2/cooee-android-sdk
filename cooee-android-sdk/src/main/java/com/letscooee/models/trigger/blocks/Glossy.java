package com.letscooee.models.trigger.blocks;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Glossy implements Parcelable {

    private static final int DEFAULT_RADIUS = 25;
    private static final int DEFAULT_SAMPLING = 8;

    @SerializedName("r")
    @Expose
    private final int radius;

    @SerializedName("s")
    @Expose
    private final int sampling;

    @SerializedName("clr")
    @Expose
    private final Colour colour;

    protected Glossy(Parcel in) {
        radius = in.readInt();
        sampling = in.readInt();
        colour = in.readParcelable(Colour.class.getClassLoader());
    }

    public static final Creator<Glossy> CREATOR = new Creator<Glossy>() {
        @Override
        public Glossy createFromParcel(Parcel in) {
            return new Glossy(in);
        }

        @Override
        public Glossy[] newArray(int size) {
            return new Glossy[size];
        }
    };

    public int getRadius() {
        return radius != 0 ? radius : DEFAULT_RADIUS;
    }

    public int getSampling() {
        return sampling != 0 ? sampling : DEFAULT_SAMPLING;
    }

    public Colour getColor() {
        return colour;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(radius);
        dest.writeInt(sampling);
        dest.writeParcelable(colour, flags);
    }
}
