package com.example.scpreader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "scp_reader.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME = "scp_articles";
    private static final String COLUMN_NUMBER = "number";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_CONTENT = "content";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_NUMBER + " TEXT PRIMARY KEY, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_CONTENT + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Сохранение статьи
    public void saveArticle(String number, String title, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NUMBER, number);
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_CONTENT, content);
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    // Получение текста статьи
    public String getArticleContent(String number) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{COLUMN_CONTENT}, 
                COLUMN_NUMBER + "=?", new String[]{number}, null, null, null);
        
        String content = null;
        if (cursor != null && cursor.moveToFirst()) {
            content = cursor.getString(0);
            cursor.close();
        }
        db.close();
        return content;
    }
}
