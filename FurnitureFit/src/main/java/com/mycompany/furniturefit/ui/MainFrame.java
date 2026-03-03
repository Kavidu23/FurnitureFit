package com.mycompany.furnituredesignapp.ui;

import com.mycompany.furnituredesignapp.model.Design;
import com.mycompany.furnituredesignapp.model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main application frame managing all panels via CardLayout.
 */
public class MainFrame extends JFrame {

    private static final String LOGIN_VIEW = "LOGIN";
    private static final String REGISTER_VIEW = "REGISTER";
    private static final String DASHBOARD_VIEW = "DASHBOARD";
    private static final String EDITOR_VIEW = "EDITOR";
    private static final String ACCOUNT_VIEW = "ACCOUNT";
    private static final String HELP_VIEW = "HELP";

    private final CardLayout cardLayout;
    private final JPanel contentPanel;

    private final LoginPanel loginPanel;
    private final RegisterPanel registerPanel;
    private final DashboardPanel dashboardPanel;
    private final DesignEditorPanel editorPanel;
    private final AccountPanel accountPanel;
    private final HelpPanel helpPanel;

    private User currentUser;

    public MainFrame() {
        setTitle("FurnitureFit - Design Studio");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 700));
        setPreferredSize(new Dimension(1280, 800));

        // CardLayout for screen switching
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        // Initialize all panels
        loginPanel = new LoginPanel();
        registerPanel = new RegisterPanel();
        dashboardPanel = new DashboardPanel();
        editorPanel = new DesignEditorPanel();
        accountPanel = new AccountPanel();
        helpPanel = new HelpPanel();

        // Add panels to card layout
        contentPanel.add(loginPanel, LOGIN_VIEW);
        contentPanel.add(registerPanel, REGISTER_VIEW);
        contentPanel.add(dashboardPanel, DASHBOARD_VIEW);
        contentPanel.add(editorPanel, EDITOR_VIEW);
        contentPanel.add(accountPanel, ACCOUNT_VIEW);
        contentPanel.add(helpPanel, HELP_VIEW);

        setContentPane(contentPanel);

        // Wire navigation callbacks
        setupNavigation();

        // Start at login
        cardLayout.show(contentPanel, LOGIN_VIEW);
        installCloseGuard();

        pack();
        setLocationRelativeTo(null);
    }

    private void installCloseGuard() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (editorPanel.isShowing() && !editorPanel.confirmExitEditorWithUnsavedChanges()) {
                    return;
                }
                dispose();
            }
        });
    }

    private void setupNavigation() {
        // Login â†’ Dashboard
        loginPanel.setOnLoginSuccess(() -> {
            currentUser = loginPanel.getLoggedInUser();
            dashboardPanel.setCurrentUser(currentUser);
            accountPanel.setCurrentUser(currentUser);
            cardLayout.show(contentPanel, DASHBOARD_VIEW);
        });

        // Login â†” Register
        loginPanel.setOnSwitchToRegister(() -> {
            registerPanel.reset();
            cardLayout.show(contentPanel, REGISTER_VIEW);
        });

        registerPanel.setOnSwitchToLogin(() -> {
            loginPanel.reset();
            cardLayout.show(contentPanel, LOGIN_VIEW);
        });

        registerPanel.setOnRegisterSuccess(() -> {
            loginPanel.reset();
            cardLayout.show(contentPanel, LOGIN_VIEW);
        });

        // Dashboard actions
        dashboardPanel.setOnNewDesign(() -> {
            Design newDesign = new Design(currentUser.getId(), "Untitled Design");
            editorPanel.setCurrentUser(currentUser);
            editorPanel.loadDesign(newDesign, false);
            cardLayout.show(contentPanel, EDITOR_VIEW);
        });

        dashboardPanel.setOnOpenDesign(design -> {
            editorPanel.setCurrentUser(currentUser);
            editorPanel.loadDesign(design);
            cardLayout.show(contentPanel, EDITOR_VIEW);
        });

        dashboardPanel.setOnAccountClick(() -> {
            accountPanel.setCurrentUser(currentUser);
            cardLayout.show(contentPanel, ACCOUNT_VIEW);
        });

        dashboardPanel.setOnHelpClick(() -> {
            cardLayout.show(contentPanel, HELP_VIEW);
        });

        dashboardPanel.setOnLogout(() -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to logout?",
                    "Confirm Logout", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                currentUser = null;
                loginPanel.reset();
                cardLayout.show(contentPanel, LOGIN_VIEW);
            }
        });

        // Editor â†’ Dashboard
        editorPanel.setOnBackToDashboard(() -> {
            dashboardPanel.refreshDesigns();
            cardLayout.show(contentPanel, DASHBOARD_VIEW);
        });

        // Editor popup logout
        editorPanel.setOnLogoutRequested(() -> {
            currentUser = null;
            loginPanel.reset();
            cardLayout.show(contentPanel, LOGIN_VIEW);
        });
        // Account â†’ Dashboard
        accountPanel.setOnBack(() -> {
            dashboardPanel.setCurrentUser(currentUser); // Refresh in case name changed
            cardLayout.show(contentPanel, DASHBOARD_VIEW);
        });

        // Help â†’ Dashboard
        helpPanel.setOnBack(() -> {
            cardLayout.show(contentPanel, DASHBOARD_VIEW);
        });
    }
}

