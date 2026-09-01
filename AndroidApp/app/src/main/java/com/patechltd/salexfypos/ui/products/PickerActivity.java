package com.patechltd.salexfypos.ui.products;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.SimpleStringAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Relaxed single-column picker page used instead of alert-dialog pickers.
 * Returns RESULT_OK with EXTRA_INDEX (position of picked item, or -1 for "None")
 * or EXTRA_NEW_NAME when the user typed a brand-new value.
 */
public class PickerActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_ITEMS = "items";
    public static final String EXTRA_SELECTED_INDEX = "selectedIndex";
    public static final String EXTRA_ALLOW_NEW = "allowNew";
    public static final String EXTRA_NEW_HINT = "newHint";
    public static final String EXTRA_ALLOW_NONE = "allowNone";
    public static final String EXTRA_NONE_LABEL = "noneLabel";

    public static final String EXTRA_INDEX = "index";
    public static final String EXTRA_NEW_NAME = "newName";

    private final List<String> items = new ArrayList<>();
    private SimpleStringAdapter adapter;
    private String title;
    private boolean allowNew;
    private boolean allowNone;
    private int selectedIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picker);

        title = getIntent().getStringExtra(EXTRA_TITLE);
        allowNew = getIntent().getBooleanExtra(EXTRA_ALLOW_NEW, false);
        allowNone = getIntent().getBooleanExtra(EXTRA_ALLOW_NONE, false);
        selectedIndex = getIntent().getIntExtra(EXTRA_SELECTED_INDEX, -1);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(title == null ? "Pick one" : title);
        toolbar.setNavigationOnClickListener(v -> finish());

        items.addAll(getIntent().getStringArrayListExtra(EXTRA_ITEMS) == null
                ? new ArrayList<>() : getIntent().getStringArrayListExtra(EXTRA_ITEMS));
        if (allowNone) {
            String noneLabel = getIntent().getStringExtra(EXTRA_NONE_LABEL);
            items.add(noneLabel == null ? "None" : noneLabel);
        }

        RecyclerView list = findViewById(R.id.list);
        adapter = new SimpleStringAdapter(new SimpleStringAdapter.Listener() {
            @Override
            public void onClick(int position) {
                int index = mapIndex(position);
                Intent result = new Intent();
                result.putExtra(EXTRA_INDEX, index);
                setResult(RESULT_OK, result);
                finish();
            }

            @Override
            public void onEdit(int position) {
            }

            @Override
            public void onDelete(int position) {
            }
        }, false);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        adapter.submit(items);

        View newRow = findViewById(R.id.new_row);
        if (allowNew) {
            newRow.setVisibility(View.VISIBLE);
            EditText input = findViewById(R.id.new_input);
            String hint = getIntent().getStringExtra(EXTRA_NEW_HINT);
            if (hint != null) input.setHint("New " + hint);
            MaterialButton btnNew = findViewById(R.id.btn_new);
            btnNew.setOnClickListener(v -> {
                String value = input.getText() == null ? "" : input.getText().toString().trim();
                if (value.isEmpty()) {
                    Toast.makeText(this, "Type a name first", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent result = new Intent();
                result.putExtra(EXTRA_NEW_NAME, value);
                setResult(RESULT_OK, result);
                finish();
            });
        } else {
            newRow.setVisibility(View.GONE);
        }

        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            list.scrollToPosition(selectedIndex);
        }
    }

    /** Maps list position (with the trailing "None" entry) to the real index. */
    private int mapIndex(int position) {
        if (allowNone && position == items.size() - 1) return -1;
        return position;
    }
}
