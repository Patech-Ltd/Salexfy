package org.PSmart.POS.ui.MainView.ui.admin;

import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Repositories.UsersRepository;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.IconsUtils;
import org.PSmart.POS.Utils.MyUtils;

import org.PSmart.POS.ui.MainView.ui.stock.StockPanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AdminPanel extends JPanel {

    private static final String USERS = "USERS";
    private static final String SETTINGS = "SETTINGS";

    private final StockPanel stockPanel;
    private PTextField search;
    private JPanel playground;
    private JPanel usersView;
    private JPanel cards;
    private JButton usersTabBtn;
    private JButton settingsTabBtn;
    private List<User> usersList;
    private final UsersRepository usersRepository = DatabaseUtils.getInstance().getUsersRepository();
    private UserCard userCard;
    private EditUserDialog userEditor;
    private JScrollPane scrollPane;
    private AdminSettings settingsPanel;

    public AdminPanel(StockPanel stockPanel) {
        this.stockPanel = stockPanel;
        draw();
    }

    private void draw() {
        setLocation(3, 0);
        setSize(stockPanel.getPlayground().getSize());
        setLayout(null);
        setBackground(Color.WHITE);

        add(buildSubTabs());

        cards = new JPanel(new CardLayout());
        cards.setLocation(0, 50);
        cards.setSize(getWidth(), getHeight() - 50);
        cards.add(buildUsersView(), USERS);
        settingsPanel = new AdminSettings();
        cards.add(settingsPanel, SETTINGS);
        add(cards);

        showTab(USERS);
        repaint();
    }

    private JPanel buildSubTabs() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setBackground(new Color(52, 58, 64));
        bar.setLocation(0, 0);
        bar.setSize(getWidth(), 50);
        bar.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        usersTabBtn = new JButton("  \uD83D\uDC64  Users");
        settingsTabBtn = new JButton("  \u2699  Settings");

        usersTabBtn.setFont(MyUtils.courierFont(15));
        settingsTabBtn.setFont(MyUtils.courierFont(15));
        usersTabBtn.setForeground(Color.WHITE);
        settingsTabBtn.setForeground(Color.WHITE);
        usersTabBtn.setFocusPainted(false);
        settingsTabBtn.setFocusPainted(false);
        usersTabBtn.setBorderPainted(false);
        settingsTabBtn.setBorderPainted(false);
        usersTabBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        settingsTabBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        usersTabBtn.setPreferredSize(new Dimension(120, 34));
        settingsTabBtn.setPreferredSize(new Dimension(130, 34));

        usersTabBtn.addActionListener(e -> showTab(USERS));
        settingsTabBtn.addActionListener(e -> showTab(SETTINGS));

        bar.add(usersTabBtn);
        bar.add(settingsTabBtn);
        return bar;
    }

    private void showTab(String tab) {
        ((CardLayout) cards.getLayout()).show(cards, tab);

        Color active = new Color(0, 123, 255);
        Color idle = new Color(25, 118, 211);
        usersTabBtn.setBackground(tab.equals(USERS) ? active : idle);
        settingsTabBtn.setBackground(tab.equals(SETTINGS) ? active : idle);
    }

    private JPanel buildUsersView() {
        usersView = new JPanel(null);
        usersView.setBackground(Color.WHITE);

        JButton addButton = new JButton("Add New User");
        addButton.setBackground(new Color(52, 58, 64));
        addButton.setFont(MyUtils.courierFont(20));
        addButton.setForeground(Color.WHITE);
        addButton.setLocation(cards.getWidth() / 4, 10);
        addButton.setSize(cards.getWidth() / 2, 35);
        addButton.addActionListener(e -> openAddUserDialog());
        usersView.add(addButton);

        search = new PTextField(new Color(52, 58, 64), new Color(0, 0, 255),
                "Search User", IconsUtils.getIcon("search.png"));
        search.setLocation(cards.getWidth() / 6, 50);
        search.setSize(cards.getWidth() - cards.getWidth() / 3, 42);
        // Trigger filter on every keystroke
        search.getField().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { drawItems(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { drawItems(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { drawItems(); }
        });
        usersView.add(search);

        usersView.add(buildHeaderRow());

        drawItems();
        return usersView;
    }

    private JPanel buildHeaderRow() {
        JPanel header = new JPanel(new GridLayout(1, 5, 4, 0));
        header.setBackground(new Color(52, 58, 64));
        header.setLocation(0, 100);
        header.setSize(cards.getWidth(), 36);

        String[] cols = {"Name", "Username", "Phone", "Roles", "Status"};
        for (String col : cols) {
            JLabel l = new JLabel("  " + col, col.equals("Name") ? SwingConstants.LEFT : SwingConstants.CENTER);
            l.setFont(new Font("Courier", Font.BOLD, 13));
            l.setForeground(Color.WHITE);
            header.add(l);
        }
        return header;
    }

    private void openAddUserDialog() {
        if (userEditor != null) userEditor.dispose();
        userEditor = new EditUserDialog(
                null,
                stockPanel.getDashBoard().getDbsession(),
                stockPanel.getDashBoard().getLoggedUser()
        );
        userEditor.setSaveAction(this::drawItems);
        userEditor.setVisible(true);
    }

    public void drawItems() {
        if (scrollPane != null) usersView.remove(scrollPane);

        usersList = usersRepository.findAll();

        // Apply search filter if text is present
        String query = search != null ? search.getText().trim() : "";
        List<User> displayed = query.isEmpty() ? usersList : filterUsers(query);

        buildScrollPane(displayed);
        repaint();
    }

    /** Pure filter — no UI side-effects */
    private List<User> filterUsers(String query) {
        List<User> result = new ArrayList<>();
        String lq = query.toLowerCase();
        for (User u : usersList) {
            if (u.getFullname().toLowerCase().contains(lq)
                    || u.getUsername().toLowerCase().contains(lq)
                    || (u.getPhone() != null && u.getPhone().contains(lq))) {
                result.add(u);
            }
        }
        return result;
    }

    private void buildScrollPane(List<User> users) {
        int itemHeight = 50;
        int vgap = 5;

        GridLayout layout = new GridLayout(Math.max(users.size(), 1), 1);
        layout.setVgap(vgap);

        playground = new JPanel(layout);
        playground.setBackground(Color.WHITE);

        int contentHeight = (itemHeight + vgap) * Math.max(users.size(), 1);
        playground.setPreferredSize(new Dimension(cards.getWidth() - 40, contentHeight));

        for (User user : users) {
            UserJPanel row = new UserJPanel(user,
                    stockPanel.getDashBoard().getDbsession(),
                    stockPanel.getDashBoard().getLoggedUser());
            row.setAdminPanel(this);
            row.setPreferredSize(new Dimension(cards.getWidth() - 40, itemHeight));
            playground.add(row);

            row.setClickRunnable(() -> openUserCard(user, row));
        }

        // Wrap in a top-aligned container so GridLayout rows keep their fixed height
        // instead of stretching to fill the whole viewport (so a single user doesn't
        // occupy the entire page).
        JPanel topAlign = new JPanel(new BorderLayout());
        topAlign.setBackground(Color.WHITE);
        JPanel filler = new JPanel();
        filler.setBackground(Color.WHITE);
        topAlign.add(playground, BorderLayout.NORTH);
        topAlign.add(filler, BorderLayout.CENTER);

        scrollPane = new JScrollPane(topAlign);
        scrollPane.setBackground(Color.WHITE);
        scrollPane.setLocation(0, 140);
        scrollPane.setSize(cards.getWidth(), cards.getHeight() - 150);
        usersView.add(scrollPane);
    }

    private void openUserCard(User user, UserJPanel row) {
        if (userCard != null) userCard.dispose();
        userCard = new UserCard(
                user,
                stockPanel.getDashBoard().getDbsession(),
                stockPanel.getDashBoard().getLoggedUser()
        );
        userCard.setEditAction(() -> {
            userCard.dispose();
            openEditDialog(user, row);
        });
        userCard.setDeleteAction(() -> {
            userCard.dispose();
            drawItems();
        });
        userCard.setVisible(true);
    }

    private void openEditDialog(User user, UserJPanel row) {
        if (userEditor != null) userEditor.dispose();
        userEditor = new EditUserDialog(
                user,
                stockPanel.getDashBoard().getDbsession(),
                stockPanel.getDashBoard().getLoggedUser()
        );
        userEditor.setSaveAction(() -> {
            // Reload this single row from DB
            User fresh = usersRepository.findById(user.getUid()).orElse(user);
            row.setUser(fresh);
            row.feedData();
        });
        userEditor.setDeleteAction(() -> {
            userEditor.dispose();
            drawItems();
        });
        userEditor.setVisible(true);
    }
}
