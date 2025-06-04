import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class BingoClient extends JFrame {
    private JTextField nameField;
    private JLabel statusLabel, cardIdLabel;
    private JButton readyButton, lineButton, bingoButton;
    private JButton[] cardButtons = new JButton[25];
    private JPanel drawnNumbersPanel;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String name;
    private String cardId;

    public BingoClient() {
        setTitle("Cliente Bingo ESTGA");
        setSize(950, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        setFont(new Font("Segoe UI", Font.PLAIN, 14));

        UIManager.put("Button.arc", 15);
        UIManager.put("Component.arc", 10);

        // Top Panel
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        namePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 0));
        nameField = new JTextField(15);
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        namePanel.add(new JLabel("Nome: "));
        namePanel.add(nameField);
        topPanel.add(namePanel, BorderLayout.WEST);

        cardIdLabel = new JLabel("Card ID: ---");
        cardIdLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JPanel idPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        idPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 10));
        idPanel.add(cardIdLabel);
        topPanel.add(idPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Card Panel
        JPanel cardPanel = new JPanel(new GridLayout(5, 5, 8, 8));
        cardPanel.setBorder(new TitledBorder("Cartão de Bingo"));
        for (int i = 0; i < 25; i++) {
            JButton cell = new JButton("--");
            int finalI = i;
            cell.setFont(new Font("Segoe UI", Font.BOLD, 20));
            cell.setBackground(Color.LIGHT_GRAY);
            cell.setFocusPainted(false);
            cell.setEnabled(false);
            cell.addActionListener(e -> {
                cell.setBackground(new Color(102, 255, 102));
                cell.setForeground(Color.BLACK);
            });
            cardButtons[i] = cell;
            cardPanel.add(cell);
        }
        add(cardPanel, BorderLayout.CENTER);

        // Drawn Numbers Panel
        drawnNumbersPanel = new JPanel();
        drawnNumbersPanel.setLayout(new BoxLayout(drawnNumbersPanel, BoxLayout.Y_AXIS));
        drawnNumbersPanel.setBorder(new TitledBorder("Números sorteados"));
        JScrollPane scrollPane = new JScrollPane(drawnNumbersPanel);
        scrollPane.setPreferredSize(new Dimension(150, 0));
        add(scrollPane, BorderLayout.EAST);

        // Bottom Panel
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        JPanel buttonsPanel = new JPanel();
        readyButton = new JButton("Pronto para iniciar");
        lineButton = new JButton("Linha");
        bingoButton = new JButton("Bingo");
        readyButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lineButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        bingoButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lineButton.setEnabled(false);
        bingoButton.setEnabled(false);
        buttonsPanel.add(readyButton);
        buttonsPanel.add(lineButton);
        buttonsPanel.add(bingoButton);

        statusLabel = new JLabel("Status: Aguardando nome...");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        bottomPanel.add(buttonsPanel, BorderLayout.NORTH);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        add(bottomPanel, BorderLayout.SOUTH);

        // Events
        readyButton.addActionListener(e -> onReady());
        lineButton.addActionListener(e -> out.println("CLAIM:LINE"));
        bingoButton.addActionListener(e -> out.println("CLAIM:BINGO"));

        connectToServer();
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        handleServerMessage(line);
                    }
                } catch (IOException e) {
                    showMessage("Conexão perdida com o servidor.");
                }
            }).start();

        } catch (IOException e) {
            showMessage("Erro ao conectar ao servidor.");
        }
    }

    private void onReady() {
        name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Insira um nome válido.");
            return;
        }
        out.println(name);
        nameField.setEditable(false);
        readyButton.setEnabled(false);
        statusLabel.setText("Aguardando início do jogo...");
    }

    private void handleServerMessage(String msg) {
        if (msg.startsWith("CARD_ID:")) {
            cardId = msg.substring(8);
            SwingUtilities.invokeLater(() -> cardIdLabel.setText("Card ID: " + cardId));
        } else if (msg.startsWith("CARD_NUMBERS:")) {
            String[] nums = msg.substring(14).split(",");
            SwingUtilities.invokeLater(() -> {
                for (int i = 0; i < 25; i++) {
                    cardButtons[i].setText(nums[i]);
                    cardButtons[i].setEnabled(true);
                }
                lineButton.setEnabled(true);
                bingoButton.setEnabled(true);
                out.println("READY");
                statusLabel.setText("Cartão recebido. À espera dos outros jogadores...");
            });
        } else if (msg.startsWith("DRAW:")) {
            String number = msg.substring(5);
            SwingUtilities.invokeLater(() -> {
                JLabel label = new JLabel(number);
                label.setFont(new Font("Segoe UI", Font.BOLD, 16));
                drawnNumbersPanel.add(label);
                drawnNumbersPanel.revalidate();
            });
        } else if (msg.startsWith("LINE_BY:")) {
            String winner = msg.substring(8);
            showMessage("Linha feita por " + winner);
        } else if (msg.startsWith("BINGO_BY:")) {
            String winner = msg.substring(9);
            showMessage("Bingo feito por " + winner);
        } else if (msg.equals("WINNER")) {
            showMessage("Parabéns!");
        } else if (msg.equals("LOSE")) {
            showMessage("Ainda não foi desta. Tente novamente.");
        }
    }

    private void showMessage(String msg) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, msg));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BingoClient().setVisible(true));
    }
}
