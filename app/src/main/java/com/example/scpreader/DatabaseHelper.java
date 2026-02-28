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
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_SCP = "scp_objects";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NUMBER = "number";
    private static final String COLUMN_TITLE = "title";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_SCP_TABLE = "CREATE TABLE " + TABLE_SCP + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NUMBER + " TEXT UNIQUE,"
                + COLUMN_TITLE + " TEXT" + ")";
        db.execSQL(CREATE_SCP_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCP);
        onCreate(db);
    }

    public void addSCP(SCPObject scp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NUMBER, scp.getNumber());
        values.put(COLUMN_TITLE, scp.getTitle());

        // Используем replace чтобы избежать дубликатов (UNIQUE constraint на number)
        db.replace(TABLE_SCP, null, values);
        db.close();
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
                scpList.add(scp);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return scpList;
    }
}
