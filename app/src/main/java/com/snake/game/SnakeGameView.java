package com.snake.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SnakeGameView extends View {

    public enum Direction { UP, DOWN, LEFT, RIGHT }
    public enum GameState { IDLE, RUNNING, PAUSED, GAME_OVER }

    private static final int GRID_SIZE = 20;
    private int cellSize;
    private int gridWidth, gridHeight;
    private int offsetX, offsetY;

    private List<int[]> snake = new ArrayList<>();
    private int[] food = new int[2];
    private int[] bonusFood = null;
    private Direction currentDirection = Direction.RIGHT;
    private Direction nextDirection = Direction.RIGHT;
    private GameState gameState = GameState.IDLE;

    private int score = 0;
    private int highScore = 0;
    private int level = 1;
    private int speed = 150; // ms per tick

    private Handler handler = new Handler(Looper.getMainLooper());
    private Random random = new Random();
    private long bonusFoodTimer = 0;
    private boolean bonusFoodVisible = false;

    // Paints
    private Paint bgPaint = new Paint();
    private Paint gridPaint = new Paint();
    private Paint snakeHeadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint snakeBodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint snakeBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint foodPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint bonusFoodPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint overlayPaint = new Paint();

    private GameEventListener listener;
    private long lastUpdateTime = 0;

    private Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            if (gameState == GameState.RUNNING) {
                updateGame();
                invalidate();
                handler.postDelayed(this, speed);
            }
        }
    };

    public interface GameEventListener {
        void onScoreChanged(int score, int highScore, int level);
        void onGameOver(int score, int highScore);
        void onGameStarted();
    }

    public SnakeGameView(Context context) {
        super(context);
        setupPaints();
    }

    private void setupPaints() {
        bgPaint.setColor(Color.parseColor("#0A0E1A"));

        gridPaint.setColor(Color.parseColor("#1A1F2E"));
        gridPaint.setStrokeWidth(0.5f);
        gridPaint.setStyle(Paint.Style.STROKE);

        snakeHeadPaint.setColor(Color.parseColor("#00FF88"));
        snakeHeadPaint.setStyle(Paint.Style.FILL);

        snakeBodyPaint.setColor(Color.parseColor("#00CC66"));
        snakeBodyPaint.setStyle(Paint.Style.FILL);

        snakeBorderPaint.setColor(Color.parseColor("#004422"));
        snakeBorderPaint.setStyle(Paint.Style.STROKE);
        snakeBorderPaint.setStrokeWidth(1.5f);

        foodPaint.setColor(Color.parseColor("#FF4757"));
        foodPaint.setStyle(Paint.Style.FILL);
        foodPaint.setShadowLayer(8, 0, 0, Color.parseColor("#FF0000"));

        bonusFoodPaint.setColor(Color.parseColor("#FFD700"));
        bonusFoodPaint.setStyle(Paint.Style.FILL);
        bonusFoodPaint.setShadowLayer(12, 0, 0, Color.parseColor("#FF8C00"));

        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        scorePaint.setColor(Color.parseColor("#00FF88"));
        scorePaint.setTextAlign(Paint.Align.LEFT);
        scorePaint.setFakeBoldText(true);

        overlayPaint.setColor(Color.parseColor("#CC000000"));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int headerHeight = (int)(h * 0.10f);
        int gameAreaH = h - headerHeight;
        int gameAreaW = w;

        cellSize = Math.min(gameAreaW / GRID_SIZE, gameAreaH / GRID_SIZE);
        gridWidth = gameAreaW / cellSize;
        gridHeight = gameAreaH / cellSize;

        offsetX = (gameAreaW - gridWidth * cellSize) / 2;
        offsetY = headerHeight + (gameAreaH - gridHeight * cellSize) / 2;

        if (gameState == GameState.IDLE) {
            initGame();
        }
    }

    private void initGame() {
        snake.clear();
        score = 0;
        level = 1;
        speed = 150;
        currentDirection = Direction.RIGHT;
        nextDirection = Direction.RIGHT;
        bonusFood = null;
        bonusFoodVisible = false;

        int startX = gridWidth / 4;
        int startY = gridHeight / 2;
        for (int i = 4; i >= 0; i--) {
            snake.add(new int[]{startX - i, startY});
        }
        spawnFood();
    }

    private void spawnFood() {
        do {
            food[0] = random.nextInt(gridWidth);
            food[1] = random.nextInt(gridHeight);
        } while (isOccupied(food[0], food[1]));
    }

    private void spawnBonusFood() {
        int[] pos = new int[2];
        int attempts = 0;
        do {
            pos[0] = random.nextInt(gridWidth);
            pos[1] = random.nextInt(gridHeight);
            attempts++;
        } while (isOccupied(pos[0], pos[1]) && attempts < 50);
        if (attempts < 50) {
            bonusFood = pos;
            bonusFoodVisible = true;
            bonusFoodTimer = System.currentTimeMillis();
        }
    }

    private boolean isOccupied(int x, int y) {
        for (int[] seg : snake) {
            if (seg[0] == x && seg[1] == y) return true;
        }
        if (food[0] == x && food[1] == y) return true;
        return false;
    }

    private void updateGame() {
        currentDirection = nextDirection;

        int[] head = snake.get(snake.size() - 1);
        int newX = head[0];
        int newY = head[1];

        switch (currentDirection) {
            case UP:    newY--; break;
            case DOWN:  newY++; break;
            case LEFT:  newX--; break;
            case RIGHT: newX++; break;
        }

        // Wall collision
        if (newX < 0 || newX >= gridWidth || newY < 0 || newY >= gridHeight) {
            triggerGameOver();
            return;
        }

        // Self collision (skip tail since it moves)
        for (int i = 0; i < snake.size() - 1; i++) {
            if (snake.get(i)[0] == newX && snake.get(i)[1] == newY) {
                triggerGameOver();
                return;
            }
        }

        snake.add(new int[]{newX, newY});

        boolean ate = false;
        if (newX == food[0] && newY == food[1]) {
            score += 10 * level;
            ate = true;
            spawnFood();
            if (score % 100 == 0 && !bonusFoodVisible) {
                spawnBonusFood();
            }
            updateLevelAndSpeed();
            if (listener != null) listener.onScoreChanged(score, highScore, level);
        } else if (bonusFoodVisible && bonusFood != null && newX == bonusFood[0] && newY == bonusFood[1]) {
            score += 50 * level;
            ate = true;
            bonusFood = null;
            bonusFoodVisible = false;
            updateLevelAndSpeed();
            if (listener != null) listener.onScoreChanged(score, highScore, level);
        } else {
            snake.remove(0);
        }

        // Bonus food timeout (10 seconds)
        if (bonusFoodVisible && System.currentTimeMillis() - bonusFoodTimer > 10000) {
            bonusFood = null;
            bonusFoodVisible = false;
        }

        if (score > highScore) highScore = score;
    }

    private void updateLevelAndSpeed() {
        int newLevel = 1 + score / 150;
        if (newLevel != level) {
            level = Math.min(newLevel, 10);
            speed = Math.max(60, 150 - (level - 1) * 10);
        }
    }

    private void triggerGameOver() {
        gameState = GameState.GAME_OVER;
        handler.removeCallbacks(gameLoop);
        if (score > highScore) highScore = score;
        if (listener != null) listener.onGameOver(score, highScore);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        // Background
        canvas.drawColor(Color.parseColor("#0A0E1A"));

        // Header
        drawHeader(canvas, w);

        // Grid
        drawGrid(canvas);

        // Snake
        drawSnake(canvas);

        // Food
        drawFood(canvas);

        // Overlay for IDLE / GAME_OVER
        if (gameState == GameState.IDLE || gameState == GameState.GAME_OVER) {
            drawOverlay(canvas, w, h);
        }
    }

    private void drawHeader(Canvas canvas, int w) {
        int headerH = offsetY - (int)(getHeight() * 0.10f) + (int)(getHeight() * 0.10f);

        scorePaint.setTextSize(offsetY * 0.35f);
        scorePaint.setColor(Color.parseColor("#00FF88"));
        canvas.drawText("SCORE: " + score, offsetX + cellSize * 0.3f, offsetY * 0.6f, scorePaint);

        scorePaint.setTextAlign(Paint.Align.CENTER);
        scorePaint.setColor(Color.parseColor("#FFD700"));
        canvas.drawText("LV " + level, w / 2f, offsetY * 0.6f, scorePaint);

        scorePaint.setTextAlign(Paint.Align.RIGHT);
        scorePaint.setColor(Color.parseColor("#7B8CDE"));
        canvas.drawText("BEST: " + highScore, w - offsetX - cellSize * 0.3f, offsetY * 0.6f, scorePaint);

        scorePaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawGrid(Canvas canvas) {
        for (int x = 0; x <= gridWidth; x++) {
            canvas.drawLine(offsetX + x * cellSize, offsetY,
                    offsetX + x * cellSize, offsetY + gridHeight * cellSize, gridPaint);
        }
        for (int y = 0; y <= gridHeight; y++) {
            canvas.drawLine(offsetX, offsetY + y * cellSize,
                    offsetX + gridWidth * cellSize, offsetY + y * cellSize, gridPaint);
        }

        // Border glow
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.parseColor("#00FF88"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        borderPaint.setAlpha(80);
        canvas.drawRect(offsetX, offsetY, offsetX + gridWidth * cellSize, offsetY + gridHeight * cellSize, borderPaint);
    }

    private void drawSnake(Canvas canvas) {
        float r = cellSize * 0.35f;
        for (int i = 0; i < snake.size(); i++) {
            int[] seg = snake.get(i);
            float left = offsetX + seg[0] * cellSize + 2;
            float top = offsetY + seg[1] * cellSize + 2;
            float right = left + cellSize - 4;
            float bottom = top + cellSize - 4;
            RectF rect = new RectF(left, top, right, bottom);

            boolean isHead = (i == snake.size() - 1);

            if (isHead) {
                snakeHeadPaint.setShadowLayer(10, 0, 0, Color.parseColor("#00FF88"));
                canvas.drawRoundRect(rect, r * 1.2f, r * 1.2f, snakeHeadPaint);

                // Eyes
                Paint eyePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                eyePaint.setColor(Color.parseColor("#0A0E1A"));
                float eyeR = cellSize * 0.08f;
                float eyeOffset = cellSize * 0.18f;
                switch (currentDirection) {
                    case RIGHT:
                        canvas.drawCircle(right - eyeOffset, top + eyeOffset * 1.2f, eyeR, eyePaint);
                        canvas.drawCircle(right - eyeOffset, bottom - eyeOffset * 1.2f, eyeR, eyePaint);
                        break;
                    case LEFT:
                        canvas.drawCircle(left + eyeOffset, top + eyeOffset * 1.2f, eyeR, eyePaint);
                        canvas.drawCircle(left + eyeOffset, bottom - eyeOffset * 1.2f, eyeR, eyePaint);
                        break;
                    case UP:
                        canvas.drawCircle(left + eyeOffset * 1.2f, top + eyeOffset, eyeR, eyePaint);
                        canvas.drawCircle(right - eyeOffset * 1.2f, top + eyeOffset, eyeR, eyePaint);
                        break;
                    case DOWN:
                        canvas.drawCircle(left + eyeOffset * 1.2f, bottom - eyeOffset, eyeR, eyePaint);
                        canvas.drawCircle(right - eyeOffset * 1.2f, bottom - eyeOffset, eyeR, eyePaint);
                        break;
                }
            } else {
                // Gradient body: brighter near head
                float ratio = (float) i / snake.size();
                int alpha = (int)(120 + 135 * ratio);
                snakeBodyPaint.setAlpha(alpha);
                canvas.drawRoundRect(rect, r, r, snakeBodyPaint);
                snakeBorderPaint.setAlpha(alpha);
                canvas.drawRoundRect(rect, r, r, snakeBorderPaint);
            }
        }
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    private void drawFood(Canvas canvas) {
        float cx = offsetX + food[0] * cellSize + cellSize / 2f;
        float cy = offsetY + food[1] * cellSize + cellSize / 2f;
        float foodR = cellSize * 0.38f;

        setLayerType(LAYER_TYPE_SOFTWARE, null);
        canvas.drawCircle(cx, cy, foodR, foodPaint);

        // Shine
        Paint shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shinePaint.setColor(Color.parseColor("#FFAAAA"));
        shinePaint.setAlpha(180);
        canvas.drawCircle(cx - foodR * 0.25f, cy - foodR * 0.3f, foodR * 0.2f, shinePaint);

        if (bonusFoodVisible && bonusFood != null) {
            float bx = offsetX + bonusFood[0] * cellSize + cellSize / 2f;
            float by = offsetY + bonusFood[1] * cellSize + cellSize / 2f;
            float pulse = (float)(Math.sin(System.currentTimeMillis() / 200.0) * 0.1 + 0.9);
            canvas.drawCircle(bx, by, foodR * 1.2f * pulse, bonusFoodPaint);

            // Star points
            Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            starPaint.setColor(Color.WHITE);
            starPaint.setAlpha(200);
            canvas.drawCircle(bx - foodR * 0.3f, by - foodR * 0.35f, foodR * 0.15f, starPaint);
        }
    }

    private void drawOverlay(Canvas canvas, int w, int h) {
        canvas.drawRect(0, 0, w, h, overlayPaint);

        float centerX = w / 2f;
        float centerY = h / 2f;

        if (gameState == GameState.GAME_OVER) {
            textPaint.setTextSize(h * 0.065f);
            textPaint.setColor(Color.parseColor("#FF4757"));
            canvas.drawText("GAME OVER", centerX, centerY - h * 0.12f, textPaint);

            textPaint.setTextSize(h * 0.04f);
            textPaint.setColor(Color.WHITE);
            canvas.drawText("Score: " + score, centerX, centerY - h * 0.04f, textPaint);
            canvas.drawText("Best: " + highScore, centerX, centerY + h * 0.02f, textPaint);

            textPaint.setTextSize(h * 0.032f);
            textPaint.setColor(Color.parseColor("#00FF88"));
            canvas.drawText("Tap to Play Again", centerX, centerY + h * 0.1f, textPaint);
        } else {
            textPaint.setTextSize(h * 0.08f);
            textPaint.setColor(Color.parseColor("#00FF88"));
            canvas.drawText("SNAKE", centerX, centerY - h * 0.1f, textPaint);

            textPaint.setTextSize(h * 0.032f);
            textPaint.setColor(Color.WHITE);
            canvas.drawText("Use swipes to control", centerX, centerY, textPaint);

            textPaint.setColor(Color.parseColor("#FFD700"));
            canvas.drawText("🍎 = 10pts  ⭐ = 50pts", centerX, centerY + h * 0.06f, textPaint);

            textPaint.setTextSize(h * 0.036f);
            textPaint.setColor(Color.parseColor("#00FF88"));
            canvas.drawText("Tap to Start", centerX, centerY + h * 0.14f, textPaint);
        }
    }

    // --- Public controls ---

    public void startGame() {
        initGame();
        gameState = GameState.RUNNING;
        handler.removeCallbacks(gameLoop);
        handler.post(gameLoop);
        if (listener != null) listener.onGameStarted();
    }

    public void pauseGame() {
        if (gameState == GameState.RUNNING) {
            gameState = GameState.PAUSED;
            handler.removeCallbacks(gameLoop);
            invalidate();
        }
    }

    public void resumeGame() {
        if (gameState == GameState.PAUSED) {
            gameState = GameState.RUNNING;
            handler.post(gameLoop);
        }
    }

    public void handleTap() {
        if (gameState == GameState.IDLE || gameState == GameState.GAME_OVER) {
            startGame();
        }
    }

    public void setDirection(Direction dir) {
        if (gameState != GameState.RUNNING) return;
        // Prevent reversing
        if (dir == Direction.UP && currentDirection == Direction.DOWN) return;
        if (dir == Direction.DOWN && currentDirection == Direction.UP) return;
        if (dir == Direction.LEFT && currentDirection == Direction.RIGHT) return;
        if (dir == Direction.RIGHT && currentDirection == Direction.LEFT) return;
        nextDirection = dir;
    }

    public GameState getGameState() { return gameState; }
    public int getScore() { return score; }
    public int getHighScore() { return highScore; }

    public void setGameEventListener(GameEventListener l) { this.listener = l; }
}
