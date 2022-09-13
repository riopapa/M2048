package com.urrecliner.game2048;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.urrecliner.game2048.Model.Tile;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import static com.urrecliner.game2048.Vars.CAN_UNDO;
import static com.urrecliner.game2048.Vars.GAME_STATE;
import static com.urrecliner.game2048.Vars.HIGH_MOVE;
import static com.urrecliner.game2048.Vars.HIGH_SCORE;
import static com.urrecliner.game2048.Vars.MOVE;
import static com.urrecliner.game2048.Vars.SCORE;
import static com.urrecliner.game2048.Vars.UNDO_GAME_STATE;
import static com.urrecliner.game2048.Vars.UNDO_GRID;
import static com.urrecliner.game2048.Vars.UNDO_SCORE;
import static com.urrecliner.game2048.Vars.aGrid;
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
import static com.urrecliner.game2048.Vars.tvHiScore;
import static com.urrecliner.game2048.Vars.tvHiMove;
import static com.urrecliner.game2048.Vars.tvScore;
import static com.urrecliner.game2048.Vars.tvMove;
import static com.urrecliner.game2048.Vars.tvTime;
import static com.urrecliner.game2048.Vars.settings;
import static com.urrecliner.game2048.Vars.editor;
import static com.urrecliner.game2048.Vars.mainView;

public class MainActivity extends AppCompatActivity {

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
        prepareBoxView();

        settings = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);   //
        editor = settings.edit();
        mainView.hasSaveState = settings.getBoolean("save_state", false);

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("hasState")) {
                load();
            }
        }

        ImageView ivAI = findViewById(R.id.ai);
        ivAI.setOnClickListener(view -> Observable.create(subscriber -> subscriber.onNext(mainView.game.getAIMove())).subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<Object>() {
                @Override
                public void call(Object move) {
                    mainView.game.move((int) move);
                }
            }));

        ImageView ivNew = findViewById(R.id.new_game);
        ivNew.setOnClickListener(view -> {
            if (!mainView.game.gameLost()) {
                new AlertDialog.Builder(mainView.getContext())
                        .setPositiveButton(R.string.reset, (dialog, which) -> mainView.game.newGame())
                        .setNegativeButton(R.string.continue_game, null)
                        .setTitle(R.string.reset_dialog_title)
                        .setMessage(R.string.reset_dialog_message)
                        .show();
            } else {
                mainView.game.newGame();
            }
        });

        ImageView ivUndo = findViewById(R.id.un_do);
        ivUndo.setOnClickListener(view -> mainView.game.revertUndoState());


    }

    private void prepareBoxView() {
        LayoutInflater inflater = getLayoutInflater();
        View theView = inflater.inflate(R.layout.layout_puzzle, null);
        mainView = theView.findViewById(R.id.puzzle_4box);
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

        Tile[][] field = grid.field;
        Tile[][] undoField = grid.undoField;
//        editor.putInt(WIDTH, field.length);
//        editor.putInt(HEIGHT, field.length);

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
        editor.putLong(SCORE, score);
        editor.putLong(HIGH_SCORE+"T", highScore);
        editor.putLong(UNDO_SCORE, lastScore);
        editor.putBoolean(CAN_UNDO, canUndo);
        editor.putInt(HIGH_MOVE+"T", highMoves);
        editor.putInt(MOVE, moves);
        editor.putInt(GAME_STATE, gameState);
        editor.putInt(UNDO_GAME_STATE, lastGameState);
        editor.apply();
    }

    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        //Stopping all animations
        aGrid.cancelAnimations();

        for (int xx = 0; xx < grid.field.length; xx++) {
            for (int yy = 0; yy < grid.field[0].length; yy++) {
                int value = settings.getInt(xx + " " + yy, -1);
                if (value > 0) {
                    grid.field[xx][yy] = new Tile(xx, yy, value);
                } else if (value == 0) {
                    grid.field[xx][yy] = null;
                }

                int undoValue = settings.getInt(UNDO_GRID + xx + " " + yy, -1);
                if (undoValue > 0) {
                    grid.undoField[xx][yy] = new Tile(xx, yy, undoValue);
                } else if (value == 0) {
                    grid.undoField[xx][yy] = null;
                }
            }
        }

        score = settings.getLong(SCORE, score);
        highScore = settings.getLong(HIGH_SCORE+"T", highScore);
        moves = settings.getInt(MOVE, moves);
        highMoves = settings.getInt(HIGH_MOVE+"T", highMoves);
        lastScore = settings.getLong(UNDO_SCORE, lastScore);
        canUndo = settings.getBoolean(CAN_UNDO, canUndo);
        gameState = settings.getInt(GAME_STATE, gameState);
        lastGameState = settings.getInt(UNDO_GAME_STATE, lastGameState);
    }

}