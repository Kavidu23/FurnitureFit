package com.mycompany.furnituredesignapp.ui;

import com.mycompany.furnituredesignapp.model.Furniture;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.ToLongFunction;

/** Improved Cart dialog */
public final class EditorCartDialog {

    private static final Color GREEN = new Color(45, 136, 45);
    private static final int DIALOG_W = 861;
    private static final int DIALOG_H = 450;

    private EditorCartDialog() {}

    public static void show(Component parent,
                            List<Furniture> items,
                            ToLongFunction<Furniture.Type> priceResolver) {

        Frame owner = (Frame) SwingUtilities.getWindowAncestor(parent);
        JDialog cartDlg = new JDialog(owner, true);

        cartDlg.setUndecorated(true);
        cartDlg.setBackground(new Color(0, 0, 0, 0));

        // ===== Main Card =====
        JPanel card = new JPanel(
                new MigLayout(
                        "wrap 1, insets 28 40 28 40, gapy 12",
                        "[grow, fill]"
                )
        ) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(220, 220, 220));
                g2.drawRoundRect(0, 0, getWidth() - 1,
                        getHeight() - 1, 20, 20);
                g2.dispose();
            }
        };
        card.setOpaque(false);

        // ===== Header =====
        JPanel headerRow = new JPanel(new MigLayout("insets 0", "[grow][]"));
        headerRow.setOpaque(false);

        JLabel cartTitle = new JLabel("Cart");
        cartTitle.setFont(new Font("Segoe UI", Font.BOLD, 34));
        cartTitle.setForeground(new Color(30, 30, 30));

        JButton closeCart = new JButton("X");
        closeCart.setFont(new Font("Segoe UI", Font.BOLD, 16));
        closeCart.setForeground(new Color(100, 100, 100));
        closeCart.setBorderPainted(false);
        closeCart.setContentAreaFilled(false);
        closeCart.setFocusPainted(false);
        closeCart.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeCart.addActionListener(e -> cartDlg.dispose());

        headerRow.add(cartTitle);
        headerRow.add(closeCart);

        card.add(headerRow, "growx, gapbottom 20");

        // ===== Column Headers =====
        JPanel colHdr = new JPanel(
                new MigLayout("insets 0",
                        "[grow 70][grow 10][grow 20, right]")
        );
        colHdr.setOpaque(false);

        Font hdrFont = new Font("Segoe UI", Font.BOLD, 15);
        Color hdrClr = new Color(130, 130, 130);

        JLabel h1 = new JLabel("Item");
        h1.setFont(hdrFont);
        h1.setForeground(hdrClr);

        JLabel h2 = new JLabel("Qty");
        h2.setFont(hdrFont);
        h2.setForeground(hdrClr);

        JLabel h3 = new JLabel("Price");
        h3.setFont(hdrFont);
        h3.setForeground(hdrClr);

        colHdr.add(h1);
        colHdr.add(h2, "center");
        colHdr.add(h3, "right");

        card.add(colHdr, "growx");
        card.add(new JSeparator(), "growx, gapbottom 8");

        // ===== Items =====
        long total = 0;
        StringBuilder billText = new StringBuilder();
        billText.append("======= FURNITURE BILL =======\n\n");

        for (Furniture f : items) {

            long price = priceResolver.applyAsLong(f.getType());
            total += price;

            billText.append(f.getName())
                    .append("  -  Rs. ")
                    .append(String.format("%,d", price))
                    .append("\n");

            JPanel row = new JPanel(
                    new MigLayout("insets 6 0 6 0",
                            "[grow 70][grow 10][grow 20, right]")
            );
            row.setOpaque(false);

            JPanel dot = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(f.getColor());
                    g2.fillOval(0, 2, 10, 10);
                    g2.dispose();
                }
            };
            dot.setPreferredSize(new Dimension(10, 14));
            dot.setOpaque(false);

            JLabel nameL = new JLabel(f.getName());
            nameL.setFont(new Font("Segoe UI", Font.PLAIN, 17));

            JPanel nameRow = new JPanel(new FlowLayout(
                    FlowLayout.LEFT, 6, 0));
            nameRow.setOpaque(false);
            nameRow.add(dot);
            nameRow.add(nameL);

            JLabel qtyL = new JLabel("x1");
            qtyL.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            qtyL.setForeground(new Color(120, 120, 120));

            JLabel priceL = new JLabel("Rs. " +
                    String.format("%,d", price));
            priceL.setFont(new Font("Segoe UI", Font.PLAIN, 17));

            row.add(nameRow, "growx");
            row.add(qtyL, "center");
            row.add(priceL, "right");

            card.add(row, "growx");
        }

        card.add(new JSeparator(), "growx, gapy 12");

        // ===== Total Row =====
        JPanel totalRow = new JPanel(
                new MigLayout("insets 6 0 0 0",
                        "[grow][right]")
        );
        totalRow.setOpaque(false);

        JLabel totLabel = new JLabel("Total");
        totLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));

        JLabel totValue = new JLabel("Rs. " +
                String.format("%,d", total));
        totValue.setFont(new Font("Segoe UI", Font.BOLD, 22));
        totValue.setForeground(GREEN);

        totalRow.add(totLabel);
        totalRow.add(totValue);

        card.add(totalRow, "growx, gapbottom 20");

        billText.append("\n-------------------------------\n");
        billText.append("TOTAL : Rs. ")
                .append(String.format("%,d", total))
                .append("\n");
        billText.append("================================");

        // ===== Checkout Button =====
        JButton checkoutBtn = new JButton("Checkout");
        checkoutBtn.setBackground(GREEN);
        checkoutBtn.setForeground(Color.WHITE);
        checkoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 20));
        checkoutBtn.setFocusPainted(false);
        checkoutBtn.setBorderPainted(false);
        checkoutBtn.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        checkoutBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(
                    cartDlg,
                    billText.toString(),
                    "Bill Printed",
                    JOptionPane.INFORMATION_MESSAGE
            );
            cartDlg.dispose();
        });

        card.add(checkoutBtn, "growx, h 55!");

        // ===== Final Dialog Setup (with dim background overlay) =====
        card.setPreferredSize(new Dimension(DIALOG_W, DIALOG_H));

        JPanel overlay = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0, 0, 0, 115)); // darken parent view
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        overlay.setOpaque(false);
        overlay.add(card);

        cartDlg.setContentPane(overlay);
        cartDlg.setResizable(false);
        if (owner != null) {
            try {
                Point p = owner.getLocationOnScreen();
                cartDlg.setBounds(p.x, p.y, owner.getWidth(), owner.getHeight());
            } catch (IllegalComponentStateException ex) {
                cartDlg.setSize(Toolkit.getDefaultToolkit().getScreenSize());
                cartDlg.setLocationRelativeTo(parent);
            }
        } else {
            cartDlg.setSize(Toolkit.getDefaultToolkit().getScreenSize());
            cartDlg.setLocationRelativeTo(parent);
        }
        cartDlg.setVisible(true);
    }
}
