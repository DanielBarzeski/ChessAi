package App;

public class Clock {
    public static final Clock white = new Clock(), black = new Clock();

    private int startHour, startMinute;
    private int hour, minute, second;
    private boolean run;

    public void setHour(int hour) {
        this.startHour = hour;
        this.hour = hour;
        run = this.startHour != 0 || this.startMinute != 0;
    }

    public void setMinute(int minute) {
        this.startMinute = minute;
        this.minute = minute;
        run = this.startHour != 0 || this.startMinute != 0;
    }

    public void reset() {
        this.hour = startHour;
        this.minute = startMinute;
        this.second = 0;
        this.run = startMinute != 0 || startHour != 0;
    }

    public void move() {
        if (!isGameOver()) {
            second--;
            if (second < 0) {
                second = 59;
                minute--;
                if (minute < 0) {
                    minute = 59;
                    hour--;
                    if (hour < 0) {
                        hour = 0;
                        minute = 0;
                        second = 0;
                    }
                }
            }
        }
    }

    public boolean isGameOver() {
        return run && hour == 0 && minute == 0 && second == 0;
    }

    public boolean isRunning() {
        return run;
    }

    @Override
    public String toString() {
        return String.format("%02d:%02d:%02d", hour, minute, second);
    }
}
