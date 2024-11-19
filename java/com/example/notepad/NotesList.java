/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.notepad;

import static com.example.notepad.NotePad.AUTHORITY;

import com.example.notepad.NotePad;
import com.google.android.material.tabs.TabLayout;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Displays a list of notes. Will display notes from the {@link Uri}
 * provided in the incoming Intent if there is one, otherwise it defaults to displaying the
 * contents of the {@link NotePadProvider}.
 *
 * NOTE: Notice that the provider operations in this Activity are taking place on the UI thread.
 * This is not a good practice. It is only done here to make the code more readable. A real
 * application should use the {@link android.content.AsyncQueryHandler} or
 * {@link android.os.AsyncTask} object to perform operations asynchronously on a separate thread.
 */
public class NotesList extends ListActivity {

    // For logging and debugging
    private static final String TAG = "NotesList";

    /**
     * The columns needed by the cursor adapter
     */

    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_CREATE_DATE
    };

    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_CREATE_DATE = 2;

    /**
     * onCreate is called when Android starts this Activity from scratch.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 创建一个垂直的LinearLayout作为主布局
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#f2f2f2"));
        //mainLayout.setBackgroundColor(Color.rgb(245, 245, 205));
        mainLayout.setPadding(18, 18, 18, 18);

        // 创建搜索框
        View searchLayout = getLayoutInflater().inflate(R.layout.search_layout, null);
        EditText searchBox = searchLayout.findViewById(R.id.search_box);

        // 创建一个水平的LinearLayout作为标签容器
        LinearLayout tabContainer = new LinearLayout(this);
        LinearLayout.LayoutParams tabContainerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        tabContainer.setLayoutParams(tabContainerParams);
        tabContainer.setOrientation(LinearLayout.HORIZONTAL);
        tabContainer.setBackgroundColor(Color.WHITE);
        tabContainer.setPadding(0, 16, 0, 16);

// 创建两个Button作为标签
        Button notesTab = new Button(this);
        Button todoTab = new Button(this);

// 设置按钮参数
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f);  // weight为1，使两个按钮平分宽度
        buttonParams.setMargins(8, 0, 8, 0);  // 设置按钮间距

        notesTab.setLayoutParams(buttonParams);
        todoTab.setLayoutParams(buttonParams);

// 设置按钮文字
        notesTab.setText("笔记");
        todoTab.setText("待办");

// 设置按钮背景和文字颜色
        notesTab.setBackgroundColor(Color.rgb(33, 150, 243));  // 默认选中的颜色
        todoTab.setBackgroundColor(Color.LTGRAY);
        notesTab.setTextColor(Color.WHITE);
        todoTab.setTextColor(Color.BLACK);

// 添加点击事件
        notesTab.setOnClickListener(v -> {
            notesTab.setBackgroundColor(Color.rgb(33, 150, 243));
            notesTab.setTextColor(Color.WHITE);
            todoTab.setBackgroundColor(Color.LTGRAY);
            todoTab.setTextColor(Color.BLACK);
            performSearch(searchBox.getText().toString());
        });

        todoTab.setOnClickListener(v -> {
            todoTab.setBackgroundColor(Color.rgb(33, 150, 243));
            todoTab.setTextColor(Color.WHITE);
            notesTab.setBackgroundColor(Color.LTGRAY);
            notesTab.setTextColor(Color.BLACK);
            performTodoSearch(searchBox.getText().toString());
        });

// 将按钮添加到容器中
        tabContainer.addView(notesTab);
        tabContainer.addView(todoTab);

// 将搜索框添加到主布局
        mainLayout.addView(searchLayout);

// 将标签容器添加到主布局
        mainLayout.addView(tabContainer);

        // 创建一个新的ListView
        ListView listView = new ListView(this);
        listView.setId(android.R.id.list);

        // 设置ListView的属性
        listView.setDivider(null);
        listView.setDividerHeight(10);
        listView.setPadding(0, 24, 0, 0);
        listView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // 创建ListView的布局参数
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        listView.setLayoutParams(listParams);

        // 将ListView直接添加到主布局
        mainLayout.addView(listView);

        // 设置活动的内容视图
        setContentView(mainLayout);

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        listView.setOnCreateContextMenuListener(this);

        // 初始加载所有数据
        performSearch("");

    }
    // 添加待办搜索方法
    private void performTodoSearch(String query) {
        String selection = null;
        String[] selectionArgs = null;

        if (!TextUtils.isEmpty(query)) {
            selection = NotePad.TodoItems.COLUMN_NAME_TITLE + " LIKE ?";
            selectionArgs = new String[]{"%" + query + "%"};
        }

        Cursor cursor = managedQuery(
                NotePad.TodoItems.CONTENT_URI,  // 使用待办事项的URI
                new String[] {  // 待办事项的列
                        NotePad.TodoItems._ID,
                        NotePad.TodoItems.COLUMN_NAME_TITLE,
                        NotePad.TodoItems.COLUMN_NAME_CREATE_DATE,
                        NotePad.TodoItems.COLUMN_NAME_COMPLETED  // 新增完成状态列
                },
                selection,
                selectionArgs,
                NotePad.TodoItems.DEFAULT_SORT_ORDER
        );

        String[] dataColumns = {
                NotePad.TodoItems.COLUMN_NAME_TITLE,
                NotePad.TodoItems.COLUMN_NAME_CREATE_DATE,
                NotePad.TodoItems.COLUMN_NAME_COMPLETED
        };

        int[] viewIDs = {
                android.R.id.text1,
                R.id.text_note_time,
                R.id.checkbox_todo
        };

        SimpleCursorAdapter adapter = getTodoCursorAdapter(cursor, dataColumns, viewIDs);
        setListAdapter(adapter);
    }
    private void performSearch(String query) {
        String selection = null;
        String[] selectionArgs = null;

        if (!TextUtils.isEmpty(query)) {
            selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ?";
            selectionArgs = new String[]{"%" + query + "%"};
        }

        Cursor cursor = managedQuery(
                getIntent().getData(),
                PROJECTION,
                selection,
                selectionArgs,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        String[] dataColumns = {
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_CREATE_DATE
        };

        int[] viewIDs = {
                android.R.id.text1,
                R.id.text_note_time
        };

        SimpleCursorAdapter adapter = getSimpleCursorAdapter(cursor, dataColumns, viewIDs);
        setListAdapter(adapter);
    }
    private @NonNull SimpleCursorAdapter getSimpleCursorAdapter(Cursor cursor, String[] dataColumns, int[] viewIDs) {
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                R.layout.noteslist_item,
                cursor,
                dataColumns,
                viewIDs
        ) {
            private final String[] colors = {"#ffffff", "#ffe1ff", "#cae1ff", "#ffc0cb"};
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                // 根据位置循环设置颜色
                view.setBackgroundColor(Color.parseColor(colors[position % 4]));
                return view;
            }
        };
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (columnIndex == COLUMN_INDEX_CREATE_DATE) {
                    long timestamp = cursor.getLong(columnIndex);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    String dateStr = sdf.format(new Date(timestamp));
                    TextView textView = (TextView) view;
                    textView.setText(dateStr);
                    return true;
                }
                return false;
            }
        });
        return adapter;
    }

    private @NonNull SimpleCursorAdapter getTodoCursorAdapter(Cursor cursor, String[] dataColumns, int[] viewIDs) {
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                R.layout.todo_list_item,
                cursor,
                dataColumns,
                viewIDs
        ) {
            private final String[] colors = {"#ffffff", "#ffe1ff", "#cae1ff", "#ffc0cb"};
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                // 根据位置循环设置颜色
                view.setBackgroundColor(Color.parseColor(colors[position % 4]));
                // 获取当前项的数据
                Cursor cursor = (Cursor) getItem(position);
                // 找到CheckBox
                CheckBox checkBox = view.findViewById(R.id.checkbox_todo);
                // 设置CheckBox的状态
                @SuppressLint("Range") boolean isCompleted = cursor.getInt(cursor.getColumnIndex(NotePad.TodoItems.COLUMN_NAME_COMPLETED)) == 1;
                checkBox.setChecked(isCompleted);
                // 为CheckBox添加点击事件
                checkBox.setOnClickListener(v -> {
                    ContentValues values = new ContentValues();
                    values.put(NotePad.TodoItems.COLUMN_NAME_COMPLETED, checkBox.isChecked() ? 1 : 0);
                    @SuppressLint("Range") long id = cursor.getLong(cursor.getColumnIndex(NotePad.TodoItems._ID));
                    Uri todoUri = ContentUris.withAppendedId(NotePad.TodoItems.CONTENT_URI, id);
                    getContentResolver().update(todoUri, values, null, null);
                    TextView titleView = view.findViewById(android.R.id.text1);
                    if (checkBox.isChecked()) {
                        titleView.setPaintFlags(titleView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    } else {
                        titleView.setPaintFlags(titleView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    }
                });
                return view;
            }
        };

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                // 处理时间显示
                if (view.getId() == R.id.text_note_time) {
                    long timestamp = cursor.getLong(columnIndex);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    String dateStr = sdf.format(new Date(timestamp));
                    ((TextView) view).setText(dateStr);
                    return true;
                }
                // 处理标题的删除线
                else if (view.getId() == android.R.id.text1) {
                    TextView titleView = (TextView) view;
                    String title = cursor.getString(columnIndex);
                    titleView.setText(title);

                    // 检查完成状态
                    int completedIndex = cursor.getColumnIndex(NotePad.TodoItems.COLUMN_NAME_COMPLETED);
                    boolean isCompleted = cursor.getInt(completedIndex) == 1;

                    // 根据完成状态设置删除线
                    if (isCompleted) {
                        titleView.setPaintFlags(titleView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    } else {
                        titleView.setPaintFlags(titleView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    }
                    return true;
                }
                return false;
            }
        });

        return adapter;
    }
    // 添加dp到px的转换方法
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    /**
     * Called when the user clicks the device's Menu button the first time for
     * this Activity. Android passes in a Menu object that is populated with items.
     *
     * Sets up a menu that provides the Insert option plus a list of alternative actions for
     * this Activity. Other applications that want to handle notes can "register" themselves in
     * Android by providing an intent filter that includes the category ALTERNATIVE and the
     * mimeTYpe NotePad.Notes.CONTENT_TYPE. If they do this, the code in onCreateOptionsMenu()
     * will add the Activity that contains the intent filter to its list of options. In effect,
     * the menu will offer the user other applications that can handle notes.
     * @param menu A Menu object, to which menu items should be added.
     * @return True, always. The menu should be displayed.
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 增加日志跟踪菜单创建
        Log.d(TAG, "onCreateOptionsMenu called");

        // 使用 MenuInflater 加载菜单资源
        getMenuInflater().inflate(R.menu.list_options_menu, menu);

        // Generate any additional actions that can be performed on the
        // overall list.  In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        Log.d(TAG, "Menu inflated");
        return super.onCreateOptionsMenu(menu);


    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // The paste menu item is enabled if there is data on the clipboard.
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);


        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);

        // If the clipboard contains an item, enables the Paste option on the menu.
        if (clipboard.hasPrimaryClip()) {
            mPasteItem.setEnabled(true);
        } else {
            // If the clipboard is empty, disables the menu's Paste option.
            mPasteItem.setEnabled(false);
        }

        // Gets the number of notes currently being displayed.
        final boolean haveItems = getListAdapter().getCount() > 0;

        // If there are any notes in the list (which implies that one of
        // them is selected), then we need to generate the actions that
        // can be performed on the current selection.  This will be a combination
        // of our own specific actions along with any extensions that can be
        // found.
        if (haveItems) {

            // This is the selected item.
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());

            // Creates an array of Intents with one element. This will be used to send an Intent
            // based on the selected menu item.
            Intent[] specifics = new Intent[1];

            // Sets the Intent in the array to be an EDIT action on the URI of the selected note.
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);

            // Creates an array of menu items with one element. This will contain the EDIT option.
            MenuItem[] items = new MenuItem[1];

            // Creates an Intent with no specific action, using the URI of the selected note.
            Intent intent = new Intent(null, uri);

            /* Adds the category ALTERNATIVE to the Intent, with the note ID URI as its
             * data. This prepares the Intent as a place to group alternative options in the
             * menu.
             */
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            /*
             * Add alternatives to the menu
             */
            menu.addIntentOptions(
                Menu.CATEGORY_ALTERNATIVE,  // Add the Intents as options in the alternatives group.
                Menu.NONE,                  // A unique item ID is not required.
                Menu.NONE,                  // The alternatives don't need to be in order.
                null,                       // The caller's name is not excluded from the group.
                specifics,                  // These specific options must appear first.
                intent,                     // These Intent objects map to the options in specifics.
                Menu.NONE,                  // No flags are required.
                items                       // The menu items generated from the specifics-to-
                                            // Intents mapping
            );
                // If the Edit menu item exists, adds shortcuts for it.
                if (items[0] != null) {

                    // Sets the Edit menu item shortcut to numeric "1", letter "e"
                    items[0].setShortcut('1', 'e');
                }
            } else {
                // If the list is empty, removes any existing alternative actions from the menu
                menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
            }

        // Displays the menu
        return true;
    }

    /**
     * This method is called when the user selects an option from the menu, but no item
     * in the list is selected. If the option was INSERT, then a new Intent is sent out with action
     * ACTION_INSERT. The data from the incoming Intent is put into the new Intent. In effect,
     * this triggers the NoteEditor activity in the NotePad application.
     *
     * If the item was not INSERT, then most likely it was an alternative option from another
     * application. The parent method is called to process the item.
     * @param item The menu item that was selected by the user
     * @return True, if the INSERT menu item was selected; otherwise, the result of calling
     * the parent method.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int a = item.getItemId();
        if (a==R.id.menu_add){
            startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
            return true;
        }else if (a == R.id.menu_add_todo) {
            // 根据当前选中的标签页决定创建类型
            Intent intent = new Intent(Intent.ACTION_INSERT, NotePad.TodoItems.CONTENT_URI);
            // 可以添加额外标记表明这是待办事项
            intent.putExtra("type", "todo");
            startActivity(intent);
            return true;
        } else if (a==R.id.menu_paste){
            startActivity(new Intent(Intent.ACTION_PASTE, getIntent().getData()));
            return true;
        }else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This method is called when the user context-clicks a note in the list. NotesList registers
     * itself as the handler for context menus in its ListView (this is done in onCreate()).
     *
     * The only available options are COPY and DELETE.
     *
     * Context-click is equivalent to long-press.
     *
     * @param menu A ContexMenu object to which items should be added.
     * @param view The View for which the context menu is being constructed.
     * @param menuInfo Data associated with view.
     * @throws ClassCastException
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {

        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        // Tries to get the position of the item in the ListView that was long-pressed.
        try {
            // Casts the incoming data object into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            // If the menu object can't be cast, logs an error.
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        /*
         * Gets the data associated with the item at the selected position. getItem() returns
         * whatever the backing adapter of the ListView has associated with the item. In NotesList,
         * the adapter associated all of the data for a note with its list item. As a result,
         * getItem() returns that data as a Cursor.
         */
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);

        // If the cursor is empty, then for some reason the adapter can't get the data from the
        // provider, so returns null to the caller.
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }

        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        // Sets the menu header to be the title of the selected note.
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        // Append to the
        // menu items for any other activities that can do stuff with it
        // as well.  This does a query on the system for any activities that
        // implement the ALTERNATIVE_ACTION for our data, adding a menu item
        // for each one that is found.
        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(), 
                                        Integer.toString((int) info.id) ));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }

    /**
     * This method is called when the user selects an item from the context menu
     * (see onCreateContextMenu()). The only menu items that are actually handled are DELETE and
     * COPY. Anything else is an alternative option, for which default handling should be done.
     *
     * @param item The selected menu item
     * @return True if the menu item was DELETE, and no default processing is need, otherwise false,
     * which triggers the default handling of the item.
     * @throws ClassCastException
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        /*
         * Gets the extra info from the menu item. When an note in the Notes list is long-pressed, a
         * context menu appears. The menu items for the menu automatically get the data
         * associated with the note that was long-pressed. The data comes from the provider that
         * backs the list.
         *
         * The note's data is passed to the context menu creation routine in a ContextMenuInfo
         * object.
         *
         * When one of the context menu items is clicked, the same data is passed, along with the
         * note ID, to onContextItemSelected() via the item parameter.
         */
        try {
            // Casts the data object in the item into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {

            // If the object can't be cast, logs an error
            Log.e(TAG, "bad menuInfo", e);

            // Triggers default processing of the menu item.
            return false;
        }
        // Appends the selected note's ID to the URI sent with the incoming Intent.
        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        /*
         * Gets the menu item's ID and compares it to known actions.
         */
        if (item.getItemId() == R.id.context_open) {
            // Launch activity to view/edit the currently selected item
            startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
            return true;
        } else if (item.getItemId() == R.id.context_copy) {
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);

            clipboard.setPrimaryClip(ClipData.newUri(   // new clipboard item holding a URI
                    getContentResolver(),               // resolver to retrieve URI info
                    "Note",                            // label for the clip
                    noteUri)                           // the URI
            );

            return true;
        } else if (item.getItemId() == R.id.context_delete) {
            getContentResolver().delete(
                    noteUri,  // The URI of the provider
                    null,     // No where clause is needed, since only a single note ID is being passed in
                    null      // No where clause is used, so no where arguments are needed.
            );

            // Returns to the caller and skips further processing.
            return true;
        } else {
            return super.onContextItemSelected(item);
        }
    }

    /**
     * This method is called when the user clicks a note in the displayed list.
     *
     * This method handles incoming actions of either PICK (get data from the provider) or
     * GET_CONTENT (get or create data). If the incoming action is EDIT, this method sends a
     * new Intent to start NoteEditor.
     * @param l The ListView that contains the clicked item
     * @param v The View of the individual item
     * @param position The position of v in the displayed list
     * @param id The row ID of the clicked item
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        // Constructs a new URI from the incoming URI and the row ID
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

        // Gets the action from the incoming Intent
        String action = getIntent().getAction();

        // Handles requests for note data
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {

            // Sets the result to return to the component that called this Activity. The
            // result contains the new URI
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {

            // Sends out an Intent to start an Activity that can handle ACTION_EDIT. The
            // Intent's data is the note ID URI. The effect is to call NoteEdit.
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }
}
