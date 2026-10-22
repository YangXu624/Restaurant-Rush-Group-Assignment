package restaurantrush.integration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import restaurantrush.Main;

/** Exercises production Main in a separate JVM with redirected fixture input. */
public class MainTest {
    public static void main(String[] args) throws Exception {
        String victory = launch("victory.txt");
        completed(victory, "Victory", "$66.00", "79.20", "Paid: 5 | Served: 5 | Unhappy departures: 0");
        String invalid = launch("invalid-input.txt");
        completed(invalid, "Victory", "$66.00", "79.20", "Paid: 5 | Served: 5 | Unhappy departures: 0");
        contains(invalid, "Invalid command.");
        contains(invalid, "IDs must be whole numbers");
        for (String fixture : List.of("defeat.txt", "abandonment.txt")) {
            completed(launch(fixture), "Defeat", "$0.00", "N/A", "Paid: 0 | Served: 0 | Unhappy departures: 2");
        }
        for (String fixture : List.of("eof-waiter.txt", "eof-chef.txt")) {
            String text = launch(fixture);
            contains(text, "Input ended. The current turn was not completed.");
            for (String forbidden : List.of("End of turn", "Turns used:", "Victory", "Defeat")) {
                if (text.contains(forbidden)) {
                    throw new AssertionError(fixture + ": fabricated completion: " + forbidden);
                }
            }
        }
        System.out.println("All Main launcher tests passed successfully!");
    }

    private static String launch(String fixture) throws Exception {
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        Path java = Path.of(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java");
        // Use only production classes: test helpers must not be needed to play the game.
        Path classes = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path input = Path.of("tests", "fixtures", "m5", fixture).toAbsolutePath();
        Path transcript = Files.createTempFile("m5-main-", ".log");
        Process process = null;
        try {
            process = new ProcessBuilder(java.toString(), "-cp", classes.toString(), "restaurantrush.Main")
                    .redirectInput(input.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(transcript.toFile())
                    .start();
            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                throw new AssertionError(fixture + ": launcher did not terminate within 20 seconds");
            }
            String text = Files.readString(transcript, StandardCharsets.UTF_8).replace("\r\n", "\n");
            if (process.exitValue() != 0) {
                throw new AssertionError(fixture + ": launcher exited with " + process.exitValue() + "\n" + text);
            }
            contains(text, "Turn 1\n");
            if (text.contains("skeleton")) {
                throw new AssertionError("Launcher must start the game, not print the skeleton message");
            }
            return text;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly().waitFor();
            }
            Files.deleteIfExists(transcript);
        }
    }

    private static void completed(String text, String outcome, String revenue, String average, String counts) {
        String finalSummary = outcome + "\nRevenue: " + revenue + "\nAverage paid satisfaction: "
                + average + "\n" + counts + "\nTurns used: 18\n";
        if (!text.endsWith(finalSummary)) {
            throw new AssertionError("Missing final summary:\n" + finalSummary);
        }
        if (occurrences(text, "End of turn ") != 18 || occurrences(text, outcome + "\n") != 1) {
            throw new AssertionError("Expected exactly 18 completed turns and one final outcome");
        }
        if (text.contains("Turn 19") || text.contains("Input ended.")) {
            throw new AssertionError("Complete fixture must finish without an extra turn or early EOF");
        }
    }

    private static int occurrences(String text, String part) {
        return (text.length() - text.replace(part, "").length()) / part.length();
    }

    private static void contains(String text, String expected) {
        if (!text.contains(expected)) {
            throw new AssertionError("Missing output: " + expected);
        }
    }
}
