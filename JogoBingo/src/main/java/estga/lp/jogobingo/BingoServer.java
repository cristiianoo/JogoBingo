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
    private static final Set<Integer> drawnNumbers = Collections.synchronizedSet(new LinkedHashSet<>());
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

                name = in.readLine();
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
            List<Integer> allNumbers = new ArrayList<>();
            for (int i = 1; i <= 99; i++) {
                allNumbers.add(i);
            }
            Collections.shuffle(allNumbers);
            
            int[] card = new int[25];
            for (int i = 0; i < 25; i++) {
                card[i] = allNumbers.get(i);
            }
            return card;
        }

        private void checkAllReady() {
            if (clients.stream().allMatch(c -> c.ready) && !gameStarted) {
                broadcast("ALL_READY");
                startGame();
            }
        }

        private void validateClaim(String claimType) {
            Set<Integer> marked = new HashSet<>();
            for (int n : cardNumbers) {
                if (drawnNumbers.contains(n)) {
                    marked.add(n);
                }
            }

            boolean valid = false;

            if ("LINE".equals(claimType)) {
                for (int i = 0; i < 5; i++) {
                    boolean line = true;
                    for (int j = 0; j < 5; j++) {
                        if (!marked.contains(cardNumbers[i * 5 + j])) {
                            line = false;
                            break;
                        }
                    }
                    if (line) {
                        valid = true;
                        break;
                    }
                }
                if (valid) {
                    broadcast("LINE_BY:" + name);
                }
            } else if ("BINGO".equals(claimType)) {
                if (marked.size() == 25) {
                    valid = true;
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

            if (!valid) {
                sendMessage("INVALID_CLAIM");
            }
        }
    }
}