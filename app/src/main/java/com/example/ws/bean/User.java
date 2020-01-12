package com.example.ws.bean;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class User implements Parcelable {
    private String userId;
    private int height;
    private String gender;
    private Date birthDay;
    private int athleteType;
    private int choseShape;
    private int choseGoal;
    private double clothesWeight;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Date getBirthDay() {
        return birthDay;
    }

    public void setBirthDay(Date birthDay) {
        this.birthDay = birthDay;
    }


    public User () {

    }

    public int getAthleteType() {
        return athleteType;
    }

    public void setAthleteType(int athleteType) {
        this.athleteType = athleteType;
    }

    public int getChoseShape() {
        return choseShape;
    }

    public void setChoseShape(int choseShape) {
        this.choseShape = choseShape;
    }

    public int getChoseGoal() {
        return choseGoal;
    }

    public void setChoseGoal(int choseGoal) {
        this.choseGoal = choseGoal;
    }

    public double getClothesWeight() {
        return clothesWeight;
    }

    public void setClothesWeight(double clothesWeight) {
        this.clothesWeight = clothesWeight;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", height=" + height +
                ", gender='" + gender + '\'' +
                ", birthDay=" + birthDay +
                ", athleteType=" + athleteType +
                ", choseShape=" + choseShape +
                ", choseGoal=" + choseGoal +
                ", clothesWeight=" + clothesWeight +
                '}';
    }

    protected User(Parcel in) {
        this.userId = in.readString();
        this.height = in.readInt();
        this.gender = in.readString();
        long tmpBirthDay = in.readLong();
        this.birthDay = tmpBirthDay == -1 ? null : new Date(tmpBirthDay);
        this.athleteType = in.readInt();
        this.choseShape = in.readInt();
        this.choseGoal = in.readInt();
        this.clothesWeight = in.readDouble();
    }


    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.userId);
        dest.writeInt(this.height);
        dest.writeString(this.gender);
        dest.writeLong(this.birthDay != null ? this.birthDay.getTime() : -1);
        dest.writeInt(this.athleteType);
        dest.writeInt(this.choseShape);
        dest.writeInt(this.choseGoal);
        dest.writeDouble(this.clothesWeight);
    }
}
