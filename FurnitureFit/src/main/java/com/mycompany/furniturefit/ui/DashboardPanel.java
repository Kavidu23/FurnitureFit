package com.mycompany.furniturefit.ui;

import com.mycompany.furniturefit.db.DesignDAO;
import com.mycompany.furniturefit.model.Design;
import com.mycompany.furniturefit.model.Furniture;
import com.mycompany.furniturefit.model.Room;
import com.mycompany.furniturefit.model.User;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard panel with clear task hierarchy and quick actions.
 */
public class DashboardPanel extends JPanel {

    private static final Color GREEN = new Color(45, 136, 45);
    private static final Color PANEL_BG = new Color(240, 242, 245);
    private static final Color CARD_BG = new Color(255, 255, 255);
    private static final Color MUTED_TEXT = new Color(100, 100, 100);

    private final Font titleFont = new Font("Segoe UI", Font.BOLD, 30);
    private final Font subtitleFont = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font bodyFont = new Font("Segoe UI", Font.PLAIN, 13);
    private final Font buttonFont = createMediumWeightFont(15f);

    private User currentUser;
    private final DesignDAO designDAO;

    private final JLabel welcomeLabel;
    private final JLabel subWelcomeLabel;

    private final JLabel designCountValue;
    private final JLabel furnitureCountValue;
    private final JLabel avgRoomValue;
    private final JLabel lastUpdatedValue;

    private final JButton openLatestButton;
    private final JPanel designsListPanel;

    private List<Design> cachedDesigns = new ArrayList<>();

    private Runnable onNewDesign;
    private java.util.function.Consumer<Design> onOpenDesign;
    private Runnable onAccountClick;
    private Runnable onHelpClick;
    private Runnable onLogout;

    public DashboardPanel() {
        designDAO = new DesignDAO();

        setLayout(new BorderLayout());
        setBackground(PANEL_BG);

        add(createSidebarContainer(), BorderLayout.WEST);

        JPanel mainPanel = new JPanel(new MigLayout("fill, insets 20, wrap 1, gapy 14", "[grow, fill]", "[][][grow, fill]"));
        mainPanel.setOpaque(false);

        JPanel headerCard = createSurfacePanel(new MigLayout("fill, insets 18", "[grow, fill][]", "[]"));
        JPanel headerText = new JPanel(new MigLayout("insets 0, wrap 1, gapy 2", "[grow, fill]", "[]"));
        headerText.setOpaque(false);

        welcomeLabel = new JLabel("Welcome!");
        welcomeLabel.setFont(titleFont);
        welcomeLabel.setForeground(new Color(25, 25, 25));
        headerText.add(welcomeLabel);

        subWelcomeLabel = new JLabel("Start from a new design or continue where you left off.");
        subWelcomeLabel.setFont(subtitleFont);
        subWelcomeLabel.setForeground(MUTED_TEXT);
        headerText.add(subWelcomeLabel);

        JPanel headerActions = new JPanel(new MigLayout("insets 0, gapx 8", "[][]", "[]"));
        headerActions.setOpaque(false);

        JButton newDesignButton = createPrimaryButton("+ New Design");
        newDesignButton.addActionListener(e -> {
            if (onNewDesign != null) {
                onNewDesign.run();
            }
        });
        headerActions.add(newDesignButton, "h 50!, w 180!");

        openLatestButton = createGhostButton("Open Latest");
        openLatestButton.addActionListener(e -> {
            if (!cachedDesigns.isEmpty() && onOpenDesign != null) {
                onOpenDesign.accept(cachedDesigns.get(0));
            }
        });
        headerActions.add(openLatestButton, "h 50!, w 180!");

        headerCard.add(headerText, "growx, pushx");
        headerCard.add(headerActions, "right");
        mainPanel.add(headerCard, "growx");

        JPanel statsRow = new JPanel(new MigLayout("fill, insets 0, gapx 12", "[grow, fill][grow, fill][grow, fill][grow, fill]", "[]"));
        statsRow.setOpaque(false);

        designCountValue = new JLabel("0");
        furnitureCountValue = new JLabel("0");
        avgRoomValue = new JLabel("0.0 m2");
        lastUpdatedValue = new JLabel("No activity");

        statsRow.add(createStatCard("Designs", designCountValue));
        statsRow.add(createStatCard("Furniture Items", furnitureCountValue));
        statsRow.add(createStatCard("Average Room Area", avgRoomValue));
        statsRow.add(createStatCard("Last Updated", lastUpdatedValue));
        mainPanel.add(statsRow, "growx");

        JPanel contentRow = new JPanel(new MigLayout("fill, insets 0, gapx 12", "[grow 75, fill][grow 25, fill]", "[grow, fill]"));
        contentRow.setOpaque(false);

        JPanel recentCard = createSurfacePanel(new MigLayout("fill, insets 16, wrap 1, gapy 10", "[grow, fill]", "[][][grow, fill]"));
        JLabel recentTitle = new JLabel("Recent Designs");
        recentTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        recentTitle.setForeground(new Color(30, 30, 30));
        recentCard.add(recentTitle);

        JLabel recentHint = new JLabel("Double-click a card to open quickly.");
        recentHint.setFont(bodyFont);
        recentHint.setForeground(MUTED_TEXT);
        recentCard.add(recentHint);

        designsListPanel = new JPanel(new MigLayout("fillx, insets 0, wrap 1, gapy 10", "[grow, fill]", ""));
        designsListPanel.setOpaque(false);

        JScrollPane designsScroll = new JScrollPane(designsListPanel);
        designsScroll.setBorder(null);
        designsScroll.setOpaque(false);
        designsScroll.getViewport().setOpaque(false);
        designsScroll.getVerticalScrollBar().setUnitIncrement(16);
        recentCard.add(designsScroll, "grow, push");

        contentRow.add(recentCard, "grow, push");

        JPanel sideStack = new JPanel(new MigLayout("fill, insets 0, wrap 1, gapy 12", "[grow, fill]", "[][]"));
        sideStack.setOpaque(false);

        JPanel quickActions = createSurfacePanel(new MigLayout("fill, insets 16, wrap 1, gapy 8", "[grow, fill]", ""));
        JLabel quickTitle = new JLabel("Quick Actions");
        quickTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        quickActions.add(quickTitle);

        JButton accountButton = createGhostButton("Account Settings");
        accountButton.addActionListener(e -> {
            if (onAccountClick != null) {
                onAccountClick.run();
            }
        });
        quickActions.add(accountButton, "growx, h 50!");

        JButton helpButton = createGhostButton("Help & Shortcuts");
        helpButton.addActionListener(e -> {
            if (onHelpClick != null) {
                onHelpClick.run();
            }
        });
        quickActions.add(helpButton, "growx, h 50!");

        JButton logoutButton = createGhostButton("Logout");
        styleLogoutButton(logoutButton);
        logoutButton.addActionListener(e -> {
            if (onLogout != null) {
                onLogout.run();
            }
        });
        quickActions.add(logoutButton, "growx, h 50!");

        JPanel tipsCard = createSurfacePanel(new MigLayout("fill, insets 16, wrap 1, gapy 6", "[grow, fill]", ""));
        JLabel tipsTitle = new JLabel("Design Tips");
        tipsTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        tipsCard.add(tipsTitle);
        tipsCard.add(createTipLabel("1. Start with room size before adding furniture."));
        tipsCard.add(createTipLabel("2. Keep walking paths at least 0.8m clear."));
        tipsCard.add(createTipLabel("3. Group related furniture to improve flow."));

        sideStack.add(quickActions, "growx");
        sideStack.add(tipsCard, "grow, push");
        contentRow.add(sideStack, "grow, push");

        mainPanel.add(contentRow, "grow, push");

        add(mainPanel, BorderLayout.CENTER);

        openLatestButton.setEnabled(false);
    }

    private JPanel createSidebarContainer() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 0));
        wrapper.add(createSidebar(), BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new MigLayout("fillx, insets 18, wrap 1, gapy 8", "[grow, fill]", "[][][][][][push][]"));
        sidebar.setBackground(new Color(250, 250, 250));
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(225, 225, 225)));

        JLabel brand = new JLabel("FurnitureFit");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 24));
        brand.setForeground(GREEN);
        sidebar.add(brand, "gaptop 8");

        JLabel tag = new JLabel("Design Workspace");
        tag.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tag.setForeground(MUTED_TEXT);
        sidebar.add(tag, "gapbottom 18");

        JButton dashboardBtn = createSidebarButton("Dashboard", true, false);
        dashboardBtn.addActionListener(e -> refreshDesigns());
        sidebar.add(dashboardBtn, "growx, h 50!");

        JButton newBtn = createSidebarButton("New Design", false, true);
        newBtn.addActionListener(e -> {
            if (onNewDesign != null) {
                onNewDesign.run();
            }
        });
        sidebar.add(newBtn, "growx, h 50!");

        JButton accountBtn = createSidebarButton("Account", false, true);
        accountBtn.addActionListener(e -> {
            if (onAccountClick != null) {
                onAccountClick.run();
            }
        });
        sidebar.add(accountBtn, "growx, h 50!");

        JButton helpBtn = createSidebarButton("Help", false, true);
        helpBtn.addActionListener(e -> {
            if (onHelpClick != null) {
                onHelpClick.run();
            }
        });
        sidebar.add(helpBtn, "growx, h 50!");

        JButton logoutBtn = createSidebarButton("Logout", false, false);
        styleLogoutButton(logoutBtn);
        logoutBtn.addActionListener(e -> {
            if (onLogout != null) {
                onLogout.run();
            }
        });
        sidebar.add(logoutBtn, "growx, h 50!");

        return sidebar;
    }

    private JPanel createSurfacePanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(220, 220, 220), 1, 14),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        return panel;
    }

    private JPanel createStatCard(String title, JLabel valueLabel) {
        JPanel card = createSurfacePanel(new MigLayout("fill, insets 12, wrap 1, gapy 4", "[grow, fill]", "[]"));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(bodyFont);
        titleLabel.setForeground(MUTED_TEXT);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLabel.setForeground(new Color(30, 30, 30));

        card.add(titleLabel);
        card.add(valueLabel);
        return card;
    }

    private JLabel createTipLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(bodyFont);
        label.setForeground(new Color(70, 70, 70));
        return label;
    }

    private Font createMediumWeightFont(float size) {
        Map<TextAttribute, Object> attrs = new HashMap<>();
        attrs.put(TextAttribute.FAMILY, "Segoe UI");
        attrs.put(TextAttribute.SIZE, size);
        attrs.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        return new Font(attrs);
    }

    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(buttonFont);
        button.setForeground(Color.WHITE);
        button.setBackground(GREEN);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton createGhostButton(String text) {
        JButton button = new JButton(text);
        button.setFont(buttonFont);
        button.setForeground(Color.WHITE);
        button.setBackground(GREEN);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton createSidebarButton(String text, boolean active, boolean hoverEffect) {
        JButton button = new JButton(text);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMargin(new Insets(0, 16, 0, 10));
        button.setFont(buttonFont);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (active) {
            button.setBackground(new Color(35, 122, 35));
            button.setForeground(Color.WHITE);
            button.setEnabled(true);
        } else {
            button.setBackground(GREEN);
            button.setForeground(Color.WHITE);
            if (hoverEffect) {
                button.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        button.setBackground(new Color(35, 122, 35));
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        button.setBackground(GREEN);
                    }
                });
            }
        }

        return button;
    }

    private void styleDangerButton(JButton button) {
        button.setForeground(new Color(190, 50, 50));
        button.setBackground(Color.WHITE);
        button.setBorderPainted(true);
        button.setBorder(BorderFactory.createLineBorder(new Color(220, 140, 140), 1));
        button.setOpaque(true);
    }

    private void styleLogoutButton(JButton button) {
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(190, 50, 50));
        button.setBorderPainted(false);
        button.setOpaque(true);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;

        String name = user != null ? user.getFullName() : "Designer";
        welcomeLabel.setText("Welcome, " + name + "!");
        subWelcomeLabel.setText("Continue your design work with clear next actions.");
        refreshDesigns();
    }

    public void refreshDesigns() {
        if (currentUser == null) {
            return;
        }

        List<Design> designs = new ArrayList<>(designDAO.findByUserId(currentUser.getId()));
        designs.sort(Comparator.comparing(this::activityKey).reversed());
        cachedDesigns = designs;

        updateStats(designs);
        openLatestButton.setEnabled(!designs.isEmpty());

        designsListPanel.removeAll();

        if (designs.isEmpty()) {
            JPanel emptyCard = createSurfacePanel(new MigLayout("fill, insets 20, wrap 1, gapy 8", "[grow, fill]", ""));
            JLabel emptyTitle = new JLabel("No saved designs yet");
            emptyTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
            emptyTitle.setForeground(new Color(45, 45, 45));

            JLabel emptyHint = new JLabel("Create your first design to start planning your room.");
            emptyHint.setFont(bodyFont);
            emptyHint.setForeground(MUTED_TEXT);

            JButton startButton = createPrimaryButton("Create First Design");
            startButton.addActionListener(e -> {
                if (onNewDesign != null) {
                    onNewDesign.run();
                }
            });

            emptyCard.add(emptyTitle);
            emptyCard.add(emptyHint);
            emptyCard.add(startButton, "w 180!, h 38!, gaptop 6");
            designsListPanel.add(emptyCard, "growx");
        } else {
            int displayCount = Math.min(8, designs.size());
            for (int i = 0; i < displayCount; i++) {
                designsListPanel.add(createDesignCard(designs.get(i)), "growx");
            }
        }

        designsListPanel.revalidate();
        designsListPanel.repaint();
    }

    private void updateStats(List<Design> designs) {
        int designCount = designs.size();
        int furnitureCount = 0;
        double totalArea = 0;

        for (Design design : designs) {
            if (design.getFurnitureList() != null) {
                furnitureCount += design.getFurnitureList().size();
            }
            Room room = design.getRoom();
            if (room != null) {
                totalArea += room.getWidth() * room.getDepth();
            }
        }

        designCountValue.setText(String.valueOf(designCount));
        furnitureCountValue.setText(String.valueOf(furnitureCount));

        if (designCount > 0) {
            double avg = totalArea / designCount;
            avgRoomValue.setText(String.format("%.1f m2", avg));
            lastUpdatedValue.setText(shortDate(activityKey(designs.get(0))));
        } else {
            avgRoomValue.setText("0.0 m2");
            lastUpdatedValue.setText("No activity");
        }
    }

    private JPanel createDesignCard(Design design) {
        JPanel card = createSurfacePanel(new MigLayout("fill, insets 12, gapx 12", "[120!, fill][grow, fill][right]", "[100!, fill]"));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel preview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                drawDesignPreview(g2d, design, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        preview.setBackground(new Color(245, 245, 245));
        preview.setBorder(new RoundedBorder(new Color(220, 220, 220), 1, 10));
        card.add(preview, "grow");

        JPanel infoPanel = new JPanel(new MigLayout("fillx, insets 0, wrap 1, gapy 4", "[grow, fill]", ""));
        infoPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(design.getName() != null ? design.getName() : "Untitled Design");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        nameLabel.setForeground(new Color(30, 30, 30));
        infoPanel.add(nameLabel);

        int itemCount = design.getFurnitureList() == null ? 0 : design.getFurnitureList().size();
        Room room = design.getRoom();
        String roomLabel = room == null
                ? "Room details unavailable"
                : String.format("Room %.1fm x %.1fm (%s)", room.getWidth(), room.getDepth(), room.getShape().getDisplayName());

        JLabel roomInfo = new JLabel(roomLabel);
        roomInfo.setFont(bodyFont);
        roomInfo.setForeground(MUTED_TEXT);
        infoPanel.add(roomInfo);

        JLabel itemInfo = new JLabel(itemCount + " furniture item" + (itemCount == 1 ? "" : "s"));
        itemInfo.setFont(bodyFont);
        itemInfo.setForeground(MUTED_TEXT);
        infoPanel.add(itemInfo);

        JLabel updatedInfo = new JLabel("Updated " + shortDate(activityKey(design)));
        updatedInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        updatedInfo.setForeground(new Color(120, 120, 120));
        infoPanel.add(updatedInfo);

        card.add(infoPanel, "growx");

        JPanel actions = new JPanel(new MigLayout("insets 0, wrap 1, gapy 6", "[110!, fill]", ""));
        actions.setOpaque(false);

        JButton openButton = createPrimaryButton("Open");
        openButton.addActionListener(e -> openDesign(design));
        actions.add(openButton, "h 42!");

        JButton deleteButton = createGhostButton("Delete");
        styleDangerButton(deleteButton);
        deleteButton.addActionListener(e -> deleteDesign(design));
        actions.add(deleteButton, "h 42!");

        card.add(actions, "top");

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openDesign(design);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(new Color(155, 195, 155), 1, 14),
                        BorderFactory.createEmptyBorder(0, 0, 0, 0)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(new Color(220, 220, 220), 1, 14),
                        BorderFactory.createEmptyBorder(0, 0, 0, 0)
                ));
            }
        });

        return card;
    }

    private void openDesign(Design design) {
        if (onOpenDesign != null) {
            onOpenDesign.accept(design);
        }
    }

    private void deleteDesign(Design design) {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete design '" + design.getName() + "'?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            designDAO.delete(design.getId(), currentUser.getId());
            refreshDesigns();
        }
    }

    private void drawDesignPreview(Graphics2D g2d, Design design, int w, int h) {
        g2d.setColor(new Color(250, 250, 250));
        g2d.fillRect(0, 0, w, h);

        Room room = design.getRoom();
        if (room == null) {
            g2d.setColor(MUTED_TEXT);
            g2d.drawString("No room", 12, h / 2);
            return;
        }

        double roomW = Math.max(0.1, room.getWidth());
        double roomD = Math.max(0.1, room.getDepth());

        double scale = Math.min((w - 20.0) / roomW, (h - 20.0) / roomD);
        double cx = w / 2.0;
        double cy = h / 2.0;

        int rx = (int) (cx - roomW * scale / 2);
        int ry = (int) (cy - roomD * scale / 2);
        int rw = Math.max(1, (int) (roomW * scale));
        int rh = Math.max(1, (int) (roomD * scale));

        g2d.setColor(room.getFloorColor());
        g2d.fillRoundRect(rx, ry, rw, rh, 8, 8);
        g2d.setColor(new Color(120, 120, 120));
        g2d.drawRoundRect(rx, ry, rw, rh, 8, 8);

        if (design.getFurnitureList() == null) {
            return;
        }

        for (Furniture furniture : design.getFurnitureList()) {
            int fx = (int) (cx + furniture.getX() * scale - furniture.getWidth() * scale / 2);
            int fy = (int) (cy + furniture.getY() * scale - furniture.getDepth() * scale / 2);
            int fw = Math.max(2, (int) (furniture.getWidth() * scale));
            int fd = Math.max(2, (int) (furniture.getDepth() * scale));

            g2d.setColor(furniture.getColor());
            g2d.fillRoundRect(fx, fy, fw, fd, 4, 4);
            g2d.setColor(new Color(70, 70, 70));
            g2d.drawRoundRect(fx, fy, fw, fd, 4, 4);
        }
    }

    private String activityKey(Design design) {
        if (design.getUpdatedAt() != null && !design.getUpdatedAt().isBlank()) {
            return design.getUpdatedAt();
        }
        if (design.getCreatedAt() != null && !design.getCreatedAt().isBlank()) {
            return design.getCreatedAt();
        }
        return "0000-00-00";
    }

    private String shortDate(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return "N/A";
        }
        return timestamp.length() >= 10 ? timestamp.substring(0, 10) : timestamp;
    }

    private static class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int thickness;
        private final int radius;

        RoundedBorder(Color color, int thickness, int radius) {
            this.color = color;
            this.thickness = thickness;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness, thickness, thickness, thickness);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = thickness;
            insets.right = thickness;
            insets.top = thickness;
            insets.bottom = thickness;
            return insets;
        }
    }

    // ---- Setters for callbacks ----
    public void setOnNewDesign(Runnable callback) {
        this.onNewDesign = callback;
    }

    public void setOnOpenDesign(java.util.function.Consumer<Design> callback) {
        this.onOpenDesign = callback;
    }

    public void setOnAccountClick(Runnable callback) {
        this.onAccountClick = callback;
    }

    public void setOnHelpClick(Runnable callback) {
        this.onHelpClick = callback;
    }

    public void setOnLogout(Runnable callback) {
        this.onLogout = callback;
    }
}
