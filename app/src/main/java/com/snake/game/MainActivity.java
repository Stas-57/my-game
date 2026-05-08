package com.snake.game;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

public class MainActivity extends AppCompatActivity implements SnakeGameView.GameEventListener {

    private SnakeGameView snakeGameView;
    private GestureDetectorCompat gestureDetector;
    private ImageButton btnPause;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Build UI programmatically
        RelativeLayout root = new RelativeLayout(this);
        root.setBackgroundColor(0xFF0A0E1A);

        snakeGameView = new SnakeGameView(this);
        snakeGameView.setGameEventListener(this);

        RelativeLayout.LayoutParams gameParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );
        root.addView(snakeGameView, gameParams);

        // Pause button (top-right corner overlay)
        btnPause = new ImageButton(this);
        btnPause.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        btnPause.setImageResource(android.R.drawable.ic_media_pause);
        RelativeLayout.LayoutParams pauseParams = new RelativeLayout.LayoutParams(120, 120);
        pauseParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        pauseParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        pauseParams.setMargins(0, 40, 20, 0);
        root.addView(btnPause, pauseParams);

        btnPause.setOnClickListener(v -> {
            SnakeGameView.GameState state = snakeGameView.getGameState();
            if (state == SnakeGameView.GameState.RUNNING) {
                snakeGameView.pauseGame();
                btnPause.setImageResource(android.R.drawable.ic_media_play);
                showToast("Paused — tap ▶ to resume");
            } else if (state == SnakeGameView.GameState.PAUSED) {
                snakeGameView.resumeGame();
                btnPause.setImageResource(android.R.drawable.ic_media_pause);
            }
        });

        setContentView(root);

        // Gesture detector for swipes + tap
        gestureDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener() {

            private static final int SWIPE_THRESHOLD = 80;
            private static final int SWIPE_VELOCITY_THRESHOLD = 80;

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                snakeGameView.handleTap();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
                if (e1 == null || e2 == null) return false;
                float dX = e2.getX() - e1.getX();
                float dY = e2.getY() - e1.getY();

                if (Math.abs(dX) > Math.abs(dY)) {
                    if (Math.abs(dX) > SWIPE_THRESHOLD && Math.abs(vX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (dX > 0) snakeGameView.setDirection(SnakeGameView.Direction.RIGHT);
                        else        snakeGameView.setDirection(SnakeGameView.Direction.LEFT);
                        return true;
                    }
                } else {
                    if (Math.abs(dY) > SWIPE_THRESHOLD && Math.abs(vY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (dY > 0) snakeGameView.setDirection(SnakeGameView.Direction.DOWN);
                        else        snakeGameView.setDirection(SnakeGameView.Direction.UP);
                        return true;
                    }
                }
                return false;
            }
        });

        snakeGameView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }

    private void showToast(String msg) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onScoreChanged(int score, int highScore, int level) {
        // Score is drawn directly on the game canvas — nothing extra needed
    }

    @Override
    public void onGameOver(int score, int highScore) {
        runOnUiThread(() -> {
            btnPause.setImageResource(android.R.drawable.ic_media_pause);
        });
    }

    @Override
    public void onGameStarted() {
        runOnUiThread(() -> {
            btnPause.setImageResource(android.R.drawable.ic_media_pause);
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        snakeGameView.pauseGame();
        btnPause.setImageResource(android.R.drawable.ic_media_play);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Don't auto-resume; let user tap pause button
    }
}
