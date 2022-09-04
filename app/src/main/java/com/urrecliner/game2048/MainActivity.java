package com.urrecliner.game2048;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.urrecliner.game2048.Model.Tile;

public class MainActivity extends AppCompatActivity {

    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String SCORE = "score";
    private static final String HIGH_SCORE = "high score temp";
    private static final String MOVE = "move";
    private static final String HIGH_MOVE = "high move";
    private static final String UNDO_SCORE = "undo score";
    private static final String CAN_UNDO = "can undo";
    private static final String UNDO_GRID = "undo";
    private static final String GAME_STATE = "game state";
    private static final String UNDO_GAME_STATE = "undo game state";
    private MainView mainView;
    public static Activity mActivity;
    public static TextView tvHiScore, tvScore, tvMove, tvHiMove, tvTime, tvElapsed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        setContentView(R.layout.activity_main);
        tvHiScore = findViewById(R.id.highScore);
        tvScore = findViewById(R.id.score);
        tvHiMove = findViewById(R.id.highMoves);
        tvMove = findViewById(R.id.moves);
        tvTime = findViewById(R.id.nowTime);

        LayoutInflater inflater = getLayoutInflater();
        View theView = inflater.inflate(R.layout.layout_puzzle, null);
        mainView = theView.findViewById(R.id.puzzle_4box);

        SharedPreferences settings = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
        mainView.hasSaveState = settings.getBoolean("save_state", false);

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("hasState")) {
                load();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            //Do nothing
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            mainView.game.move(2);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            mainView.game.move(0);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            mainView.game.move(3);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            mainView.game.move(1);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("hasState", true);
        save();
    }

    protected void onPause() {
        super.onPause();
        save();
    }

    private void save() {
        SharedPreferences settings = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        Tile[][] field = mainView.game.grid.field;
        Tile[][] undoField = mainView.game.grid.undoField;
        editor.putInt(WIDTH, field.length);
        editor.putInt(HEIGHT, field.length);
        for (int xx = 0; xx < field.length; xx++) {
            for (int yy = 0; yy < field[0].length; yy++) {
                if (field[xx][yy] != null) {
                    editor.putInt(xx + " " + yy, field[xx][yy].getValue());
                } else {
                    editor.putInt(xx + " " + yy, 0);
                }

                if (undoField[xx][yy] != null) {
                    editor.putInt(UNDO_GRID + xx + " " + yy, undoField[xx][yy].getValue());
                } else {
                    editor.putInt(UNDO_GRID + xx + " " + yy, 0);
                }
            }
        }
        editor.putLong(SCORE, mainView.game.score);
        editor.putLong(HIGH_SCORE, mainView.game.highScore);
        editor.putLong(UNDO_SCORE, mainView.game.lastScore);
        editor.putBoolean(CAN_UNDO, mainView.game.canUndo);
        editor.putInt(MOVE, mainView.game.moves);
        editor.putInt(HIGH_MOVE, mainView.game.highMoves);
        editor.putInt(GAME_STATE, mainView.game.gameState);
        editor.putInt(UNDO_GAME_STATE, mainView.game.lastGameState);
        editor.apply();
    }

    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        //Stopping all animations
        mainView.game.aGrid.cancelAnimations();

        SharedPreferences settings = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
        for (int xx = 0; xx < mainView.game.grid.field.length; xx++) {
            for (int yy = 0; yy < mainView.game.grid.field[0].length; yy++) {
                int value = settings.getInt(xx + " " + yy, -1);
                if (value > 0) {
                    mainView.game.grid.field[xx][yy] = new Tile(xx, yy, value);
                } else if (value == 0) {
                    mainView.game.grid.field[xx][yy] = null;
                }

                int undoValue = settings.getInt(UNDO_GRID + xx + " " + yy, -1);
                if (undoValue > 0) {
                    mainView.game.grid.undoField[xx][yy] = new Tile(xx, yy, undoValue);
                } else if (value == 0) {
                    mainView.game.grid.undoField[xx][yy] = null;
                }
            }
        }

        mainView.game.score = settings.getLong(SCORE, mainView.game.score);
        mainView.game.highScore = settings.getLong(HIGH_SCORE, mainView.game.highScore);
        mainView.game.moves = settings.getInt(MOVE, mainView.game.moves);
        mainView.game.highMoves = settings.getInt(HIGH_MOVE, mainView.game.highMoves);
        mainView.game.lastScore = settings.getLong(UNDO_SCORE, mainView.game.lastScore);
        mainView.game.canUndo = settings.getBoolean(CAN_UNDO, mainView.game.canUndo);
        mainView.game.gameState = settings.getInt(GAME_STATE, mainView.game.gameState);
        mainView.game.lastGameState = settings.getInt(UNDO_GAME_STATE, mainView.game.lastGameState);
    }
}