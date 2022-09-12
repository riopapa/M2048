package com.urrecliner.game2048;

import static com.urrecliner.game2048.Vars.GAME_ENDLESS;
import static com.urrecliner.game2048.Vars.GAME_ENDLESS_WON;
import static com.urrecliner.game2048.Vars.GAME_LOST;
import static com.urrecliner.game2048.Vars.GAME_NORMAL;
import static com.urrecliner.game2048.Vars.GAME_WIN;
import static com.urrecliner.game2048.Vars.aGrid;
import static com.urrecliner.game2048.Vars.bufferGameState;
import static com.urrecliner.game2048.Vars.bufferScore;
import static com.urrecliner.game2048.Vars.canUndo;
import static com.urrecliner.game2048.Vars.gameState;
import static com.urrecliner.game2048.Vars.grid;
import static com.urrecliner.game2048.Vars.highMoves;
import static com.urrecliner.game2048.Vars.highScore;
import static com.urrecliner.game2048.Vars.lastGameState;
import static com.urrecliner.game2048.Vars.lastScore;
import static com.urrecliner.game2048.Vars.mActivity;
import static com.urrecliner.game2048.Vars.moves;
import static com.urrecliner.game2048.Vars.score;
import static com.urrecliner.game2048.Vars.tvHiMove;
import static com.urrecliner.game2048.Vars.tvMove;
import static com.urrecliner.game2048.Vars.tvScore;
import static com.urrecliner.game2048.Vars.tvTime;

import android.content.Context;
import android.widget.TextView;

import com.urrecliner.game2048.AI.AlphaBeta;
import com.urrecliner.game2048.Animation.AnimationGrid;
import com.urrecliner.game2048.Model.Cell;
import com.urrecliner.game2048.Model.Grid;
import com.urrecliner.game2048.Model.Tile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** game service */
public class MainGame {

    public static final int SPAWN_ANIMATION = -1;
    public static final int MOVE_ANIMATION = 0;
    public static final int MERGE_ANIMATION = 1;

    public static final int FADE_GLOBAL_ANIMATION = 0;
    private static final long MOVE_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME;
    private static final long SPAWN_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME;
    private static final long NOTIFICATION_DELAY_TIME = MOVE_ANIMATION_TIME + SPAWN_ANIMATION_TIME;
    private static final long NOTIFICATION_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME * 5;
    private static final int startingMaxValue = 2048;
    //Odd state = game is not active
    //Even state = game is active
    //Win state = active state + 1
    private static int endingMaxValue;
    final int numSquaresX = 4;
    final int numSquaresY = 4;  // @ha
    private final Context mContext;
    private final MainView mView;


    public AlphaBeta gameAI;
    public MainGame(Context context, MainView view) {
        mContext = context;
        mView = view;
        endingMaxValue = (int) Math.pow(2, view.numCellTypes - 1);
        gameAI=new AlphaBeta();
    }

    public void newGame() {
        if (grid == null) {
            grid = new Grid(numSquaresX, numSquaresY);
        } else {
            prepareUndoState();
            saveUndoState();
            grid.clearGrid();
        }

        aGrid = new AnimationGrid(numSquaresX, numSquaresY);

        moves = 0;
        score = 0;
        gameState = GAME_NORMAL;
        showStatics();
        addStartTiles();
        mView.refreshLastTime = true;
        mView.resyncTime();
        mView.invalidate();
    }

    void showStatics() {
        TextView tvHiScore = mActivity.findViewById(R.id.highScore);
        if (tvHiScore != null) {
            tvHiMove.setText(""+ highMoves);
            tvMove.setText(""+ moves);
            tvHiScore.setText(""+highScore);
            tvScore.setText(""+score);

            SimpleDateFormat timeStamp = new SimpleDateFormat("HH:mm", Locale.KOREA);
            tvTime.setText(timeStamp.format(System.currentTimeMillis()));
        }

    }
    private void addStartTiles() {
        int startTiles = 2;
        for (int xx = 0; xx < startTiles; xx++) {
            this.addRandomTile();
        }
    }

    private void addRandomTile() {
        if (grid.isCellsAvailable()) {
            int value = Math.random() < 0.9 ? 2 : 4;
            Tile tile = new Tile(grid.randomAvailableCell(), value);
            spawnTile(tile);
        }
    }

    private void spawnTile(Tile tile) {
        grid.insertTile(tile);
        aGrid.startAnimation(tile.getX(), tile.getY(), SPAWN_ANIMATION,
                SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null); //Direction: -1 = EXPANDING
    }

    private void prepareTiles() {
        for (Tile[] array : grid.field) {
            for (Tile tile : array) {
                if (grid.isCellOccupied(tile)) {
                    tile.setMergedFrom(null);
                }
            }
        }
    }

    private void moveTile(Tile tile, Cell cell) {
        grid.field[tile.getX()][tile.getY()] = null;
        grid.field[cell.getX()][cell.getY()] = tile;
        tile.updatePosition(cell);
    }

    private void saveUndoState() {
        grid.saveTiles();
        canUndo = true;
        lastScore = bufferScore;
        lastGameState = bufferGameState;
    }

    private void prepareUndoState() {
        grid.prepareSaveTiles();
        bufferScore = score;
        bufferGameState = gameState;
    }

    public void revertUndoState() {
        if (canUndo) {
            canUndo = false;
            aGrid.cancelAnimations();
            grid.revertTiles();
            score = lastScore;
            gameState = lastGameState;
            mView.refreshLastTime = true;
            mView.invalidate();
        }
    }

    public boolean gameWon() {
        return (gameState > 0 && gameState % 2 != 0);
    }

    public boolean gameLost() {
        return (gameState == GAME_LOST);
    }

    /**game is running */
    public boolean isActive() {
        return !(gameWon() || gameLost());
    }

    public void move(int direction) {
        aGrid.cancelAnimations();
        if (!isActive()) {
            return;
        }
        prepareUndoState();
        Cell vector = getVector(direction);
        List<Integer> traversalsX = buildTraversalsX(vector);
        List<Integer> traversalsY = buildTraversalsY(vector);
        boolean moved = false;
        prepareTiles();

        for (int xx : traversalsX) {
            for (int yy : traversalsY) {
                Cell cell = new Cell(xx, yy);
                Tile tile = grid.getCellContent(cell);

                if (tile != null) {
                    Cell[] positions = findFarthestPosition(cell, vector);
                    Tile next = grid.getCellContent(positions[1]);

                    if (next != null && next.getValue() == tile.getValue() && next.getMergedFrom() == null) {
                        Tile merged = new Tile(positions[1], tile.getValue() * 2);
                        Tile[] temp = {tile, next};
                        merged.setMergedFrom(temp);

                        grid.insertTile(merged);
                        grid.removeTile(tile);

                        // Converge the two tiles' positions
                        tile.updatePosition(positions[1]);

                        int[] extras = {xx, yy};
                        aGrid.startAnimation(merged.getX(), merged.getY(), MOVE_ANIMATION,
                                MOVE_ANIMATION_TIME, 0, extras); //Direction: 0 = MOVING MERGED
                        aGrid.startAnimation(merged.getX(), merged.getY(), MERGE_ANIMATION,
                                SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null);

                        // Update the score
                        score = score + merged.getValue();
                        if (score >= highScore) {
                            highScore = score;
                            highMoves = moves;
                        }

                        // The mighty 2048 tile
                        if (merged.getValue() >= winValue() && !gameWon()) {
                            gameState = gameState + GAME_WIN; // Set win state
                            endGame();
                        }
                    } else {
                        moveTile(tile, positions[0]);
                        int[] extras = {xx, yy, 0};
                        aGrid.startAnimation(positions[0].getX(), positions[0].getY(), MOVE_ANIMATION, MOVE_ANIMATION_TIME, 0, extras); //Direction: 1 = MOVING NO MERGE
                    }

                    if (!positionsEqual(cell, tile)) {
                        moved = true;
                    }
                }
            }
        }

        if (moved) {
            saveUndoState();
            addRandomTile();
            checkLose();
        }
        mView.resyncTime();
        mView.invalidate();
        showStatics();
    }

    private void checkLose() {
        if (!movesAvailable() && !gameWon()) {
            gameState = GAME_LOST;
            endGame();
        }
    }

    private void endGame() {
        aGrid.startAnimation(-1, -1, FADE_GLOBAL_ANIMATION, NOTIFICATION_ANIMATION_TIME, NOTIFICATION_DELAY_TIME, null);
        if (score >= highScore) {
            highScore = score;
            highMoves = moves;
        }
    }

    private Cell getVector(int direction) {
        Cell[] map = {
                new Cell(0, -1), // up
                new Cell(0, 1),  // down
                new Cell(-1, 0),  // left
                new Cell(1, 0)  // right

        };
        return map[direction];
    }

    private List<Integer> buildTraversalsX(Cell vector) {
        List<Integer> traversals = new ArrayList<>();

        for (int xx = 0; xx < numSquaresX; xx++) {
            traversals.add(xx);
        }
        if (vector.getX() == 1) {
            Collections.reverse(traversals);
        }

        return traversals;
    }

    private List<Integer> buildTraversalsY(Cell vector) {
        List<Integer> traversals = new ArrayList<>();

        for (int xx = 0; xx < numSquaresY; xx++) {
            traversals.add(xx);
        }
        if (vector.getY() == 1) {
            Collections.reverse(traversals);
        }

        return traversals;
    }

    private Cell[] findFarthestPosition(Cell cell, Cell vector) {
        Cell previous;
        Cell nextCell = new Cell(cell.getX(), cell.getY());
        do {
            previous = nextCell;
            nextCell = new Cell(previous.getX() + vector.getX(),
                    previous.getY() + vector.getY());
        } while (grid.isCellWithinBounds(nextCell) && grid.isCellAvailable(nextCell));

        return new Cell[]{previous, nextCell};
    }

    private boolean movesAvailable() {
        return grid.isCellsAvailable() || tileMatchesAvailable();
    }

    private boolean tileMatchesAvailable() {
        Tile tile;

        for (int xx = 0; xx < numSquaresX; xx++) {
            for (int yy = 0; yy < numSquaresY; yy++) {
                tile = grid.getCellContent(new Cell(xx, yy));

                if (tile != null) {
                    for (int direction = 0; direction < 4; direction++) {
                        Cell vector = getVector(direction);
                        Cell cell = new Cell(xx + vector.getX(), yy + vector.getY());

                        Tile other = grid.getCellContent(cell);

                        if (other != null && other.getValue() == tile.getValue()) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean positionsEqual(Cell first, Cell second) {
        return first.getX() == second.getX() && first.getY() == second.getY();
    }

    private int winValue() {
        if (!canContinue()) {
            return endingMaxValue;
        } else {
            return startingMaxValue;
        }
    }

    public void setEndlessMode() {
        gameState = GAME_ENDLESS;
        mView.invalidate();
        mView.refreshLastTime = true;
    }

    public boolean canContinue() {
        return !(gameState == GAME_ENDLESS || gameState == GAME_ENDLESS_WON);
    }

    public Object getAIMove(){
        return gameAI.SinglePlayGame(grid.field);
    }
}