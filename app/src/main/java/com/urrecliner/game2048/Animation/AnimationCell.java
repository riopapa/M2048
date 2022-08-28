package com.urrecliner.game2048.Animation;

import com.urrecliner.game2048.Model.Cell;

public class AnimationCell extends Cell {
    public final int[] extras;
    private final int animationType;
    private final long animationTime;
    private final long delayTime;
    private long timeElapsed;

    public AnimationCell(int x, int y, int animationType, long length, long delay, int[] extras) {
        super(x, y);
        this.animationType = animationType;
        animationTime = length;
        delayTime = delay;
        this.extras = extras;
    }

    public int getAnimationType() {
        return animationType;
    }

    public void tick(long timeElapsed) {
        this.timeElapsed = this.timeElapsed + timeElapsed;
    }

    public boolean animationDone() {
        return animationTime + delayTime < timeElapsed;
    }

    public double getPercentageDone() {
        return Math.max(0, 1.0 * (timeElapsed - delayTime) / animationTime);
    }

    public boolean isActive() {
        return (timeElapsed >= delayTime);
    }

}