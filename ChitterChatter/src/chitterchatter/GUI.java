package chitterchatter;

/**
 * @author Kyle Bashford
 * @author Bo Guan
 * @author David Diez-Perez
 * @author Jason Rice
 * 
 * special thanks to Mark Williams
 * 
 */

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.MaskFormatter;

public class GUI implements ActionListener, WindowListener, KeyListener {

    private static final String NEWLINE = "\n";
    private final int MSGSIZE = 512;
    private String username = null;
    private final int width = 600;
    private final int height = 400;
    private final JFrame frame;
    private final JPanel panel;
    private final JTextArea msgArea;
    private final JTextArea msgInput;
    private final JButton send;
    private final JMenuBar menuBar;
    private final JMenu menu;
    private final JMenuItem usernames;
    private final JMenuItem getChat;
    private final JMenuItem kick;
    private final JMenuItem leave;
    private final JScrollPane msgPane;
    private final JScrollPane inputPane;
    private final JPanel inputPanel;

    ClientListener cl;
    boolean voting = false;

    public GUI() throws IOException {

        frame = new JFrame("Chitter Chatter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(width / 2, height / 2));

        menuBar = new JMenuBar();
        menu = new JMenu("menu");
        menuBar.add(menu);
        usernames = new JMenuItem("Get Usernames");
        menu.add(usernames);
        getChat = new JMenuItem("Get Chat");
        menu.add(getChat);
        kick = new JMenuItem("Kick User");
        menu.add(kick);
        leave = new JMenuItem("Leave Chat");
        menu.add(leave);
        frame.setJMenuBar(menuBar);

        panel = new JPanel();
        panel.setPreferredSize(new Dimension(width, height));

        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        msgArea = new JTextArea();
        msgArea.setEditable(false);
        msgArea.setLineWrap(true);
        msgArea.setWrapStyleWord(true);
        msgPane = new JScrollPane(msgArea);
        msgPane.setPreferredSize(new Dimension(width - 10, height
                - (height / 3)));

        msgInput = new JTextArea();
        msgInput.setLineWrap(true);
        msgInput.setWrapStyleWord(true);
        inputPane = new JScrollPane(msgInput);
        inputPane.setPreferredSize(new Dimension((width - 10) - (width / 5),
                (height / 3) - 20));

        send = new JButton("Send");
        send.setMinimumSize(new Dimension(70, 30));
        send.setPreferredSize(new Dimension(((width - 10) - inputPane
                .getPreferredSize().width) - 20,
                inputPane.getPreferredSize().height / 2));

        inputPanel = new JPanel();
        inputPanel.add(inputPane);
        inputPanel.add(send);
        GroupLayout sLayout = new GroupLayout(inputPanel);
        inputPanel.setLayout(sLayout);
        sLayout.setAutoCreateGaps(true);
        sLayout.setAutoCreateContainerGaps(true);
        sLayout.setHorizontalGroup(sLayout
                .createSequentialGroup()
                .addComponent(inputPane)
                .addComponent(send, send.getPreferredSize().width,
                        send.getPreferredSize().width,
                        send.getPreferredSize().width));
        sLayout.setVerticalGroup(sLayout.createSequentialGroup().addGroup(
                sLayout.createParallelGroup()
                .addComponent(inputPane)
                .addComponent(send, send.getPreferredSize().height,
                        send.getPreferredSize().height,
                        send.getPreferredSize().height)));

        layout.setHorizontalGroup(layout.createSequentialGroup().addGroup(
                layout.createParallelGroup().addComponent(msgPane)
                .addComponent(inputPanel)));
        layout.setVerticalGroup(layout.createSequentialGroup()
                .addComponent(msgPane).addComponent(inputPanel));

        DefaultCaret caret = (DefaultCaret) msgArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        msgInput.addKeyListener(this);

        // add action listeners
        menu.addActionListener(this);
        usernames.addActionListener(this);
        getChat.addActionListener(this);
        kick.addActionListener(this);
        leave.addActionListener(this);
        send.addActionListener(this);

        panel.add(msgPane);
        panel.add(inputPanel);

        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.addWindowListener(this);
        msgInput.requestFocus();
        getUsername();
    }

    public String getUserName() {
        return username;
    }

    public void displayMsg(String username, String msg) {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm");
        Date date = new Date();
        msgArea.append(dateFormat.format(date) + " - [" + username.trim()
                + "] " + msg + NEWLINE);
    }

    public void printDemUsers(String username, ArrayList<String> usernamesList) {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm");
        Date date = new Date();
        msgArea.append(dateFormat.format(date) + " - [" + username.trim()
                + "] " + "LIST OF CURRENT USERS IN THE SESSION: " + NEWLINE);
        int i = 0;
        msgArea.append("     ");
        for (String s : usernamesList) {
            msgArea.append(s + " ");
            i++;
            if (i == 5) {
                i = 0;
                msgArea.append(NEWLINE + "     ");
            }
        }
        msgArea.append(NEWLINE);
    }

    public void printSomeMsgs(String username, ArrayList<String> msgs) {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm");
        Date date = new Date();
        msgArea.append(dateFormat.format(date) + " - [" + username.trim()
                + "] " + "Printing Previous Messages: " + NEWLINE);
        for (String s : msgs) {
            msgArea.append(s + NEWLINE);
        }
        msgArea.append("End of Messages" + NEWLINE);
    }

    private void handleInput() {
        String input = msgInput.getText().trim();
        if (input.length() > 0) {
            System.out.println("Text Entered: " + input);
            if (cl.socket.isClosed() == true) {
                displayMsg("ChitterChatter",
                        "You are currently not connected to the server");
            }
        }
        msgInput.setText("");
        cl.MessageFromGUI(input);
    }

    private void startKick() {
        cl.getUsernamesForGui();
    }

    public void kick(String[] usernames) {
        System.out.println(usernames.length);
        String toKick = (String) JOptionPane.showInputDialog(frame,
                "Select who to kick:", "Kick", JOptionPane.PLAIN_MESSAGE, null,
                usernames, usernames[0]);
        if (toKick != null) {
            System.out.println(toKick);
            cl.startKickUserVote(toKick);
        }
    }

    public void kickRequest(String username) {
        voting = true;
        long start = System.currentTimeMillis();
        int response = JOptionPane.showConfirmDialog(frame,
                "Do you want to kick: " + username + "?", "Kick Request",
                JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (response == 0) {
            // kick
            cl.sendVoteFromClient(username, true);
            System.out.println("Kick");
        } else if (response == 1) {
            // don't kick
            cl.sendVoteFromClient(username, false);
            System.out.println("Don't Kick");
        }
        WaitDialog wd = new WaitDialog(frame, start);
        new Thread(wd).start();
        wd.setVisible(true);
    }

    public void kickUser(String user) {
        if (username.equals(user)) {
            System.exit(0);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == usernames) {
            cl.reqUsernames();
            System.out.println("usernames");
        } else if (e.getSource() == getChat) {
            System.out.println("getChat");
            cl.getChat();
        } else if (e.getSource() == kick) {
            System.out.println("kick");
            if (!voting) {
                startKick();
            }
        } else if (e.getSource() == leave) {
            System.out.println("leave");
            cl.sendLeavePacket();
            System.exit(0);
        } else if (e.getSource() == send) {
            System.out.println("send");
            handleInput();
        }
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        // send leave msg
        cl.sendLeavePacket();
        System.out.println("closing");
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowOpened(WindowEvent e) {
        System.out.println("Open");
    }

    private void getUsername() {
        while (true) {
            CustomDialog cd = new CustomDialog(frame);
            cd.pack();
            cd.setLocationRelativeTo(frame);
            cd.setVisible(true);
            username = cd.getInput();

            if (username == null) {
                System.exit(0);
            }
            if (username.trim().equals("")
                    || username.trim().equalsIgnoreCase("server")) {
            } else {
                System.out.println("Username: " + username);
                try {
                    cl = new ClientListener(this);
                    return;
                } catch (IOException ex) {
                    Logger.getLogger(GUI.class.getName()).log(Level.SEVERE,
                            null, ex);
                }
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            e.consume();
            // send msg
            System.out.println("send");
            handleInput();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (msgInput.getText().length() >= MSGSIZE) {
            e.consume();
            try {
                msgInput.setText(msgInput.getText(0, MSGSIZE));
            } catch (BadLocationException e1) {
                e1.printStackTrace();
            }
            System.out.println("max message size reached");
        }
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    new GUI();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static class CustomDialog extends JDialog implements
            ActionListener, PropertyChangeListener {

        private String input = null;
        private JFormattedTextField inputField;

        private JOptionPane optionPane;

        private final String buttonText1 = "Enter";
        private final String buttonText2 = "Quit";

        public CustomDialog(Frame frame) {
            super(frame, true);

            setTitle("Enter Username");
            MaskFormatter formatter = null;
            try {
                formatter = new MaskFormatter("************");
            } catch (java.text.ParseException exc) {
                System.err.println("formatter is bad: " + exc.getMessage());
                System.exit(-1);
            }
            inputField = new JFormattedTextField(formatter);
            String msg = "Enter Username";

            Object[] array = {msg, inputField};
            Object[] options = {buttonText1, buttonText2};

            optionPane = new JOptionPane(array, JOptionPane.PLAIN_MESSAGE,
                    JOptionPane.YES_NO_OPTION, null, options, options[0]);
            setContentPane(optionPane);
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    optionPane.setValue(JOptionPane.CLOSED_OPTION);
                }
            });
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentShown(ComponentEvent e) {
                    inputField.requestFocusInWindow();
                }
            });

            inputField.addActionListener(this);
            optionPane.addPropertyChangeListener(this);
            setResizable(false);
        }

        @Override
        public void propertyChange(PropertyChangeEvent e) {
            String prop = e.getPropertyName();
            if (isVisible()
                    && (e.getSource() == optionPane)
                    && (JOptionPane.VALUE_PROPERTY.equals(prop) || JOptionPane.INPUT_VALUE_PROPERTY
                    .equals(prop))) {
                Object value = optionPane.getValue();

                if (value == JOptionPane.UNINITIALIZED_VALUE) {
                    return;
                }

                optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
                if (buttonText1.equals(value)) {
                    input = inputField.getText();
                    shutdown();
                } else {
                    input = null;
                    shutdown();
                }
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            optionPane.setValue(buttonText1);
        }

        public String getInput() {
            return input;
        }

        public void shutdown() {
            inputField.setText(null);
            setVisible(false);
        }
    }

    private static class WaitDialog extends JDialog implements Runnable {

        private long start;
        private long current;
        private JOptionPane optionPane;

        public WaitDialog(Frame frame, long s) {
            super(frame, true);
            System.out.println("MAKE");
            start = s;
            setTitle("Voting in Session");
            setModal(true);
            optionPane = new JOptionPane("Please Wait",
                    JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION,
                    null, new Object[]{}, null);
            setContentPane(optionPane);
            setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            pack();
            setResizable(false);
            setLocationRelativeTo(null);
        }

        @Override
        public void run() {
            System.out.println("Start");
            while (true) {
                current = System.currentTimeMillis();
                optionPane.setMessage("Please Wait: " + (30 - (int) ((current - start) / 1000)) + "s");
                if (current - start > (30000)) {
                    dispose();
                    break;
                }
            }
        }
    }
}
