package com.letscooee.models.v3.block;

import android.os.Parcel;
import android.os.Parcelable;

public class Color implements Parcelable {

    private String hex;
    private Gradient grad;

    protected Color(Parcel in) {
        hex = in.readString();
        grad = in.readParcelable(Gradient.class.getClassLoader());
    }

    public static final Creator<Color> CREATOR = new Creator<Color>() {
        @Override
        public Color createFromParcel(Parcel in) {
            return new Color(in);
        }

        @Override
        public Color[] newArray(int size) {
            return new Color[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(hex);
        dest.writeParcelable(grad, flags);
    }

    public String getHex() {
        return hex;
    }

    public Gradient getGrad() {
        return grad;
    }

    public int getSolidColor() {
        return android.graphics.Color.parseColor(hex);
    }
}
