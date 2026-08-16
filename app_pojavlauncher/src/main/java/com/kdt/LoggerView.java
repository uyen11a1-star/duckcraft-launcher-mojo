package com.kdt;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import net.kdt.pojavlaunch.Logger;
import git.artdeell.mojo.R;

import java.util.ArrayDeque;

/**
 * A class able to display logs to the user.
 * It has support for the Logger class.
 */
public class LoggerView extends ConstraintLayout {
    private static final int MAX_LOG_LINES = 2000;
    private static final long UI_BATCH_DELAY_MS = 50L;

    private Logger.eventLogListener mLogListener;
    private ToggleButton mLogToggle;
    private DefocusableScrollView mScrollView;
    private TextView mLogTextView;
    private final ArrayDeque<String> mPendingLogs = new ArrayDeque<>();
    private boolean mLogUpdateScheduled;

    public LoggerView(@NonNull Context context) {
        this(context, null);
    }

    public LoggerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        // Triggers the log view shown state by default when viewing it.
        mLogToggle.setChecked(visibility == VISIBLE);
    }

    /**
     * Inflate the layout, and add component behaviors.
     */
    private void init() {
        inflate(getContext(), R.layout.view_logger, this);
        mLogTextView = findViewById(R.id.content_log_view);
        mLogTextView.setTypeface(Typeface.MONOSPACE);
        mLogTextView.setMaxLines(MAX_LOG_LINES);
        mLogTextView.setEllipsize(null);
        mLogTextView.setVisibility(GONE);

        // Toggle log visibility.
        mLogToggle = findViewById(R.id.content_log_toggle_log);
        mLogToggle.setOnCheckedChangeListener(
                (compoundButton, isChecked) -> {
                    mLogTextView.setVisibility(isChecked ? VISIBLE : GONE);
                    if (isChecked) {
                        Logger.setLogListener(mLogListener);
                    } else {
                        synchronized (mPendingLogs) {
                            mPendingLogs.clear();
                        }
                        mLogTextView.setText("");
                        Logger.setLogListener(null); // Skip expensive JNI callbacks when hidden.
                    }
                });
        mLogToggle.setChecked(false);

        // Remove the loggerView from the user View.
        ImageButton cancelButton = findViewById(R.id.log_view_cancel);
        cancelButton.setOnClickListener(view -> LoggerView.this.setVisibility(GONE));

        // Set the scroll view.
        mScrollView = findViewById(R.id.content_log_scroll);
        mScrollView.setKeepFocusing(true);

        // Set up the autoscroll switch.
        ToggleButton autoscrollToggle = findViewById(R.id.content_log_toggle_autoscroll);
        autoscrollToggle.setOnCheckedChangeListener(
                (compoundButton, isChecked) -> {
                    if (isChecked) mScrollView.fullScroll(View.FOCUS_DOWN);
                    mScrollView.setKeepFocusing(isChecked);
                }
        );
        autoscrollToggle.setChecked(true);

        // Listen to logs. The native logger may call this listener from a
        // background thread, so coalesce updates before touching the TextView.
        mLogListener = text -> {
            if (mLogTextView.getVisibility() != VISIBLE) return;
            synchronized (mPendingLogs) {
                mPendingLogs.addLast(text);
                if (mLogUpdateScheduled) return;
                mLogUpdateScheduled = true;
            }
            postDelayed(this::flushPendingLogs, UI_BATCH_DELAY_MS);
        };
    }

    private void flushPendingLogs() {
        StringBuilder batch = new StringBuilder();
        synchronized (mPendingLogs) {
            while (!mPendingLogs.isEmpty()) {
                batch.append(mPendingLogs.removeFirst()).append('\n');
            }
            mLogUpdateScheduled = false;
        }

        if (batch.length() == 0 || mLogTextView.getVisibility() != VISIBLE) return;
        mLogTextView.append(batch.toString());
        trimLogText();
        if (mScrollView.isKeepFocusing()) mScrollView.fullScroll(View.FOCUS_DOWN);
    }

    private void trimLogText() {
        Layout layout = mLogTextView.getLayout();
        if (layout == null || layout.getLineCount() <= MAX_LOG_LINES) return;

        int firstLineToKeep = layout.getLineCount() - MAX_LOG_LINES;
        int firstCharToKeep = layout.getLineStart(firstLineToKeep);
        mLogTextView.getEditableText().delete(0, firstCharToKeep);
    }
}
