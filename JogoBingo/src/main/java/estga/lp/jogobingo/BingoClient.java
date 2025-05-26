package estga.lp.jogobingo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class BingoClient extends JFrame {
    private JTextField nameField;
    private JLabel nameLabel;
    private JButton readyButton, lineButton, bingoButton;
    private JLabel statusLabel, cardIdLabel;
    private JPanel cardPanel, drawnNumbersPanel;
    private JButton[] cardButtons = new JButton[25];
    private String cardId;
    private java.util.List<JLabel> drawnNumberLabels = new ArrayList<>();
    private JPanel topPanel;
    private JPanel namePanel;

    public BingoClient() {
        setTitle("Cliente Bingo ESTGA");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Top panel with name and ID
        topPanel = new JPanel(new BorderLayout());

        namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        nameField = new JTextField(15);
        namePanel.add(new JLabel("Name: "));
        namePanel.add(nameField);
        topPanel.add(namePanel, BorderLayout.WEST);

        cardId = UUID.randomUUID().toString().substring(0, 8);
        cardIdLabel = new JLabel("Card ID: " + cardId);
        JPanel idPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        idPanel.add(cardIdLabel);
        topPanel.add(idPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Card panel
        cardPanel = new JPanel(new GridLayout(5, 5, 5, 5));
        for (int i = 0; i < 25; i++) {
            JButton cell = new JButton("--");
            cell.setFont(new Font("Arial", Font.PLAIN, 16));
            cell.setEnabled(false);
            cardButtons[i] = cell;
            cardPanel.add(cell);
        }
        add(cardPanel, BorderLayout.CENTER);

        // Bottom panel with game buttons and status
        JPanel bottomPanel = new JPanel(new BorderLayout());

        JPanel buttonsPanel = new JPanel();
        readyButton = new JButton("Pronto para iniciar");
        lineButton = new JButton("Linha");
        bingoButton = new JButton("Bingo");
        lineButton.setEnabled(false);
        bingoButton.setEnabled(false);
        buttonsPanel.add(readyButton);
        buttonsPanel.add(lineButton);
        buttonsPanel.add(bingoButton);

        statusLabel = new JLabel("Status: Waiting for login...");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        bottomPanel.add(buttonsPanel, BorderLayout.NORTH);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        // Panel for drawn numbers
        drawnNumbersPanel = new JPanel();
        drawnNumbersPanel.setLayout(new BoxLayout(drawnNumbersPanel, BoxLayout.Y_AXIS));
        drawnNumbersPanel.setBorder(BorderFactory.createTitledBorder("Drawn Numbers"));
        JScrollPane scrollPane = new JScrollPane(drawnNumbersPanel);
        scrollPane.setPreferredSize(new Dimension(150, 0));
        add(scrollPane, BorderLayout.EAST);

        // Enable "Ready" button only if name is filled
        nameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { validateName(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { validateName(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { validateName(); }
            private void validateName() {
                readyButton.setEnabled(!nameField.getText().trim().isEmpty());
            }
        });
        readyButton.setEnabled(false);

        readyButton.addActionListener(e -> {
            readyButton.setVisible(false);
            generateCard();
            statusLabel.setText("Status: Waiting for other players...");
            lineButton.setEnabled(true);
            bingoButton.setEnabled(true);
            replaceNameField();
            simulateDrawnNumbers();
        });

        lineButton.addActionListener(e -> statusLabel.setText("Claimed: Line"));
        bingoButton.addActionListener(e -> statusLabel.setText("Claimed: Bingo"));

        setVisible(true);
    }

    private void replaceNameField() {
        String name = nameField.getText();
        namePanel.removeAll();
        nameLabel = new JLabel("Name: " + name);
        namePanel.add(nameLabel);
        namePanel.revalidate();
        namePanel.repaint();
    }

    private void generateCard() {
        Set<Integer> numbers = new LinkedHashSet<>();
        Random rand = new Random();
        while (numbers.size() < 25) {
            numbers.add(rand.nextInt(75) + 1);
        }
        Iterator<Integer> it = numbers.iterator();
        for (int i = 0; i < 25; i++) {
            int number = it.next();
            JButton button = cardButtons[i];
            button.setText(String.valueOf(number));
            button.setEnabled(true);
            button.setBackground(null);
            button.addActionListener(new ActionListener() {
                boolean marked = false;
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!marked) {
                        button.setBackground(Color.GREEN);
                        marked = true;
                    } else {
                        button.setBackground(null);
                        marked = false;
                    }
                }
            });
        }
    }

    private void simulateDrawnNumbers() {
        new Thread(() -> {
            try {
                Random rand = new Random();
                Set<Integer> numbers = new HashSet<>();
                while (numbers.size() < 5) {
                    int number = rand.nextInt(75) + 1;
                    if (numbers.add(number)) {
                        SwingUtilities.invokeLater(() -> addDrawnNumber(number));
                        Thread.sleep(5000);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void addDrawnNumber(int number) {
        JLabel newLabel = new JLabel(String.valueOf(number));
        newLabel.setFont(new Font("Arial", Font.BOLD, 18));
        for (JLabel label : drawnNumberLabels) {
            label.setFont(new Font("Arial", Font.PLAIN, 16));
        }
        drawnNumberLabels.add(newLabel);
        drawnNumbersPanel.add(newLabel);
        drawnNumbersPanel.revalidate();
        drawnNumbersPanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BingoClient::new);
    }
}
