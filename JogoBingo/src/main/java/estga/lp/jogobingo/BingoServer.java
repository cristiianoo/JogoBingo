package estga.lp.jogobingo;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class BingoServer {
    private static final int PORT = 12345;
    private static final int MAX_PLAYERS = 4;
    private static final int TOTAL_NUMBERS = 99;
    private static final int DRAW_INTERVAL_MS = 5000;

    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private static final Set<Integer> drawnNumbers = new LinkedHashSet<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static boolean gameStarted = false;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Servidor Bingo iniciado na porta " + PORT);

        while (clients.size() < MAX_PLAYERS) {
            Socket socket = serverSocket.accept();
            ClientHandler clientHandler = new ClientHandler(socket);
            clients.add(clientHandler);
            new Thread(clientHandler).start();
        }
    }

    private static void startGame() {
        gameStarted = true;
        scheduler.scheduleAtFixedRate(() -> {
            if (drawnNumbers.size() >= TOTAL_NUMBERS) return;
            int number;
            do {
                number = new Random().nextInt(TOTAL_NUMBERS) + 1;
            } while (drawnNumbers.contains(number));
            drawnNumbers.add(number);
            broadcast("DRAW:" + number);
        }, 0, DRAW_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private static void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler ch : clients) {
                ch.sendMessage(message);
            }
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String name;
        private String cardId;
        private int[] cardNumbers;
        private boolean ready = false;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void sendMessage(String msg) {
            out.println(msg);
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                name = in.readLine(); // nome do jogador
                cardId = UUID.randomUUID().toString().substring(0, 8);
                cardNumbers = generateCard();

                out.println("CARD_ID:" + cardId);
                out.println("CARD_NUMBERS:" + Arrays.toString(cardNumbers).replaceAll("[\\[\\] ]", ""));

                String input;
                while ((input = in.readLine()) != null) {
                    if (input.equals("READY")) {
                        ready = true;
                        checkAllReady();
                    } else if (input.startsWith("CLAIM:")) {
                        String claimType = input.split(":")[1];
                        validateClaim(claimType);
                    }
                }
            } catch (IOException e) {
                System.out.println("Erro com cliente: " + e.getMessage());
            }
        }

        private int[] generateCard() {
            Set<Integer> nums = new LinkedHashSet<>();
            Random rand = new Random();
            while (nums.size() < 25) {
                nums.add(rand.nextInt(99) + 1);
            }
            return nums.stream().mapToInt(Integer::intValue).toArray();
        }

        private void checkAllReady() {
            if (clients.stream().allMatch(c -> c.ready) && !gameStarted) {
                broadcast("ALL_READY");
                startGame();
            }
        }

        private void validateClaim(String claimType) {
            if (claimType.equals("LINE")) {
                broadcast("LINE_BY:" + name);
            } else if (claimType.equals("BINGO")) {
                broadcast("BINGO_BY:" + name);
                sendMessage("WINNER");
                synchronized (clients) {
                    for (ClientHandler ch : clients) {
                        if (!ch.name.equals(this.name)) {
                            ch.sendMessage("LOSE");
                        }
                    }
                }
                scheduler.shutdown();
            }
        }
    }
}
