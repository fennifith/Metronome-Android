package james.metronome;

public class Utils {

    public static int toBpm(long interval) {
        return (int) (60000 / interval);
    }

    public static long toInterval(int bpm) {
        return (long) 60000 / bpm;
    }

}
