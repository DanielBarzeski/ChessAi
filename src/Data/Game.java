package Data;

public class Game {
    public static final int CELL_SIZE = 50;

    private static boolean VISIBLE, FINISHED, RESTARTING, ENEMY_STARTING, AI, UNDO, HINT, MENU_ACTIVE = true, DRAW;

    public static void setEnemyStarting(boolean state) {
        Game.ENEMY_STARTING = state;
    }

    public static boolean isEnemyStarting() {
        return ENEMY_STARTING;
    }

    public static void setAiExistence(boolean state) {
        AI = state;
    }

    public static boolean isAiExist() {
        return AI;
    }

    public static void start() {
        DRAW = false;
        FINISHED = false;
    }

    public static void end(boolean draw) {
        FINISHED = true;
        DRAW = draw;
    }

    public static void restart() {
        RESTARTING = true;
    }

    public static void stopRestarting() {
        RESTARTING = false;
    }

    public static boolean isVisible() {
        return VISIBLE;
    }

    public static boolean isFinished() {
        return FINISHED;
    }

    public static boolean isRestarting() {
        return RESTARTING;
    }

    public static void setIfVisible(boolean state) {
        VISIBLE = state;
    }

    public static boolean isUndoing() {
        return UNDO;
    }

    public static void setUndoing(boolean state) {
        Game.UNDO = state;
    }

    public static boolean isMenuActive() {
        return MENU_ACTIVE;
    }

    public static void setMenuActive(boolean menuActive) {
        MENU_ACTIVE = menuActive;
    }

    public static boolean isHinting() {
        return HINT;
    }

    public static void setHint(boolean state) {
        Game.HINT = state;
    }

    public static boolean isDraw() {
        return DRAW;
    }
}
