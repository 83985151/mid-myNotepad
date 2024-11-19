package com.example.notepad;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

public class TodoEditor extends Activity {
    private EditText mTitleText;
    private EditText mContentText;
    private Uri mUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.todo_editor);

        mTitleText = findViewById(R.id.todo_title);
        mContentText = findViewById(R.id.todo_content);
        Button saveButton = findViewById(R.id.todo_save);

        // 获取intent
        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (Intent.ACTION_EDIT.equals(action)) {
            // 编辑现有待办
            mUri = intent.getData();
            Cursor cursor = managedQuery(mUri, null, null, null, null);
            cursor.moveToFirst();
            mTitleText.setText(cursor.getString(cursor.getColumnIndex(NotePad.TodoItems.COLUMN_NAME_TITLE)));
            mContentText.setText(cursor.getString(cursor.getColumnIndex(NotePad.TodoItems.COLUMN_NAME_NOTE)));
        } else if (Intent.ACTION_INSERT.equals(action)) {
            // 创建新待办
            mUri = getContentResolver().insert(NotePad.TodoItems.CONTENT_URI, new ContentValues());
        }

        saveButton.setOnClickListener(v -> {
            ContentValues values = new ContentValues();
            values.put(NotePad.TodoItems.COLUMN_NAME_TITLE, mTitleText.getText().toString());
            values.put(NotePad.TodoItems.COLUMN_NAME_NOTE, mContentText.getText().toString());
            values.put(NotePad.TodoItems.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());

            getContentResolver().update(mUri, values, null, null);
            finish();
        });
    }
}