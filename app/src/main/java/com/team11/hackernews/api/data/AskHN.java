package com.team11.hackernews.api.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class AskHN extends Thread {

    public static final Parcelable.Creator<AskHN> CREATOR = new Parcelable.Creator<AskHN>() {
        public AskHN createFromParcel(Parcel in) {
            return new AskHN(in);
        }

        public AskHN[] newArray(int size) {
            return new AskHN[size];
        }
    };

    private AskHN(Parcel in) {
        mId = in.readLong();
        mBy = in.readString();
        mTime = in.readLong();
        mText = in.readString();
        mKids = new ArrayList<Long>();
        in.readList(mKids, List.class.getClassLoader());
        mScore = in.readLong();
        mTitle = in.readString();
    }


    public AskHN() {
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeString(mBy);
        dest.writeLong(mTime);
        dest.writeString(mText);
        dest.writeList(mKids);
        dest.writeLong(mScore);
        dest.writeString(mTitle);
    }

    @Override
    public boolean hasComments() {
        return true;
    }

    @Override
    public boolean hasURL() {
        return false;
    }
}
