package com.urrecliner.game2048.Util;

import com.urrecliner.game2048.Model.Tile;

/**
 * Created by Tang on 2017/6/18.
 */
public class AIUtil {
    public static final long ROW_MASK = 0xFFFFL;
    public static final long COL_MASK = 0x000F000F000F000FL;

    public static final float CPROB_THRESH_BASE = 0.0001f;
    public static final int CACHE_DEPTH_LIMIT  = 6;
    public static long unpack_col(int row) {
        return (((long) row | ((long) row << 12L) | ((long) row << 24L) | ((long) row << 36L)) & COL_MASK);
    }
    public static int reverse_row(int row) {
        return ((row >> 12) | ((row >> 4) & 0x00F0)  | ((row << 4) & 0x0F00) | (row << 12))&0xFFFF;//不越界
    }

    public static Long TileArrayToLong(int[][] field){
        long value=1;
        for(int i=3;i>=0;i--)
            for(int j=3;j>=0;j--){
                value<<=4;
                value+=DecimalToBinaryBit(field[i][j]);
            }
        return value;
    }

    public static Long TileArrayToLong(Tile[][] field){
        long value=1;
        for(int i=3;i>=0;i--)
            for(int j=3;j>=0;j--){
                value<<=4;
                value+=DecimalToBinaryBit(field[j][i]!=null?field[j][i].getValue():0);
            }
        return value;
    }

    public static int DecimalToBinaryBit(int value){
        int bit=0;
        while (value/2!=0){
            value/=2;
            bit++;
        }
        return bit;
    }

    public static void print_board(long board) {
        int i,j;
        for(i=0; i<4; i++) {
            for(j=0; j<4; j++) {
                long powerVal = (board) & 0xf;
                System.out.print(((powerVal == 0) ? 0 : 1 << powerVal)+"  ");
                board >>= 4;
            }
            System.out.println();
        }
        System.out.println();
    }
}