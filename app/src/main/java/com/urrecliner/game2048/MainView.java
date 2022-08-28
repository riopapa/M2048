package com.urrecliner.game2048;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

import com.urrecliner.game2048.Animation.AnimationCell;
import com.urrecliner.game2048.Model.Tile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

/** view*/
//@SuppressWarnings("deprecation")
public class MainView extends View {

    //Internal Constants
    static final int BASE_ANIMATION_TIME = 100000000;
    private static final String TAG = MainView.class.getSimpleName();
    private static final float MERGING_ACCELERATION = (float) -0.5;
    private static final float INITIAL_VELOCITY = (1 - MERGING_ACCELERATION) / 4;
    public final int numCellTypes = 21;//Kind of chess piece
    private final BitmapDrawable[] bitmapCell = new BitmapDrawable[numCellTypes];
    public final MainGame game;
    //Internal variables
    private final Paint paint = new Paint();
    public boolean hasSaveState = false;
    public boolean continueButtonEnabled = false;
    //网格尺寸
    public int startingX;
    public int startingY;
    public int endingX;
    public int endingY;
    //Icon variables
    public int sYIcons;
    public int sXNewGame;
    public int sXAI;
    public int sXUndo;
    public int iconSize;
    //Misc
    boolean refreshLastTime = true;
    //Timing
    private long lastFPSTime = System.nanoTime();
    //Text
    private float titleTextSize;//title font size
    private float bodyTextSize;//Score column size
    private float headerTextSize;//title font size
    private float instructionsTextSize;//Game description font size
    private float gameOverTextSize;//game over font
    private int cellSize = 0;//方块大小
    private float textSize = 0;//字体大小
    private float cellTextSize = 0;//方块字体大小
    private int gridWidth = 0;//分割线宽度
    private int textPaddingSize;//分数区域大小
    private int iconPaddingSize;//按钮区域大小
    //Assets
    private Drawable backgroundRectangle;
    private Drawable lightUpRectangle;
    private Drawable fadeRectangle;
    private Bitmap background = null;
    private BitmapDrawable loseGameOverlay;
    private BitmapDrawable winGameContinueOverlay;
    private BitmapDrawable winGameFinalOverlay;
    //Text variables
    private int sYAll;
    private int titleStartYAll;
    private int bodyStartYAll;
    private int eYAll;
    private int titleWidthHighScore;
    private int titleWidthScore;

    public MainView(Context context) {
        super(context);

        Resources resources = context.getResources();
        //Loading resources
        game = new MainGame(context, this);
        try {
            //Getting assets
            backgroundRectangle = resources.getDrawable(R.drawable.background_rectangle);
            lightUpRectangle = resources.getDrawable(R.drawable.light_up_rectangle);
            fadeRectangle = resources.getDrawable(R.drawable.fade_rectangle);
            this.setBackgroundColor(resources.getColor(R.color.background));
//            Typeface font = Typeface.createFromAsset(resources.getAssets(), "ClearSans-Bold.ttf");

//            paint.setTypeface(font);
            paint.setTypeface(context.getResources().getFont(R.font.nanumbarungothic));
            paint.setAntiAlias(true);
        } catch (Exception e) {
            Log.e(TAG, "Error getting assets?", e);
        }
        setOnTouchListener(new InputListener(this));
        game.newGame();
    }

    private static int log2(int n) {
        if (n <= 0) throw new IllegalArgumentException();
        return 31 - Integer.numberOfLeadingZeros(n);
    }

    @Override
    public void onDraw(Canvas canvas) {
        //Reset the transparency of the screen

        canvas.drawBitmap(background, 0, 0, paint);//draw background

        drawScoreText(canvas);//draw fractions

        if (!game.isActive() && !game.aGrid.isAnimationActive()) {
            drawNewGameButton(canvas, true);
        }
        //AI被开启
        if (!game.isActive() && !game.aGrid.isAnimationActive()) {
            drawAiButton(canvas, true);
        }
        drawCells(canvas);

        if (!game.isActive()) {
            drawEndGameState(canvas);
        }

        if (!game.canContinue()) {
            drawEndlessText(canvas);
        }

        //Animation drawing If there is still animation, continue to draw
        if (game.aGrid.isAnimationActive()) {
            invalidate(startingX, startingY, endingX, endingY);
            tick();
            //Refresh one last time on game end.
        } else if (!game.isActive() && refreshLastTime) {
            invalidate();
            refreshLastTime = false;
        }
    }

    /** when the screen size changes */
    @Override
    protected void onSizeChanged(int width, int height, int oldW, int oldH) {
        super.onSizeChanged(width, height, oldW, oldH);
        getLayout(width, height);
        createBitmapCells();//Create chess pieces
        createBackgroundBitmap(width, height);
        createOverlays();
    }

    private void createEndGameStates(Canvas canvas, boolean win, boolean showButton) {
        int width = endingX - startingX;
        int length = endingY - startingY;
        int middleX = width / 2;
        int middleY = length / 2;
        if (win) {
            lightUpRectangle.setAlpha(127);
            drawDrawable(canvas, lightUpRectangle, 0, 0, width, length);
            lightUpRectangle.setAlpha(255);
            paint.setColor(getResources().getColor(R.color.text_white));
            paint.setAlpha(255);
            paint.setTextSize(gameOverTextSize);
            paint.setTextAlign(Paint.Align.CENTER);
            int textBottom = middleY - centerText();
            canvas.drawText(getResources().getString(R.string.you_win), middleX, textBottom, paint);
            paint.setTextSize(bodyTextSize);
            String text = showButton ? getResources().getString(R.string.go_on) :
                    getResources().getString(R.string.for_now);
            canvas.drawText(text, middleX, textBottom + textPaddingSize * 2 - centerText() * 2, paint);
        } else {
            fadeRectangle.setAlpha(127);
            drawDrawable(canvas, fadeRectangle, 0, 0, width, length);
            fadeRectangle.setAlpha(255);
            paint.setColor(getResources().getColor(R.color.text_black));
            paint.setAlpha(255);
            paint.setTextSize(gameOverTextSize);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(getResources().getString(R.string.game_over), middleX, middleY - centerText(), paint);
        }
    }

    /**create cell bitmap */
    private void createBitmapCells() {
        Resources resources = getResources();
        int[] cellRectangleIds = getCellRectangleIds();
        paint.setTextAlign(Paint.Align.CENTER);
        for (int xx = 1; xx < bitmapCell.length; xx++) {
            int value = (int) Math.pow(2, xx);
            paint.setTextSize(cellTextSize);
            float tempTextSize = cellTextSize * cellSize * 0.9f / Math.max(cellSize * 0.9f, paint.measureText(String.valueOf(value)));
            paint.setTextSize(tempTextSize);
            Bitmap bitmap = Bitmap.createBitmap(cellSize, cellSize, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawDrawable(canvas, resources.getDrawable(cellRectangleIds[xx]), 0, 0, cellSize, cellSize);
            drawCellText(canvas, value);
            bitmapCell[xx] = new BitmapDrawable(resources, bitmap);
        }
    }

    private void createOverlays() {
        Resources resources = getResources();
        //Initialize overlays
        Bitmap bitmap = Bitmap.createBitmap(endingX - startingX, endingY - startingY, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        createEndGameStates(canvas, true, true);
        winGameContinueOverlay = new BitmapDrawable(resources, bitmap);
        bitmap = Bitmap.createBitmap(endingX - startingX, endingY - startingY, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        createEndGameStates(canvas, true, false);
        winGameFinalOverlay = new BitmapDrawable(resources, bitmap);
        bitmap = Bitmap.createBitmap(endingX - startingX, endingY - startingY, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        createEndGameStates(canvas, false, false);
        loseGameOverlay = new BitmapDrawable(resources, bitmap);
    }

    /** Create and draw control background files */
    private void createBackgroundBitmap(int width, int height) {
        background = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(background);
        drawHeader(canvas);
        drawNewGameButton(canvas, false);
        drawUndoButton(canvas);
        drawAiButton(canvas,false);
        drawBackground(canvas);
        drawBackgroundGrid(canvas);
        drawInstructions(canvas);
    }

    /** Drawing on a chessboard */
    private void drawDrawable(Canvas canvas, Drawable draw, int startingX, int startingY, int endingX, int endingY) {
        draw.setBounds(startingX, startingY, endingX, endingY);
        draw.draw(canvas);
    }

    /** Draw chess piece text */
    private void drawCellText(Canvas canvas, int value) {
        int textShiftY = centerText();//Offset up and down
        if (value >= 64) {
            paint.setColor(getResources().getColor(R.color.teal_700));
        } else if (value >= 8) {
            paint.setColor(getResources().getColor(R.color.text_white));
        } else {
            paint.setColor(getResources().getColor(R.color.text_black));
        }
        canvas.drawText("" + value, cellSize / 2, cellSize / 2 - textShiftY, paint);//draw numbers in the center
    }

    /** draw fractions */
    private void drawScoreText(Canvas canvas) {
        //Drawing the score text: Ver 2
        paint.setTextSize(bodyTextSize);
        paint.setTextAlign(Paint.Align.CENTER);

        int bodyWidthHighScore = (int) (paint.measureText("" + game.highScore));
        int bodyWidthScore = (int) (paint.measureText("" + game.score));

        /**adaptation */
        int textWidthHighScore = Math.max(titleWidthHighScore, bodyWidthHighScore) + textPaddingSize * 2;
        int textWidthScore = Math.max(titleWidthScore, bodyWidthScore) + textPaddingSize * 2;

        int textMiddleHighScore = textWidthHighScore / 2;
        int textMiddleScore = textWidthScore / 2;

        int eXHighScore = endingX;
        int sXHighScore = eXHighScore - textWidthHighScore;

        int eXScore = sXHighScore - textPaddingSize;
        int sXScore = eXScore - textWidthScore;

        //Outputting high-scores box
        backgroundRectangle.setBounds(sXHighScore, sYAll, eXHighScore, eYAll);
        backgroundRectangle.draw(canvas);
        paint.setTextSize(titleTextSize);
        paint.setColor(getResources().getColor(R.color.text_brown));
        canvas.drawText(getResources().getString(R.string.high_score), sXHighScore + textMiddleHighScore, titleStartYAll, paint);
        paint.setTextSize(bodyTextSize);
        paint.setColor(getResources().getColor(R.color.text_white));
        canvas.drawText(String.valueOf(game.highScore), sXHighScore + textMiddleHighScore, bodyStartYAll, paint);


        //Outputting scores box
        backgroundRectangle.setBounds(sXScore, sYAll, eXScore, eYAll);
        backgroundRectangle.draw(canvas);
        paint.setTextSize(titleTextSize);
        paint.setColor(getResources().getColor(R.color.text_brown));
        canvas.drawText(getResources().getString(R.string.score), sXScore + textMiddleScore, titleStartYAll, paint);
        paint.setTextSize(bodyTextSize);
        paint.setColor(getResources().getColor(R.color.text_white));
        canvas.drawText(String.valueOf(game.score), sXScore + textMiddleScore, bodyStartYAll, paint);

        SimpleDateFormat timeStamp = new SimpleDateFormat("HH:mm", Locale.KOREA);
        String nowTime = timeStamp.format(System.currentTimeMillis());
        paint.setTextSize(bodyTextSize+bodyTextSize/2);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(getResources().getColor(R.color.text_brown));
        canvas.drawText(nowTime, sXHighScore+20, titleStartYAll-100, paint);

    }

    /** Draw new game button */
    private void drawNewGameButton(Canvas canvas, boolean lightUp) {

        if (lightUp) {
            /** flag bit unavailable */
            drawDrawable(canvas,
                    lightUpRectangle,
                    sXNewGame,
                    sYIcons,
                    sXNewGame + iconSize,
                    sYIcons + iconSize
            );
        } else {
            drawDrawable(canvas,
                    backgroundRectangle,
                    sXNewGame,
                    sYIcons, sXNewGame + iconSize,
                    sYIcons + iconSize
            );
        }

        drawDrawable(canvas,
                getResources().getDrawable(R.drawable.ic_action_refresh),
                sXNewGame + iconPaddingSize,
                sYIcons + iconPaddingSize,
                sXNewGame + iconSize - iconPaddingSize,
                sYIcons + iconSize - iconPaddingSize
        );
    }

    /** draw back button */
    private void drawUndoButton(Canvas canvas) {

        /** draw background */
        drawDrawable(canvas,
                backgroundRectangle,
                sXUndo,
                sYIcons, sXUndo + iconSize,
                sYIcons + iconSize
        );

        /** draw back icon */
        drawDrawable(canvas,
                getResources().getDrawable(R.drawable.ic_action_undo),
                sXUndo + iconPaddingSize,
                sYIcons + iconPaddingSize,
                sXUndo + iconSize - iconPaddingSize,
                sYIcons + iconSize - iconPaddingSize
        );
    }

    private void drawAiButton(Canvas canvas,boolean lightUp){
        /** draw background */
        if (lightUp) {
            /** flag bit unavailable */
            drawDrawable(canvas,
                    lightUpRectangle,
                    sXAI,
                    sYIcons,
                    sXAI + iconSize,
                    sYIcons + iconSize
            );
        } else {
            drawDrawable(canvas,
                    backgroundRectangle,
                    sXAI,
                    sYIcons, sXAI + iconSize,
                    sYIcons + iconSize
            );
        }

        drawDrawable(canvas,
                getResources().getDrawable(R.drawable.ic_action_ai),
                sXAI + iconPaddingSize,
                sYIcons + iconPaddingSize,
                sXAI + iconSize - iconPaddingSize,
                sYIcons + iconSize - iconPaddingSize
        );
    }
    //draw the title
    private void drawHeader(Canvas canvas) {
        paint.setTextSize(headerTextSize);
        paint.setColor(getResources().getColor(R.color.text_black));
        paint.setTextAlign(Paint.Align.LEFT);
        int textShiftY = centerText() * 2;
        int headerStartY = sYAll - textShiftY;
        canvas.drawText(getResources().getString(R.string.header), startingX, headerStartY, paint);
    }

    //Drawing game reminders
    private void drawInstructions(Canvas canvas) {
        paint.setTextSize(instructionsTextSize);
        paint.setTextAlign(Paint.Align.LEFT);
        int textShiftY = centerText() * 2;
        canvas.drawText(getResources().getString(R.string.instructionsone),
                startingX, endingY - textShiftY + textPaddingSize, paint);

        paint.setTextSize(instructionsTextSize);
        paint.setTextAlign(Paint.Align.LEFT);
        textShiftY = centerText() * 8;
        canvas.drawText(getResources().getString(R.string.instructionstwo),
                startingX, endingY - textShiftY + textPaddingSize, paint);

        paint.setTextSize(instructionsTextSize);
        paint.setTextAlign(Paint.Align.LEFT);
        textShiftY = centerText() * 14;
        canvas.drawText(getResources().getString(R.string.instructionsthree),
                startingX, endingY - textShiftY + textPaddingSize, paint);

    }

    //draw checkerboard background color
    private void drawBackground(Canvas canvas) {
        drawDrawable(canvas, backgroundRectangle, startingX, startingY, endingX, endingY);
    }

    /** Draw chess pieces background square 16 */
    private void drawBackgroundGrid(Canvas canvas) {
        Resources resources = getResources();
        Drawable backgroundCell = resources.getDrawable(R.drawable.cell_rectangle);
        // Outputting the game grid
        for (int xx = 0; xx < game.numSquaresX; xx++) {
            for (int yy = 0; yy < game.numSquaresY; yy++) {
                int sX = startingX + gridWidth + (cellSize + gridWidth) * xx;
                int eX = sX + cellSize;
                int sY = startingY + gridWidth + (cellSize + gridWidth) * yy;
                int eY = sY;
                drawDrawable(canvas, backgroundCell, sX, sY, eX, eY);
            }
        }
    }

    /** drawing chess pieces */
    private void drawCells(Canvas canvas) {
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(getResources().getColor(R.color.cell_blank));
        // Outputting the individual cells
        for (int yy = 0; yy < game.numSquaresY; yy++) {
            for (int xx = 0; xx < game.numSquaresX; xx++) {
                int sX = startingX + gridWidth + (cellSize + gridWidth) * xx;
                int eX = sX + cellSize;
                int sY = startingY + gridWidth + (cellSize + gridWidth) * yy;
                int eY = sY + cellSize;

                Tile currentTile = game.grid.getCellContent(xx, yy);
                if (currentTile != null) {
                    //Get and represent the value of the tile
                    int value = currentTile.getValue();
                    int index = log2(value);
                    //Check for any active animations
                    ArrayList<AnimationCell> aArray = game.aGrid.getAnimationCell(xx, yy);
                    boolean animated = false;
                    for (int i = aArray.size() - 1; i >= 0; i--) {
                        AnimationCell aCell = aArray.get(i);
                        //If this animation is not active, skip it
                        if (aCell.getAnimationType() == MainGame.SPAWN_ANIMATION) {
                            animated = true;
                        }
                        if (!aCell.isActive()) {
                            continue;
                        }

                        if (aCell.getAnimationType() == MainGame.SPAWN_ANIMATION) { // Spawning animation
                            double percentDone = aCell.getPercentageDone();
                            float textScaleSize = (float) (percentDone);
                            paint.setTextSize(textSize * textScaleSize);

                            float cellScaleSize = cellSize / 2 * (1 - textScaleSize);
                            bitmapCell[index].setBounds((int) (sX + cellScaleSize), (int) (sY + cellScaleSize), (int) (eX - cellScaleSize), (int) (eY - cellScaleSize));
                            bitmapCell[index].draw(canvas);
                        } else if (aCell.getAnimationType() == MainGame.MERGE_ANIMATION) { // Merging Animation
                            double percentDone = aCell.getPercentageDone();
                            float textScaleSize = (float) (1 + INITIAL_VELOCITY * percentDone
                                    + MERGING_ACCELERATION * percentDone * percentDone / 2);
                            paint.setTextSize(textSize * textScaleSize);

                            float cellScaleSize = cellSize / 2 * (1 - textScaleSize);
                            bitmapCell[index].setBounds((int) (sX + cellScaleSize), (int) (sY + cellScaleSize), (int) (eX - cellScaleSize), (int) (eY - cellScaleSize));
                            bitmapCell[index].draw(canvas);
                        } else if (aCell.getAnimationType() == MainGame.MOVE_ANIMATION) {  // Moving animation
                            double percentDone = aCell.getPercentageDone();
                            int tempIndex = index;
                            if (aArray.size() >= 2) {
                                tempIndex = tempIndex - 1;
                            }
                            int previousX = aCell.extras[0];
                            int previousY = aCell.extras[1];
                            int currentX = currentTile.getX();
                            int currentY = currentTile.getY();
                            int dX = (int) ((currentX - previousX) * (cellSize + gridWidth) * (percentDone - 1) * 1.0);
                            int dY = (int) ((currentY - previousY) * (cellSize + gridWidth) * (percentDone - 1) * 1.0);
                            bitmapCell[tempIndex].setBounds(sX + dX, sY + dY, eX + dX, eY + dY);
                            bitmapCell[tempIndex].draw(canvas);
                        }
                        animated = true;
                    }

                    //No active animations? Just draw the cell
                    if (!animated) {
                        bitmapCell[index].setBounds(sX, sY, eX, eY);
                        bitmapCell[index].draw(canvas);
                    }
                    if (xx > 0)
                        checkLeftSame(canvas, xx, yy, value);
                    if (yy > 0)
                        checkUpSame(canvas, xx, yy, value);
                } else {
                    canvas.drawRoundRect(sX, sY, eX, eY, gridWidth, gridWidth, paint);
                }
            }
        }
    }

    private void checkLeftSame(Canvas canvas, int xx, int yy, int value) {
        Tile nextTile = game.grid.getCellContent(xx-1, yy);
        if (nextTile != null) {
            if (value == nextTile.getValue()) {
                drawConnection(canvas, xx, yy, value, true);
            }
        }
    }

    private void checkUpSame(Canvas canvas, int xx, int yy, int value) {
        Tile nextTile = game.grid.getCellContent(xx, yy-1);
        if (nextTile != null) {
            if (value == nextTile.getValue()) {
                drawConnection(canvas, xx, yy, value, false);
            }
        }
    }

    private void drawConnection(Canvas canvas, int xx, int yy, int value, boolean left) {
        int sX, eX, sY, eY;

        if (left) {
            sX = startingX + gridWidth + (cellSize + gridWidth) * xx + cellSize * 1/9;
            sY = startingY + gridWidth + (cellSize + gridWidth) * yy + cellSize / 2;
            eX = sX - gridWidth - cellSize * 2/9;
            eY = sY;
        } else {
            sX = startingX + gridWidth + (cellSize + gridWidth) * xx + cellSize / 2;
            sY = startingY + gridWidth + (cellSize + gridWidth) * yy + cellSize * 1/9;
            eX  = sX;
            eY = sY - gridWidth - cellSize * 2/9;
        }
        int index = log2(value) * 4;
        Paint paint = new Paint();
        paint.setColor(getResources().getColor(R.color.purple_500));
        paint.setStrokeWidth(2f);
        while (index >= 0) {
            if (left) {
                canvas.drawLine(sX, sY + index, eX, eY + index, paint);
                canvas.drawLine(sX, sY - index, eX, eY - index, paint);
            } else {
                canvas.drawLine(sX - index, sY, eX - index, eY, paint);
                canvas.drawLine(sX + index, sY, eX + index, eY, paint);
            }
            index -= 4;
        }
    }


    private void drawEndGameState(Canvas canvas) {
        double alphaChange = 1;
        continueButtonEnabled = false;
        for (AnimationCell animation : game.aGrid.globalAnimation) {
            if (animation.getAnimationType() == MainGame.FADE_GLOBAL_ANIMATION) {
                alphaChange = animation.getPercentageDone();
            }
        }
        BitmapDrawable displayOverlay = null;
        if (game.gameWon()) {
            if (game.canContinue()) {
                continueButtonEnabled = true;
                displayOverlay = winGameContinueOverlay;
            } else {
                displayOverlay = winGameFinalOverlay;
            }
        } else if (game.gameLost()) {
            displayOverlay = loseGameOverlay;
        }

        if (displayOverlay != null) {
            displayOverlay.setBounds(startingX, startingY, endingX, endingY);
            displayOverlay.setAlpha((int) (255 * alphaChange));
            displayOverlay.draw(canvas);
        }
    }

    private void drawEndlessText(Canvas canvas) {
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(bodyTextSize);
        paint.setColor(getResources().getColor(R.color.text_black));
        canvas.drawText(getResources().getString(R.string.endless), startingX, sYIcons - centerText() * 2, paint);
    }

    private void getLayout(int width, int height) {
        //格子大小
        cellSize = Math.min(width / (game.numSquaresX + 1), height / (game.numSquaresY + 3));
        //分割线宽度，从多出来的格子划分为7条分割线
        gridWidth = cellSize / 7;
        //中心坐标
        int screenMiddleX = width / 2;
        int screenMiddleY = height / 2;
        //棋盘y轴
        int boardMiddleY = screenMiddleY + cellSize / 2;
        //图标大小
        iconSize = cellSize / 2;

        //网格尺寸
        double halfNumSquaresX = game.numSquaresX / 2d;
        double halfNumSquaresY = game.numSquaresY / 2d;
        startingX = (int) (screenMiddleX - (cellSize + gridWidth) * halfNumSquaresX - gridWidth / 2);
        endingX = (int) (screenMiddleX + (cellSize + gridWidth) * halfNumSquaresX + gridWidth / 2);
        startingY = (int) (boardMiddleY - (cellSize + gridWidth) * halfNumSquaresY - gridWidth / 2);
        endingY = (int) (boardMiddleY + (cellSize + gridWidth) * halfNumSquaresY + gridWidth / 2);

        //游戏区长度
        float widthWithPadding = endingX - startingX;

        // 字体大小
        paint.setTextSize(cellSize);
        textSize = cellSize * cellSize / Math.max(cellSize, paint.measureText("0000"));//字符串宽度

        // 游戏说明字体大小
        paint.setTextAlign(Paint.Align.CENTER);
        //paint.setTextSize(1000);
        instructionsTextSize = Math.min(
                1000f * (widthWithPadding / (paint.measureText(getResources().getString(R.string.instructionsone)))),
                textSize / 2.5f
        );
        // 游戏结束字体大小屏幕适配
        gameOverTextSize = Math.min(
                Math.min(
                        1000f * ((widthWithPadding - gridWidth * 2) / (paint.measureText(getResources().getString(R.string.game_over)))),
                        textSize * 2
                ),
                1000f * ((widthWithPadding - gridWidth * 2) / (paint.measureText(getResources().getString(R.string.you_win))))
        );
        gameOverTextSize = gameOverTextSize * 3 / 4;

        paint.setTextSize(cellSize);
        cellTextSize = textSize;
        titleTextSize = textSize / 3;
        bodyTextSize = (int) (textSize / 1.5);
        headerTextSize = textSize * 2;

        //分数区域大小
        textPaddingSize = (int) (textSize / 3);
        //按钮区域大小
        iconPaddingSize = (int) (textSize/5);

        paint.setTextSize(titleTextSize);

        int textShiftYAll = centerText();
        //static variables
        sYAll = (int) (startingY - cellSize * 1.5);//title y axis
        titleStartYAll = (int) (sYAll + textPaddingSize + titleTextSize / 2 - textShiftYAll);//Fractional area y-axis
        bodyStartYAll = (int) (titleStartYAll + textPaddingSize + titleTextSize / 2 + bodyTextSize / 2);//Fractional y-axis

        titleWidthHighScore = (int) (paint.measureText(getResources().getString(R.string.high_score)));//high score
        titleWidthScore = (int) (paint.measureText(getResources().getString(R.string.score)));//score
        paint.setTextSize(bodyTextSize);
        textShiftYAll = centerText();

        eYAll = (int) (bodyStartYAll + textShiftYAll + bodyTextSize / 2 + textPaddingSize);

        sYIcons = (startingY + eYAll) / 2 - iconSize / 2;
        sXNewGame = (endingX - iconSize);
        sXUndo = sXNewGame - iconSize * 3 / 2 - iconPaddingSize;
        sXAI=sXUndo-iconSize*3/2-iconPaddingSize;
        resyncTime();
    }
    /** Get chess piece picture id*/
    private int[] getCellRectangleIds() {
        int[] cellRectangleIds = new int[numCellTypes];
        cellRectangleIds[0] = R.drawable.cell_rectangle;
        cellRectangleIds[1] = R.drawable.cell_rectangle_2;
        cellRectangleIds[2] = R.drawable.cell_rectangle_4;
        cellRectangleIds[3] = R.drawable.cell_rectangle_8;
        cellRectangleIds[4] = R.drawable.cell_rectangle_16;
        cellRectangleIds[5] = R.drawable.cell_rectangle_32;
        cellRectangleIds[6] = R.drawable.cell_rectangle_64;
        cellRectangleIds[7] = R.drawable.cell_rectangle_128;
        cellRectangleIds[8] = R.drawable.cell_rectangle_256;
        cellRectangleIds[9] = R.drawable.cell_rectangle_512;
        cellRectangleIds[10] = R.drawable.cell_rectangle_1024;
        cellRectangleIds[11] = R.drawable.cell_rectangle_2048;
        for (int xx = 12; xx < cellRectangleIds.length; xx++) {
            cellRectangleIds[xx] = R.drawable.cell_rectangle_4096;
        }
        return cellRectangleIds;
    }

    private void tick() {
        long currentTime = System.nanoTime();
        game.aGrid.tickAll(currentTime - lastFPSTime);
        lastFPSTime = currentTime;
    }

    public void resyncTime() {
        lastFPSTime = System.nanoTime();
    }

    /** font spacing */
    private int centerText() {
        return (int) ((paint.descent() + paint.ascent()) / 2);
    }

}