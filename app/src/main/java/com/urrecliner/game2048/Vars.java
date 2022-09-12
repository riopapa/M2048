package com.urrecliner.game2048;

import android.app.Activity;
import android.content.SharedPreferences;
import android.widget.TextView;

import com.urrecliner.game2048.Animation.AnimationGrid;
import com.urrecliner.game2048.Model.Grid;

public class Vars {
    static final String WIDTH = "width";
    static final String HEIGHT = "height";
    static final String SCORE = "score";
    static final String HIGH_SCORE = "high score";
    static final String MOVE = "move";
    static final String HIGH_MOVE = "high move";
    static final String UNDO_SCORE = "undo score";
    static final String CAN_UNDO = "can undo";
    static final String UNDO_GRID = "undo";
    static final String GAME_STATE = "game state";
    static final String UNDO_GAME_STATE = "undo game state";
    static MainView mainView;
    static Activity mActivity;
    static TextView tvHiScore, tvScore, tvMove, tvHiMove, tvTime, tvElapsed;
    static SharedPreferences settings;
    static SharedPreferences.Editor editor;

    /* from MainGame */

    static final int GAME_WIN = 1;
    static final int GAME_LOST = -1;
    static final int GAME_NORMAL = 0;
    static final int GAME_ENDLESS = 2;
    static final int GAME_ENDLESS_WON = 3;

    static int gameState = GAME_NORMAL;
    static int lastGameState = GAME_NORMAL;
    static int bufferGameState = GAME_NORMAL;

    static Grid grid = null;
    static AnimationGrid aGrid;
    static boolean canUndo;


    static long score = 0;
    static int moves = 0;
    static long highScore = 0;
    static int highMoves = 0;
    static long lastScore = 0;
    static long bufferScore = 0;

}