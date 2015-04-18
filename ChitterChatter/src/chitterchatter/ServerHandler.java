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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ServerHandler {


    static final int maxBufSize = 1050;
    static final int usernameLength = 12;
    static final int messageLength = 512;
    static InetAddress clientGroup;
    static TreeSet<String> serverOnlyUsernames, allUsernames, bannedUsers;
    // 12 for username, 4 for random string, 512 for message space
    static final int rafBlockSize = 528;
    static final int numOfStoredMessage = 20;
    static Pmraf raf;
    static PArray parr;
    static String servName;
    static short yesVotes, noVotes;

    public ServerHandler() throws UnknownHostException, IOException, ClassNotFoundException {

        raf = new Pmraf(rafBlockSize);
        parr = new PArray(numOfStoredMessage, raf);
        serverOnlyUsernames = new TreeSet<String>();
        allUsernames = new TreeSet<String>();
        bannedUsers = new TreeSet<String>();

    }

    short getCode(byte[] b) {
        byte byte0 = b[0];
        byte byte1 = b[1];
        short codeNum = (short) ((byte1 << 8) + byte0);
        return codeNum;
    }

    public void addMessage(byte[] b) throws IOException {
        byte[] tempUsername = new byte[usernameLength];
        byte[] tempMessage = new byte[messageLength];
        System.arraycopy(b, 2, tempUsername, 0, tempUsername.length);
        System.arraycopy(b, tempUsername.length + 2, tempMessage, 0, tempMessage.length);
        parr.put(tempUsername, tempMessage);
    }
    
        public void addMessageUpdate(byte[] b) throws IOException {
        byte[] tempUsername = new byte[usernameLength];
        byte[] tempMessage = new byte[messageLength];
        System.arraycopy(b, 0, tempUsername, 0, tempUsername.length);
        System.arraycopy(b, tempUsername.length, tempMessage, 0, tempMessage.length);
        parr.put(tempUsername, tempMessage);
    }

    public void leavingChat(byte[] b, short s) {
        String username;
        byte[] temp = new byte[usernameLength];
        System.arraycopy(b, 2, temp, 0, temp.length);
        username = new String(temp);
        // dont know if you want to add to message log that a user left chat
        allUsernames.remove(username);
        serverOnlyUsernames.remove(username);
        Server.updateServerUsers(s, username);
    }

    public void joinChat(byte[] b, short s) {
        String username;
        byte[] temp = new byte[usernameLength];
        System.arraycopy(b, 2, temp, 0, temp.length);
        username = new String(temp);
        allUsernames.add(username);
        serverOnlyUsernames.add(username);
        Server.updateServerUsers(s, username);
    }

    // method checks if there are available usernames for the client to use in chat
    // returns true if there is space
    public boolean availableUsername(byte[] b) {
        String username;
        byte[] temp = new byte[usernameLength];
        System.arraycopy(b, 2, temp, 0, temp.length);
        username = new String(temp);

        if (allUsernames.isEmpty() == false && serverOnlyUsernames.isEmpty() == false) {
            if (allUsernames.contains(username) == false && serverOnlyUsernames.contains(username) == false) {
                if (bannedUsers.isEmpty() == true) {
                    // case where there are users in the list and your name is available, and no banned users
                    return true;
                } else {
                    // case where there are users in the list, but there are banned users to look for
                    return bannedUsers.isEmpty() == false && bannedUsers.contains(username) == false;
                }

            } else {
                // case where the name is taken
                return false;
            }
        } else {
            // case where no users are on, but no there is a banned list

            if (bannedUsers.isEmpty() == true) {
                return true;
            } else {
                return !bannedUsers.contains(username);
            }

        }

    }

    public String[][] getAllMessages() {
        String[][] temp;
        temp = parr.getAll();
        return temp;
    }

    public String[] getLatest() {
        String[] temp;
        temp = parr.getLastest();
        return temp;
    }

    public void countUpYesVote() {
        yesVotes++;
    }

    public void countUpNoVotes() {
        noVotes++;
    }

    public void clearVotes() {
        yesVotes = 0;
        noVotes = 0;
    }

    public boolean tallyVotes(short yvotes, short nvotes) {
        short totalVotes = (short) (yvotes + nvotes);
        System.out.println("Voting Results: Total Votes: " + totalVotes);
        System.out.println("Voting Results: Yes Votes:   " + yvotes);
        System.out.println("Voting Results: No Votes:    " + nvotes);
        if (totalVotes != 0) {
            double results = (double) yvotes / (double) totalVotes;

            double threshold = .65;
            return results > threshold;
        } else {
            return false;
        }
    }

    public short getYVotes() {
        return yesVotes;
    }

    public short getNVotes() {
        return noVotes;
    }

    public void kickUserFromChat(String username) {
        System.out.println("The User: " + username + " is being banned");
        allUsernames.remove(username);
        serverOnlyUsernames.remove(username);
        bannedUsers.add(username);
        bannedWaitList(username);
    }

    public String[] getMessageByNumber(int location) {
        return parr.getByOrder(location);

    }

    public String[] getUserName() {
        String[] temp = allUsernames.toArray(new String[0]);
        return temp;
    }

    static void bannedWaitList(final String username) {
        Thread t = new Thread("bannedWaitList") {
            @Override
            public void run() {
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
                bannedUsers.remove(username);
            }
        };
        t.start();
    }

    public boolean checkAllUsernames() {
        return allUsernames.isEmpty();
    }


    public void updateAllUsers(String[] newUsers) {
        allUsernames.addAll(Arrays.asList(newUsers));
    }

}
