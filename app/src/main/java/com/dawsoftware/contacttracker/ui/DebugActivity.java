package com.dawsoftware.contacttracker.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import com.dawsoftware.contacttracker.R;
import com.dawsoftware.contacttracker.util.LogUtil;

public class DebugActivity extends AppCompatActivity {
	
	private NestedScrollView scrollView;
	
	private Button netButton;
	private Button dbButton;
	private Button clearNetButton;
	private Button clearDbButton;
	private Button copyButton;
	private Button scrollDownButton;
	private Button pushButton;
	private TextView unixtime;
	
	private TextView logView;
	
	@Override
	protected void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_debug);
		
		initViews();
	}
	
	private void initViews() {
		scrollView = findViewById(R.id.debug_scroll_view);
		
		netButton = findViewById(R.id.net_log_button);
		netButton.setOnClickListener(this::netButtonHandler);
		
		clearNetButton = findViewById(R.id.clear_net_button);
		clearNetButton.setOnClickListener(this::clearNetButtonHandler);
		
		dbButton = findViewById(R.id.db_log_button);
		dbButton.setOnClickListener(this::dbButtonHandler);
		
		clearDbButton = findViewById(R.id.clear_db_button);
		clearDbButton.setOnClickListener(this::clearDbButtonHandler);
		
		copyButton = findViewById(R.id.debug_copy_button);
		copyButton.setOnClickListener(this::copyButtonHandler);
		
		scrollDownButton = findViewById(R.id.debug_scroll_button);
		scrollDownButton.setOnClickListener(this::scrollDown);
		
		pushButton = findViewById(R.id.push_log_button);
		pushButton.setOnClickListener(this::pushButtonHandler);
		
		unixtime = findViewById(R.id.debug_unixtime_view);
		unixtime.setText(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));
		
		logView = findViewById(R.id.debug_text_view);
	}
	
	private void copyButtonHandler(final View view) {
		final ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
		
		if (clipboard != null) {
			final ClipData clip = android.content.ClipData.newPlainText("Copied Debug Text", logView.getText());
			clipboard.setPrimaryClip(clip);
			showToast("copied");
		}
	}
	
	private void clearDbButtonHandler(final View view) {
		LogUtil.clearFile(this, LogUtil.DB);
		logView.setText("");
	}
	
	private void dbButtonHandler(final View view) {
		String log = LogUtil.readFromFile(this, LogUtil.DB);
		logView.setText(log);
	}
	
	private void clearNetButtonHandler(final View view) {
		LogUtil.clearFile(this, LogUtil.NET);
		logView.setText("");
	}
	
	private void netButtonHandler(final View view) {
		String log = LogUtil.readFromFile(this, LogUtil.NET);
		logView.setText(log);
	}
	
	private void pushButtonHandler(final View view) {
		String log = LogUtil.readFromFile(this, LogUtil.PUSH);
		logView.setText(log);
	}
	
	private void scrollDown(final View view) {
		scrollView.fullScroll(View.FOCUS_DOWN);
	}
	
	private void showToast(final String text) {
		Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
		toast.show();
	}
}
