package com.mycompany.furnituredesignapp.ui;

import com.mycompany.furnituredesignapp.db.DesignDAO;
import com.mycompany.furnituredesignapp.model.Design;
import com.mycompany.furnituredesignapp.model.User;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Dashboard panel showing saved designs and navigation.
 */
public class DashboardPanel extends JPanel {

    private User currentUser;
    private final DesignDAO designDAO;
    private final JPanel designsGrid;
    private final JLabel welcomeLabel;

    private Runnable onNewDesign;
    private java.util.function.Consumer<Design> onOpenDesign;
    private Runnable onAccountClick;
    private Runnable onHelpClick;
    private Runnable onLogout;

    public DashboardPanel() {
        designDAO = new DesignDAO();
        setLayout(new BorderLayout());

        // ---- Left sidebar ----
        JPanel sidebar = createSidebar();
        add(sidebar, BorderLayout.WEST);

        // ---- Main content ----
        JPanel mainPanel = new JPanel(new MigLayout("wrap 1, insets 20", "[grow, fill]"));
        mainPanel.setBackground(new Color(245, 245, 245));

        // Header
        JPanel headerPanel = new JPanel(new MigLayout("insets 0", "[grow][]"));
        headerPanel.setOpaque(false);

        welcomeLabel = new JLabel("Welcome back!");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        welcomeLabel.setForeground(new Color(40, 40, 40));
        headerPanel.add(welcomeLabel);

        JButton newDesignBtn = new JButton("+ New Design");
        newDesignBtn.setBackground(new Color(56, 124, 43));
        newDesignBtn.setForeground(Color.WHITE);
        newDesignBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        newDesignBtn.setFocusPainted(false);
        newDesignBtn.setBorderPainted(false);
        newDesignBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        newDesignBtn.addActionListener(e -> { if (onNewDesign != null) onNewDesign.run(); });
        headerPanel.add(newDesignBtn, "h 38!");

        mainPanel.add(headerPanel);

        JLabel subtitle = new JLabel("Your Saved Designs");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(new Color(100, 100, 100));
        mainPanel.add(subtitle, "gaptop 10, gapbottom 10");

        // Designs grid
        designsGrid = new JPanel(new MigLayout("wrap 3, gapy 12, gapx 12", "[grow, fill][grow, fill][grow, fill]"));
        designsGrid.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(designsGrid);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mainPanel.add(scrollPane, "grow, push");

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new MigLayout("wrap 1, insets 15, gapy 5", "[grow, fill]"));
        sidebar.setBackground(new Color(255, 255, 255));
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(220, 220, 220)));

        // Logo
        JLabel logo = new JLabel("FurnitureFit");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        logo.setForeground(new Color(56, 124, 43));
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        sidebar.add(logo, "center, gapbottom 5");

        JLabel tagline = new JLabel("Design Studio");
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tagline.setForeground(new Color(120, 120, 120));
        tagline.setHorizontalAlignment(SwingConstants.CENTER);
        sidebar.add(tagline, "center, gapbottom 20");

        // Navigation
        JButton dashboardBtn = createSidebarButton("Dashboard", true, "D");
        dashboardBtn.addActionListener(e -> refreshDesigns());
        sidebar.add(dashboardBtn);

        JButton newBtn = createSidebarButton("New Design", false, "+");
        newBtn.addActionListener(e -> { if (onNewDesign != null) onNewDesign.run(); });
        sidebar.add(newBtn);

        sidebar.add(Box.createVerticalGlue(), "grow, push");

        JButton accountBtn = createSidebarButton("Account", false, "A");
        accountBtn.addActionListener(e -> { if (onAccountClick != null) onAccountClick.run(); });
        sidebar.add(accountBtn);

        JButton helpBtn = createSidebarButton("Help", false, "?");
        helpBtn.addActionListener(e -> { if (onHelpClick != null) onHelpClick.run(); });
        sidebar.add(helpBtn);

        JButton logoutBtn = createSidebarButton("Logout", false, "X");
        logoutBtn.setForeground(new Color(231, 76, 60));
        logoutBtn.addActionListener(e -> { if (onLogout != null) onLogout.run(); });
        sidebar.add(logoutBtn, "gaptop 5");

        return sidebar;
    }

    private JButton createSidebarButton(String text, boolean active, String iconChar) {
        // Create a button with a custom-drawn circular icon badge
        JButton btn = new JButton("   " + text) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int badgeSize = 22;
                int bx = 10;
                int by = (getHeight() - badgeSize) / 2;
                g2.setColor(active ? new Color(56, 124, 43, 40) : new Color(100, 120, 150, 40));
                g2.fillRoundRect(bx, by, badgeSize, badgeSize, 6, 6);
                g2.setColor(active ? Color.WHITE : new Color(80, 80, 80));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int tx = bx + (badgeSize - fm.stringWidth(iconChar)) / 2;
                int ty = by + (badgeSize + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(iconChar, tx, ty);
                g2.dispose();
            }
        };
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (active) {
            btn.setBackground(new Color(56, 124, 43));
            btn.setForeground(Color.WHITE);
            btn.setContentAreaFilled(true);
        } else {
            btn.setContentAreaFilled(false);
            btn.setForeground(new Color(80, 80, 80));
            btn.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    btn.setContentAreaFilled(true);
                    btn.setBackground(new Color(235, 245, 235));
                }
                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    btn.setContentAreaFilled(false);
                }
            });
        }
        return btn;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        welcomeLabel.setText("Welcome, " + user.getFullName() + "!");
        refreshDesigns();
    }

    public void refreshDesigns() {
        if (currentUser == null) return;

        designsGrid.removeAll();
        List<Design> designs = designDAO.findByUserId(currentUser.getId());

        if (designs.isEmpty()) {
            JLabel emptyLabel = new JLabel("No designs yet. Click '+ New Design' to get started!");
            emptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            emptyLabel.setForeground(new Color(120, 120, 120));
            designsGrid.add(emptyLabel, "span 3, center, gaptop 50");
        } else {
            for (Design design : designs) {
                designsGrid.add(createDesignCard(design));
            }
        }

        designsGrid.revalidate();
        designsGrid.repaint();
    }

    private JPanel createDesignCard(Design design) {
        JPanel card = new JPanel(new MigLayout("wrap 1, insets 12", "[grow, fill]"));
        card.setBackground(new Color(255, 255, 255));
        card.setBorder(BorderFactory.createLineBorder(new Color(210, 210, 210), 1));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Thumbnail preview
        JPanel preview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                drawDesignPreview(g2d, design, getWidth(), getHeight());
            }
        };
        preview.setPreferredSize(new Dimension(0, 120));
        preview.setBackground(new Color(240, 240, 240));
        card.add(preview, "growx");

        // Name
        JLabel nameLabel = new JLabel(design.getName());
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLabel.setForeground(new Color(40, 40, 40));
        card.add(nameLabel, "gaptop 8");

        // Details
        String details = String.format("Room: %.1f×%.1fm | %d items",
                design.getRoom().getWidth(), design.getRoom().getDepth(),
                design.getFurnitureList().size());
        JLabel detailLabel = new JLabel(details);
        detailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        detailLabel.setForeground(new Color(100, 100, 100));
        card.add(detailLabel);

        // Date
        JLabel dateLabel = new JLabel("Updated: " +
                (design.getUpdatedAt() != null ? design.getUpdatedAt().substring(0, 10) : "N/A"));
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        dateLabel.setForeground(new Color(130, 130, 130));
        card.add(dateLabel);

        // Buttons
        JPanel btnPanel = new JPanel(new MigLayout("insets 0", "[grow][grow]"));
        btnPanel.setOpaque(false);

        JButton openBtn = new JButton("Open");
        openBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        openBtn.setBackground(new Color(56, 124, 43));
        openBtn.setForeground(Color.WHITE);
        openBtn.setFocusPainted(false);
        openBtn.setBorderPainted(false);
        openBtn.addActionListener(e -> { if (onOpenDesign != null) onOpenDesign.accept(design); });

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        deleteBtn.setForeground(new Color(231, 76, 60));
        deleteBtn.setFocusPainted(false);
        deleteBtn.setBorderPainted(false);
        deleteBtn.setContentAreaFilled(false);
        deleteBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete design '" + design.getName() + "'?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                designDAO.delete(design.getId(), currentUser.getId());
                refreshDesigns();
            }
        });

        btnPanel.add(openBtn, "growx, h 28!");
        btnPanel.add(deleteBtn, "growx, h 28!");
        card.add(btnPanel, "gaptop 5");

        // Double-click to open
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && onOpenDesign != null) {
                    onOpenDesign.accept(design);
                }
            }
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBorder(BorderFactory.createLineBorder(new Color(56, 124, 43), 1));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBorder(BorderFactory.createLineBorder(new Color(210, 210, 210), 1));
            }
        });

        return card;
    }

    private void drawDesignPreview(Graphics2D g2d, Design design, int w, int h) {
        // Mini floor plan preview
        double roomW = design.getRoom().getWidth();
        double roomD = design.getRoom().getDepth();
        double scale = Math.min((w - 20.0) / roomW, (h - 20.0) / roomD);
        double cx = w / 2.0;
        double cy = h / 2.0;

        // Room outline
        int rx = (int) (cx - roomW * scale / 2);
        int ry = (int) (cy - roomD * scale / 2);
        int rw = (int) (roomW * scale);
        int rh = (int) (roomD * scale);

        g2d.setColor(design.getRoom().getFloorColor());
        g2d.fillRect(rx, ry, rw, rh);
        g2d.setColor(new Color(100, 100, 100));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRect(rx, ry, rw, rh);

        // Furniture
        for (var f : design.getFurnitureList()) {
            int fx = (int) (cx + f.getX() * scale - f.getWidth() * scale / 2);
            int fy = (int) (cy + f.getY() * scale - f.getDepth() * scale / 2);
            int fw = (int) (f.getWidth() * scale);
            int fd = (int) (f.getDepth() * scale);
            g2d.setColor(f.getColor());
            g2d.fillRect(fx, fy, fw, fd);
            g2d.setColor(new Color(60, 60, 60));
            g2d.drawRect(fx, fy, fw, fd);
        }
    }

    // ---- Setters for callbacks ----
    public void setOnNewDesign(Runnable callback) { this.onNewDesign = callback; }
    public void setOnOpenDesign(java.util.function.Consumer<Design> callback) { this.onOpenDesign = callback; }
    public void setOnAccountClick(Runnable callback) { this.onAccountClick = callback; }
    public void setOnHelpClick(Runnable callback) { this.onHelpClick = callback; }
    public void setOnLogout(Runnable callback) { this.onLogout = callback; }
}
