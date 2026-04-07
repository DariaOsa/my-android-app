package edu.acg.carsharingapp.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "carsharing.db";
    private static final int DATABASE_VERSION = 1;

    // ================= USERS TABLE =================
    public static final String TABLE_USERS = "users";
    public static final String COL_USER_ID = "id";
    public static final String COL_USER_NAME = "name";
    public static final String COL_USER_EMAIL = "email";
    public static final String COL_USER_PASSWORD = "password";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // ================= CREATE TABLE =================
    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE " + TABLE_USERS + " (" +
                        COL_USER_ID + " TEXT PRIMARY KEY, " +
                        COL_USER_NAME + " TEXT, " +
                        COL_USER_EMAIL + " TEXT UNIQUE, " +
                        COL_USER_PASSWORD + " TEXT" +
                        ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For now, do nothing (simple app)
    }

    // ================= USER METHODS =================

    // ➕ Register user
    public boolean insertUser(String id, String name, String email, String password) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_USER_ID, id);
        values.put(COL_USER_NAME, name);
        values.put(COL_USER_EMAIL, email);
        values.put(COL_USER_PASSWORD, password);

        long result = db.insert(TABLE_USERS, null, values);
        db.close();

        return result != -1;
    }

    // 🔐 Login user
    public Cursor getUser(String email, String password) {

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM " + TABLE_USERS + " WHERE email=? AND password=?",
                new String[]{email, password}
        );
    }

    // 📧 Check if email already exists
    public boolean emailExists(String email) {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT 1 FROM " + TABLE_USERS + " WHERE email=? LIMIT 1",
                new String[]{email}
        );

        boolean exists = cursor.moveToFirst();

        cursor.close();
        db.close();

        return exists;
    }
}