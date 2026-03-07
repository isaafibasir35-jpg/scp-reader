package com.example.scpreader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "scp_database.db";
    private static final int DATABASE_VERSION = 3;

    private static final String TABLE_SCP = "scp_objects";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NUMBER = "number";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_IS_FAVORITE = "is_favorite";
    private static final String COLUMN_IS_READ = "is_read";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_SCP_TABLE = "CREATE TABLE " + TABLE_SCP + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NUMBER + " TEXT UNIQUE,"
                + COLUMN_TITLE + " TEXT,"
                + COLUMN_IS_FAVORITE + " INTEGER DEFAULT 0,"
                + COLUMN_IS_READ + " INTEGER DEFAULT 0" + ")";
        db.execSQL(CREATE_SCP_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_SCP + " ADD COLUMN " + COLUMN_IS_FAVORITE + " INTEGER DEFAULT 0");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_SCP + " ADD COLUMN " + COLUMN_IS_READ + " INTEGER DEFAULT 0");
        }
    }

    public java.util.Set<String> getAllFavoritesNumbers() {
        java.util.Set<String> favorites = new java.util.HashSet<>();
        String selectQuery = "SELECT " + COLUMN_NUMBER + " FROM " + TABLE_SCP + " WHERE " + COLUMN_IS_FAVORITE + " = 1";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                favorites.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return favorites;
    }

    public java.util.Set<String> getAllReadNumbers() {
        java.util.Set<String> readSet = new java.util.HashSet<>();
        String selectQuery = "SELECT " + COLUMN_NUMBER + " FROM " + TABLE_SCP + " WHERE " + COLUMN_IS_READ + " = 1";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                readSet.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return readSet;
    }

    public void addOrUpdateSCP(SCPObject scp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NUMBER, scp.getNumber());
        values.put(COLUMN_TITLE, scp.getTitle());
        
        Cursor cursor = db.query(TABLE_SCP, new String[]{COLUMN_IS_FAVORITE, COLUMN_IS_READ}, COLUMN_NUMBER + "=?", new String[]{scp.getNumber()}, null, null, null);
        if (cursor.moveToFirst()) {
            db.update(TABLE_SCP, values, COLUMN_NUMBER + "=?", new String[]{scp.getNumber()});
        } else {
            values.put(COLUMN_IS_FAVORITE, scp.isFavorite() ? 1 : 0);
            values.put(COLUMN_IS_READ, scp.isRead() ? 1 : 0);
            db.insert(TABLE_SCP, null, values);
        }
        cursor.close();
        db.close();
    }

    public void setFavorite(String number, String title, boolean isFavorite) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NUMBER, number);
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_IS_FAVORITE, isFavorite ? 1 : 0);
        
        int rows = db.update(TABLE_SCP, values, COLUMN_NUMBER + "=?", new String[]{number});
        if (rows == 0) {
            db.insert(TABLE_SCP, null, values);
        }
        db.close();
    }

    public void setRead(String number, String title, boolean isRead) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NUMBER, number);
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_IS_READ, isRead ? 1 : 0);
        
        int rows = db.update(TABLE_SCP, values, COLUMN_NUMBER + "=?", new String[]{number});
        if (rows == 0) {
            db.insert(TABLE_SCP, null, values);
        }
        db.close();
    }

    public boolean isFavorite(String number) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SCP, new String[]{COLUMN_IS_FAVORITE}, COLUMN_NUMBER + "=?", new String[]{number}, null, null, null);
        boolean favorite = false;
        if (cursor.moveToFirst()) {
            favorite = cursor.getInt(0) == 1;
        }
        cursor.close();
        db.close();
        return favorite;
    }

    public boolean isRead(String number) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SCP, new String[]{COLUMN_IS_READ}, COLUMN_NUMBER + "=?", new String[]{number}, null, null, null);
        boolean read = false;
        if (cursor.moveToFirst()) {
            read = cursor.getInt(0) == 1;
        }
        cursor.close();
        db.close();
        return read;
    }

    public List<SCPObject> getFavorites() {
        List<SCPObject> scpList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_SCP + " WHERE " + COLUMN_IS_FAVORITE + " = 1 ORDER BY id DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                SCPObject scp = new SCPObject(
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NUMBER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
                );
                scp.setFavorite(true);
                scp.setRead(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_READ)) == 1);
                scpList.add(scp);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return scpList;
    }

    public SCPObject getSCP(String number) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SCP, null, COLUMN_NUMBER + "=?", new String[]{number}, null, null, null);
        SCPObject scp = null;
        if (cursor.moveToFirst()) {
            scp = new SCPObject(
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NUMBER)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
            );
            scp.setFavorite(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_FAVORITE)) == 1);
            scp.setRead(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_READ)) == 1);
        }
        cursor.close();
        db.close();
        return scp;
    }

    public List<SCPObject> getAllSCPs() {
        List<SCPObject> scpList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_SCP + " ORDER BY id DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                SCPObject scp = new SCPObject(
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NUMBER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
                );
                scp.setFavorite(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_FAVORITE)) == 1);
                scp.setRead(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_READ)) == 1);
                scpList.add(scp);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return scpList;
    }

    public void deleteSCP(String number) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SCP, COLUMN_NUMBER + "=?", new String[]{number});
        db.close();
    }
}
