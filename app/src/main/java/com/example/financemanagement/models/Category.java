package com.example.financemanagement.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Category implements Parcelable {
    String type;
    String name;
    String icon;
    Boolean fav;

    public Category(String type, String name, String icon) {
        this.type = type;
        this.name = name;
        this.icon = icon;
    }

    protected Category(Parcel in) {
        type = in.readString();
        name = in.readString();
        icon = in.readString();
    }

    public Category(String type, String icon) {
        this.type = type;
        this.icon = icon;
    }

    public static final Creator<Category> CREATOR = new Creator<Category>() {
        @Override
        public Category createFromParcel(Parcel in) {
            return new Category(in);
        }

        @Override
        public Category[] newArray(int size) {
            return new Category[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(type);
        dest.writeString(name);
        dest.writeString(icon);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}

