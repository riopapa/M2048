package com.urrecliner.game2048.AI;


import android.util.Log;

import com.urrecliner.game2048.Model.Tile;
import com.urrecliner.game2048.Util.AIUtil;

import java.util.HashMap;
import java.util.Map;
import static com.urrecliner.game2048.Util.AIUtil.*;



public class AlphaBeta {

    public long board;
    /* Move tables. Each row or compressed column is mapped to (oldrow^newrow) assuming row/col 0.
     *
     * Thus, the value is 0 if there is no move, and otherwise equals a value that can easily be
     * xor'ed into the current board state to update the board. */
    public static int [] row_left_table;
    public static int [] row_right_table;
    public static long [] col_up_table;
    public static long [] col_down_table;
    public static float [] heur_score_table;
    public static float [] score_table;


    // 得分
    static final float SCORE_LOST_PENALTY = 200000.0f;//失败
    static final float SCORE_MONOTONICITY_POWER = 4.0f;//单调性加权
    static final float SCORE_MONOTONICITY_WEIGHT = 47.0f;//单调性得分
    static final float SCORE_SUM_POWER = 3.5f;//总重量加权
    static final float SCORE_SUM_WEIGHT = 11.0f;//总重量
    static final float SCORE_MERGES_WEIGHT = 700.0f;//合并
    static final float SCORE_EMPTY_WEIGHT = 270.0f;//空

    public static void main(String[] args) {
        //测试转换
        int[][] field={
                {4,4,8,2},
                {0,2,4,32},
                {0,2,2,4},
                {2,2,2,2}
        };
        AlphaBeta gameAI=new AlphaBeta(field);
        print_board(gameAI.board);
        gameAI.PlayGame();
//        gameAI.execute_move_0(gameAI.board);
//        gameAI.execute_move_1(gameAI.board);
//        gameAI.execute_move_2(gameAI.board);
//        gameAI.execute_move_3(gameAI.board);
    }
    public AlphaBeta(){
        init_tables();
    }

    public AlphaBeta(int[][] field){
        board=TileArrayToLong(field);
        init_tables();
    }

    /**初始化启发值*/
    private void init_tables() {
        row_left_table = new int[65536];
        row_right_table = new int[65536];
        col_up_table = new long[65536];
        col_down_table = new long[65536];
        heur_score_table = new float[65536];
        score_table = new float[65536];

        for (int row = 0; row < 65536; ++row) {
            int []line = {
                    (row >> 0) & 0xf,
                    (row >> 4) & 0xf,
                    (row >> 8) & 0xf,
                    (row >> 12) & 0xf
            };

            // Score
            float score = 0.0f;
            for (int i = 0; i < 4; ++i) {
                int rank = line[i];
                if (rank >= 2) {
                    // the score is the total sum of the tile and all intermediate merged tiles
                    score += (rank - 1) * (1 << rank);
                }
            }
            score_table[row] = score;

            // Heuristic score
            float sum = 0;
            int empty = 0;
            int merges = 0;

            int prev = 0;
            int counter = 0;
            for (int i = 0; i < 4; ++i) {
                int rank = line[i];
                sum += Math.pow(rank, SCORE_SUM_POWER);
                if (rank == 0) {
                    empty++;
                } else {
                    if (prev == rank) {
                        counter++;
                    } else if (counter > 0) {
                        merges += 1 + counter;
                        counter = 0;
                    }
                    prev = rank;
                }
            }

            if (counter > 0) {
                merges += 1 + counter;
            }

            float monotonicity_left = 0;
            float monotonicity_right = 0;
            for (int i = 1; i < 4; ++i) {
                if (line[i - 1] > line[i]) {
                    monotonicity_left += Math.pow(line[i - 1], SCORE_MONOTONICITY_POWER) - Math.pow(line[i], SCORE_MONOTONICITY_POWER);
                } else {
                    monotonicity_right += Math.pow(line[i], SCORE_MONOTONICITY_POWER) - Math.pow(line[i - 1], SCORE_MONOTONICITY_POWER);
                }
            }

            heur_score_table[row] = SCORE_LOST_PENALTY +
                    SCORE_EMPTY_WEIGHT * empty +
                    SCORE_MERGES_WEIGHT * merges -
                    SCORE_MONOTONICITY_WEIGHT * Math.min(monotonicity_left, monotonicity_right) -
                    SCORE_SUM_WEIGHT * sum;

            // execute a move to the left
            for (int i = 0; i < 3; ++i) {
                int j;
                for (j = i + 1; j < 4; ++j) {
                    if (line[j] != 0) break;
                }
                if (j == 4) break; // no more tiles to the right

                if (line[i] == 0) {
                    line[i] = line[j];
                    line[j] = 0;
                    i--; // retry this entry
                } else if (line[i] == line[j]) {
                    if (line[i] != 0xf) {
                    /* Pretend that 32768 + 32768 = 32768 (representational limit). */
                        line[i]++;
                    }
                    line[j] = 0;
                }
            }

            int result = (line[0] << 0) |
                    (line[1] << 4) |
                    (line[2] << 8) |
                    (line[3] << 12);
            int rev_result = reverse_row(result);
            int rev_row = reverse_row(row);

            row_left_table[row] = row ^ result;
            row_right_table[rev_row] = rev_row ^ rev_result;
            col_up_table[row] = unpack_col(row) ^ unpack_col(result);
            col_down_table[rev_row] = unpack_col(rev_row) ^ unpack_col(rev_result);
        }
    }

    public int SinglePlayGame(Tile[][] field){

        board=TileArrayToLong(field);
        int moveno = 0;
        int scorepenalty = 0; // "penalty" for obtaining free 4 tiles

            int move;
            long newboard;
            for(move = 0; move < 4; move++) {
                if(execute_move(move, board) != board)
                    break;
            }
            if(move == 4)
                return -1; // no legal moves
            //++moveno;
            System.out.println("Move #"+(moveno)+", current score="+(score_board(board) - scorepenalty));

            //获得最好的move
            move = find_best_move(board);
            System.out.println("best move is:"+move);
            if(move < 0)
                return -1;
        return move;
    }

    public void PlayGame(){
        int moveno = 0;
        int scorepenalty = 0; // "penalty" for obtaining free 4 tiles

        while(true) {
            int move;
            long newboard;
            for(move = 0; move < 4; move++) {
                if(execute_move(move, board) != board)
                    break;
            }
            if(move == 4)
                break; // no legal moves
            ++moveno;
            System.out.println("Move #"+(moveno)+", current score="+(score_board(board) - scorepenalty));

            //获得最好的move
            move = find_best_move(board);
            if(move < 0)
                break;

            newboard = execute_move(move, board);
            //System.out.println("newboard............");
            print_board(newboard);
            if(newboard == board) {
                System.out.println("Illegal move!");
                moveno--;
                continue;
            }
            board=newboard;
//            long tile = draw_tile();
//            if (tile == 2) scorepenalty += 4;
//            board = insert_tile_rand(newboard, tile);
        }

        print_board(board);
        System.out.println("Game Over, Sorry");
    }

    private int find_best_move(long board) {
        int move;
        float best = 0;
        int bestmove = -1;

        print_board(board);
        //printf("Current scores: heur %.0f, actual %.0f\n", score_heur_board(board), score_board(board));

        for(move=0; move<4; move++) {
            float res = score_toplevel_move(board, move);
            if(res > best) {
                best = res;
                bestmove = move;
            }
        }

        return bestmove;
    }

    private float score_toplevel_move(long board, int move) {

        float res;
        long startTime;
        long finishTime;
        double elapsed;
        EvalState state=new EvalState();
        state.depth_limit = Math.max(3, count_distinct_tiles(board) - 2);

        startTime=System.nanoTime();
        res = _score_toplevel_move(state, board, move);
        finishTime=System.nanoTime();

        elapsed=(finishTime-startTime)/1000000000.0;//运行时间

        //move移动的方向
        //
        Log.e("message",String.format("Move %s: result %s: eval'd %s moves (%s cache hits, %s cache size) in %.3f seconds (maxdepth=%s)\n",move,res,
                state.moves_evaled,state.cachehits, (int)state.trans_table.size(), elapsed, state.maxdepth));
//        System.out.println(String.format("Move %s: result %s: eval'd %s moves (%s cache hits, %s cache size) in %.3f seconds (maxdepth=%s)\n",move,res,
//                state.moves_evaled,state.cachehits, (int)state.trans_table.size(), elapsed, state.maxdepth));
        return res;
    }

    static int count_distinct_tiles(long board) {
        int bitset = 0;
        while (board!=0) {
            bitset |= 1<<(board & 0xf);
            board >>= 4;
        }

        // Don't count empty tiles.
        bitset >>= 1;

        int count = 0;
        while (bitset!=0) {
            bitset &= bitset - 1;
            count++;
        }
        return count;
    }

    private float _score_toplevel_move(EvalState state, long board, int move) {
        long newboard = execute_move(move, board);
        if(board == newboard)
            return 0;

        return (float) (score_tilechoose_node(state, newboard, 1.0f) + 1e-6);
    }

    private float score_tilechoose_node(EvalState state, long board, float cprob) {
        if (cprob < CPROB_THRESH_BASE || state.curdepth >= state.depth_limit) {
            state.maxdepth = Math.max(state.curdepth, state.maxdepth);
            return score_heur_board(board);
        }

        if (state.curdepth < CACHE_DEPTH_LIMIT) {

            TransTableEntry object = state.trans_table.get(board);
            if (object != null) {
                TransTableEntry entry = object;
            /*
            return heuristic from transposition table only if it means that
            the node will have been evaluated to a minimum depth of state.depth_limit.
            This will result in slightly fewer cache hits, but should not impact the
            strength of the ai negatively.
            */

                if(entry.depth <= state.curdepth)
                {
                    state.cachehits++;
                    return entry.heuristic;
                }
            }
        }

        int num_open = count_empty(board);
        cprob /= num_open;

        float res = 0.0f;
        long tmp = board;
        long tile_2 = 1;
        while (tile_2!=0) {
            if ((tmp & 0xf) == 0) {
                res += score_move_node(state, board |  tile_2      , cprob * 0.9f) * 0.9f;
                res += score_move_node(state, board | (tile_2 << 1), cprob * 0.1f) * 0.1f;
            }
            tmp >>= 4;
            tile_2 <<= 4;
        }
        res = res / num_open;

        if (state.curdepth < CACHE_DEPTH_LIMIT) {
            TransTableEntry entry=new TransTableEntry();
            entry.depth=state.curdepth&0xFF;
            entry.heuristic=res;
            state.trans_table.put(board,entry);
        }

        return res;
    }

    private float score_move_node(EvalState state, long board, float cprob) {
        float best = 0.0f;
        state.curdepth++;
        for (int move = 0; move < 4; ++move) {
            long newboard = execute_move(move, board);
            state.moves_evaled++;

            if (board != newboard) {
                best = Math.max(best, score_tilechoose_node(state, newboard, cprob));
            }
        }
        state.curdepth--;
        return best;
    }

    static int count_empty(long x){
        x |= (x >> 2) & 0x3333333333333333L;
        x |= (x >> 1);
        x = ~x & 0x1111111111111111L;
        // At this point each nibble is:
        //  0 if the original nibble was non-zero
        //  1 if the original nibble was zero
        // Next sum them all
        x += x >> 32;
        x += x >> 16;
        x += x >>  8;
        x += x >>  4; // this can overflow to the next nibble if there were 16 empty positions
        return (int) (x & 0xf);
    }

    private float score_board(long board) {
        return score_helper(board, score_table);
    }

    private float score_helper(long board, final float[] table) {

        return table[(int) ((board >>  0) & ROW_MASK)] +
                table[(int) ((board >> 16) & ROW_MASK)] +
                table[(int) ((board >> 32) & ROW_MASK)] +
                table[(int) ((board >> 48) & ROW_MASK)];
    }

    private float score_heur_board(long board) {
        return score_helper(board , heur_score_table) +
                score_helper(transpose(board), heur_score_table);
    }
    /*** 移动棋子*/
    private  long execute_move(int move, long board) {

        switch(move) {
            case 0: // up
                return execute_move_0(board);
            case 1: // down
                return execute_move_1(board);
            case 2: // left
                return execute_move_2(board);
            case 3: // right
                return execute_move_3(board);
            default:
                return ~0;
        }
    }

    private long execute_move_0(long board) {
        long ret = board;
        long t = transpose(board);
        ret ^= col_up_table[(int) ((t >>  0) & AIUtil.ROW_MASK)] <<  0;
        ret ^= col_up_table[(int) ((t >> 16) & AIUtil.ROW_MASK)] <<  4;
        ret ^= col_up_table[(int) ((t >> 32) & AIUtil.ROW_MASK)] <<  8;
        ret ^= col_up_table[(int) ((t >> 48) & AIUtil.ROW_MASK)] << 12;
        //print_board(ret);
        return ret;
    }
    private long execute_move_1(long board) {
        long ret = board;
        long t = transpose(board);
        ret ^= col_down_table[(int) ((t >>  0) & ROW_MASK)] <<  0;
        ret ^= col_down_table[(int) ((t >> 16) & ROW_MASK)] <<  4;
        ret ^= col_down_table[(int) ((t >> 32) & ROW_MASK)] <<  8;
        ret ^= col_down_table[(int) ((t >> 48) & ROW_MASK)] << 12;
        //print_board(ret);
        return ret;
    }
    private long execute_move_2(long board) {
        long ret = board;
        ret ^= ((long)(row_left_table[(int) ((board >>  0) & ROW_MASK)])) <<  0;
        ret ^= ((long)(row_left_table[(int) ((board >> 16) & ROW_MASK)])) << 16;
        ret ^= ((long)(row_left_table[(int) ((board >> 32) & ROW_MASK)])) << 32;
        ret ^= ((long)(row_left_table[(int) ((board >> 48) & ROW_MASK)])) << 48;
        //print_board(ret);
        return ret;
    }
    private long execute_move_3(long board) {
        long ret = board;
        ret ^= ((long)(row_right_table[(int) ((board >>  0) & ROW_MASK)])) <<  0;
        ret ^= ((long)(row_right_table[(int) ((board >> 16) & ROW_MASK)])) << 16;
        ret ^= ((long)(row_right_table[(int) ((board >> 32) & ROW_MASK)])) << 32;
        ret ^= ((long)(row_right_table[(int) ((board >> 48) & ROW_MASK)])) << 48;
        //print_board(ret);
        return ret;
    }

// Transpose rows/columns in a board:
//   0123       048c
//   4567  -->  159d
//   89ab       26ae
//   cdef       37bf
    private long transpose(long board) {
        long a1 = board & 0xF0F00F0FF0F00F0FL;
        long a2 = board & 0x0000F0F00000F0F0L;
        long a3 = board & 0x0F0F00000F0F0000L;
        long a = a1 | (a2 << 12) | (a3 >> 12);
        long b1 = a & 0xFF00FF0000FF00FFL;
        long b2 = a & 0x00FF00FF00000000L;
        long b3 = a & 0x00000000FF00FF00L;
        return b1 | (b2 >> 24) | (b3 << 24);
    }

    class EvalState {
        public Map<Long,TransTableEntry> trans_table=new HashMap<>(); // transposition table, to cache previously-seen moves
        public int maxdepth;
        public int curdepth;
        public int cachehits;
        public long moves_evaled;
        public int depth_limit;
    }
    public class TransTableEntry{
        public int depth;
        public float heuristic;
    }
}