import java.awt.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;

// Run: java HandoverDemo.java
// Standalone illustration: scripted forecasts, not GRU outputs or experiment results.
public class HandoverDemo extends JPanel {
    static final int CAPACITY = 10;
    static final double PREPARE = 2, EXPIRY = 5, HORIZON = 3;
    static final Color INK = new Color(25, 45, 65);
    static final Color GREEN = new Color(28, 145, 97);
    static final Color ORANGE = new Color(221, 143, 32);
    static final Color RED = new Color(201, 64, 65);
    final JComboBox<String> forecasts = new JComboBox<>(new String[] {
        "Mostly correct (90%)", "Mixed (65%)", "Mostly wrong (25%)"
    });
    final JSpinner population = new JSpinner(new SpinnerNumberModel(24, 1, 40, 1));
    final ArrayList<User> users = new ArrayList<>();
    final ArrayList<String> events = new ArrayList<>();
    final Timer timer;
    Random random;
    double time, totalDelay, wasted;
    int handovers, ready, expired, rejected;

    static class User {
        int id, cell, target = -1, leg;
        double x, y, bookedAt, nextPrediction, waitingUntil;
        double[][] route;

        User(int id) {
            this.id = id;
            x = 190 + (id % 4) * 1.5;
            y = 45 + id * 3.6;
            route = new double[][] {{340, y}, {340, 330}, {60, 330}, {60, y}};
        }

        // Work on a copy for the three-second forecast; never move the real user.
        double[] positionAfter(double seconds) {
            double px = x, py = y, remaining = seconds * 1.4;
            int nextLeg = leg;
            while (remaining > 0) {
                double dx = route[nextLeg][0] - px, dy = route[nextLeg][1] - py;
                double distance = Math.hypot(dx, dy);
                if (remaining < distance) {
                    px += dx / distance * remaining;
                    py += dy / distance * remaining;
                    break;
                }
                px = route[nextLeg][0];
                py = route[nextLeg][1];
                remaining -= distance;
                nextLeg = (nextLeg + 1) % route.length;
            }
            return new double[] {px, py, nextLeg};
        }
    }

    HandoverDemo() {
        setPreferredSize(new Dimension(1120, 760));
        setBackground(Color.WHITE);
        add(new JLabel("Users:"));
        add(population);
        add(forecasts);
        JButton play = new JButton("Play / pause");
        JButton restart = new JButton("Reset");
        JButton step = new JButton("Step 0.25 s");
        add(play); add(restart); add(step);
        timer = new Timer(50, event -> tick()); // Five simulated seconds per real second.
        play.addActionListener(event -> {
            if (timer.isRunning()) timer.stop(); else timer.start();
        });
        restart.addActionListener(event -> reset());
        step.addActionListener(event -> { timer.stop(); tick(); });
        population.addChangeListener(event -> reset());
        forecasts.addActionListener(event -> reset());
        reset();
    }

    void reset() {
        timer.stop();
        time = totalDelay = wasted = 0;
        handovers = ready = expired = rejected = 0;
        random = new Random(42);
        users.clear(); events.clear();
        for (int i = 0; i < (int) population.getValue(); i++) users.add(new User(i));
        log("Morning crowd starts near the Cell 0 / Cell 1 boundary.");
        repaint();
    }

    int cell(double x, double y) {
        return (x > 200 ? 1 : 0) + (y > 200 ? 2 : 0);
    }

    int occupied(int cell) {
        int count = 0;
        for (User user : users) if (user.target == cell) count++;
        return count;
    }

    void log(String message) {
        events.add(String.format("%6.2f s   %s", time, message));
        if (events.size() > 6) events.remove(0);
    }

    void tick() {
        time += 0.25;
        // Release expired reservations before handovers and new requests.
        for (User user : users) {
            if (user.target >= 0 && time >= user.bookedAt + EXPIRY) {
                log("User " + user.id + ": unused Cell " + user.target + " slot expired.");
                user.target = -1;
                expired++;
                wasted += EXPIRY;
            }
        }
        for (User user : users) {
            double[] next = user.positionAfter(0.25);
            user.x = next[0]; user.y = next[1]; user.leg = (int) next[2];
            int newCell = cell(user.x, user.y);
            if (newCell != user.cell) {
                double delay = PREPARE;
                if (user.target == newCell) {
                    delay = Math.max(0, user.bookedAt + PREPARE - time);
                    user.target = -1;
                }
                user.cell = newCell;
                user.waitingUntil = time + delay;
                handovers++;
                if (delay == 0) ready++;
                totalDelay += delay;
                log(String.format("User %d enters Cell %d; preparation wait %.2f s.",
                                  user.id, newCell, delay));
            }
        }
        // These forecasts deliberately use the known route plus scripted errors.
        // Rotate request order so the same user does not always get first choice.
        int first = (int) (time * 4) % users.size();
        double accuracy = new double[] {0.9, 0.65, 0.25}[forecasts.getSelectedIndex()];
        for (int i = 0; i < users.size(); i++) {
            User user = users.get((first + i) % users.size());
            if (user.target >= 0 || time < user.nextPrediction) continue;
            user.nextPrediction = time + 1;
            double[] future = user.positionAfter(HORIZON);
            int predicted = cell(future[0], future[1]);
            if (predicted == user.cell) continue;
            if (random.nextDouble() > accuracy) {
                // Choose a cell that is neither the actual destination nor current cell.
                int wrong = (predicted + 1) % 4;
                if (wrong == user.cell) wrong = (wrong + 1) % 4;
                predicted = wrong;
            }
            if (occupied(predicted) == CAPACITY) {
                rejected++;
                log("User " + user.id + ": reservation rejected, Cell " + predicted + " full.");
            } else {
                user.target = predicted;
                user.bookedAt = time;
                log("User " + user.id + ": reserved Cell " + predicted + ".");
            }
        }
        repaint();
    }

    void text(Graphics2D g, String value, int x, int y, int size) {
        g.setColor(INK);
        g.setFont(new Font("SansSerif", Font.PLAIN, size));
        g.drawString(value, x, y);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        text(g, "Morning rush: predictive handover reservations", 30, 77, 27);
        text(g, "Scripted forecasts, not a trained GRU. Demo results are separate from the project results.", 30, 105, 16);
        Color[] fills = {new Color(227,240,248), new Color(225,243,233),
                         new Color(249,240,223), new Color(240,230,249)};
        for (int c = 0; c < 4; c++) {
            int x = 30 + (c % 2) * 260, y = 135 + (c / 2) * 260;
            g.setColor(fills[c]); g.fillRect(x, y, 258, 258);
            text(g, "Cell " + c, x + 12, y + 25, 19);
            int count = occupied(c);
            text(g, "Reservations: " + count + "/10", x + 12, y + 49, 16);
            for (int slot = 0; slot < CAPACITY; slot++) {
                g.setColor(slot < count ? ORANGE : Color.WHITE);
                g.fillRect(x + 12 + slot * 23, y + 61, 18, 10);
            }
            int bx = x + 130, by = y + 130;
            g.setColor(INK); g.fillRect(bx - 3, by - 6, 6, 18);
            g.drawOval(bx - 10, by - 14, 20, 20);
        }
        for (User user : users) {
            int x = 30 + (int) (user.x * 1.3), y = 135 + (int) (user.y * 1.3);
            if (user.target >= 0) {
                int tx = 160 + (user.target % 2) * 260;
                int ty = 265 + (user.target / 2) * 260;
                g.setColor(new Color(215, 174, 91));
                g.drawLine(x, y, tx, ty);
            }
            g.setColor(time < user.waitingUntil ? RED : user.target >= 0 ? ORANGE : GREEN);
            g.fillOval(x - 3, y - 3, 7, 7);
        }
        text(g, String.format("Simulation time: %.2f s  (5x playback)", time), 580, 155, 22);
        text(g, "Preparation 2 s / forecast horizon 3 s / expiry 5 s", 580, 187, 16);
        text(g, "Green: connected   Orange: reserved   Red: waiting", 580, 219, 16);
        text(g, "Handovers: " + handovers, 580, 263, 21);
        text(g, "Fully prepared on arrival: " + ready, 580, 296, 21);
        text(g, String.format("Mean preparation wait: %.2f s",
             handovers == 0 ? 0 : totalDelay / handovers), 580, 329, 21);
        text(g, "Rejected reservation requests: " + rejected, 580, 362, 19);
        text(g, "Expired reservations: " + expired, 580, 395, 19);
        text(g, String.format("Unused cost (expired only): %.0f slot-seconds", wasted), 580, 428, 18);
        text(g, "Recent events", 580, 478, 20);
        for (int i = 0; i < events.size(); i++) text(g, events.get(i), 580, 510 + i * 25, 14);
        text(g, "Each dot is a walking user. Lines show their reserved cell; bars show occupied slots.", 30, 697, 17);
        text(g, "Try 24-40 users to fill Cell 1. Change forecast quality, then replay to see wasted capacity.", 30, 726, 16);
        g.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Campus Mobility - Predictive Handover");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setContentPane(new HandoverDemo());
            window.pack();
            window.setResizable(false);
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}
