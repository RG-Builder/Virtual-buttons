package com.example.virtualbuttons.core;

public class GestureResult {
    public static final int TYPE_NONE = 0;
    public static final int TYPE_SWIPE_UP = 1;
    public static final int TYPE_SWIPE_DOWN = 2;
    public static final int TYPE_TAP = 3;
    public static final int TYPE_DOUBLE_TAP = 4;
    public static final int TYPE_LONG_PRESS = 5;
    public static final int TYPE_DRAG = 6;
    public static final int TYPE_TWO_FINGER_SWIPE = 7;

    public static final int EDGE_NONE = 0;
    public static final int EDGE_LEFT = 1;
    public static final int EDGE_RIGHT = 2;
    public static final int EDGE_TOP = 4;
    public static final int EDGE_BOTTOM = 8;

    public final int type;
    public final int edge;
    public final float x;
    public final float y;
    public final float velocity;
    public final float distance;
    public final float directionX;
    public final float directionY;
    public final long durationMs;

    public GestureResult(int type, int edge, float x, float y, float velocity,
                         float distance, float directionX, float directionY, long durationMs) {
        this.type = type;
        this.edge = edge;
        this.x = x;
        this.y = y;
        this.velocity = velocity;
        this.distance = distance;
        this.directionX = directionX;
        this.directionY = directionY;
        this.durationMs = durationMs;
    }

    public boolean isVertical() {
        return type == TYPE_SWIPE_UP || type == TYPE_SWIPE_DOWN;
    }

    public boolean isEdgeGesture() {
        return edge != EDGE_NONE;
    }

    public boolean isLeftEdge() {
        return (edge & EDGE_LEFT) != 0;
    }

    public boolean isRightEdge() {
        return (edge & EDGE_RIGHT) != 0;
    }

    public boolean isTopEdge() {
        return (edge & EDGE_TOP) != 0;
    }

    public boolean isBottomEdge() {
        return (edge & EDGE_BOTTOM) != 0;
    }
}
