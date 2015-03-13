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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Server {

    static final int listenPort = 2717;
    static final int multiPort = 2719;
    static InetAddress group, server1, server2;
    static final int maxBufSize = 10484;
    static final int usernameLength = 12;
    static final int messageLength = 512;
    static DatagramSocket sock, ssock1, ssock2;
    static MulticastSocket multi;
    static byte[] buf, check;
    static ServerHandler sh;
    static String servName, victim;
    static short yesVotesFromOthers, noVotesFromOthers;
    static Runtime r;
    static String[][] otherChat = new String[0][0];

    public static void main(String[] args) throws SocketException, IOException, ClassNotFoundException, InterruptedException {
        String usage = "usage: type server name as an argument (moxie | wolf | pi)";
        sock = new DatagramSocket(listenPort);
        multi = new MulticastSocket(multiPort);

        if (args.length != 1 || (!args[0].equalsIgnoreCase("moxie")
                && !args[0].equalsIgnoreCase("wolf") && !args[0].equalsIgnoreCase("pi"))) {
            System.out.println(usage);
            return;
        } else {
            if (args[0].equalsIgnoreCase("moxie")) {
                group = InetAddress.getByName("224.5.6.7");
                server1 = InetAddress.getByName("wolf.cs.oswego.edu");
                server2 = InetAddress.getByName("pi.cs.oswego.edu");
            } else if (args[0].equalsIgnoreCase("wolf")) {
                group = InetAddress.getByName("224.6.7.8");
                server1 = InetAddress.getByName("moxie.cs.oswego.edu");
                server2 = InetAddress.getByName("pi.cs.oswego.edu");
            } else {
                group = InetAddress.getByName("224.7.8.9");
                server1 = InetAddress.getByName("wolf.cs.oswego.edu");
                server2 = InetAddress.getByName("moxie.cs.oswego.edu");
            }

            servName = args[0];
            multi.joinGroup(group);
            System.out.println("Clients will connect to Server " + args[0]
                    + " at address: " + group.toString());

            sh = new ServerHandler();

            ssock1 = new DatagramSocket();
            ssock2 = new DatagramSocket();
            // 12 for username, 4 for space, 1024 for message size

        }

//-----------------------------------------------------------------------------
//           Grabbing all the usernames from another server if possible
//-----------------------------------------------------------------------------
        updateUsersFromServer();
        updateChat();
//-----------------------------------------------------------------------------
//           Add hook incase all someone closes the server
//-----------------------------------------------------------------------------
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {

                try {
                    ServerHandler.parr.saveInfo();
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        // listen to the datagram socket
        for (;;) {
            // read in a new packet     
            buf = new byte[maxBufSize];
            DatagramPacket temp = new DatagramPacket(buf, buf.length);
            sock.receive(temp);
            check = temp.getData();
            short codeNum = getCode(check);
//------------------------------------------------------------------------------
//                     Process the recieved packet based on the code  
//------------------------------------------------------------------------------
            if (codeNum == (short) 1) {
                // generic message    
                // method of storing the new text into the data structure

                sh.addMessage(check);

                sendMessage(check, temp.getLength(), temp.getAddress());
            } else if (codeNum == (short) 2) {
                // leave case    
                // delete the user from the list

                sh.leavingChat(check, (short) 1);

            } else if (codeNum == (short) 3) {
            // join case    
                // check the banned lists/name already exists
                // else send a message to the individual instead of group   

                // this method is called if and only if the user is not banned
                // nor is the username has already been taken
                // if this message came from the server 
                if (temp.getAddress() != server1 && temp.getAddress() != server2) {
                    boolean bool = sh.availableUsername(check);
                    if (bool == true) {


                        sh.joinChat(check, (short) 0);
                        ccpacket joinTemp = new ccpacket().joinSuccPack(temp.getAddress(), temp.getPort());
                        sock.send(joinTemp.datapacket);

                    } else {
 
                        unsuccessfulJoinMessage(temp.getAddress(), temp.getPort());
                    }
                }
            } else if (codeNum == (short) 4) {
                // print request case    

                String[][] printMessages = sh.getAllMessages();
                printAllMessages(printMessages, temp.getAddress(), temp.getPort());
            } else if (codeNum == (short) 5) {
                // start the vote to kick a user

                byte[] tempVictimName = new byte[usernameLength];
                System.arraycopy(check, 2, tempVictimName, 0, usernameLength);
                victim = new String(tempVictimName);
                rockTheVote(victim);
            } else if (codeNum == (short) 6) {
                // accept votes to kick or not to kick a person    

                short vote;
                byte[] v = new byte[2];
                System.arraycopy(check, 2, v, 0, 2);
                vote = getCode(v);
                // after getting a vote, determine if it's to kick or not
                if (vote == (short) 00) {
                    sh.countUpYesVote();
                } else {
                    sh.countUpNoVotes();
                }
            } else if (codeNum == (short) 7) {
                // udate users
                String username;
                short flagCode;
                byte[] flag = new byte[2];
                byte[] tempUser = new byte[usernameLength];
                System.arraycopy(check, 2, flag, 0, flag.length);
                System.arraycopy(check, 4, tempUser, 0, tempUser.length);
                flagCode = getCode(flag);
                username = new String(tempUser);
                // send messages according to the flag
                if (flagCode == (short) 1) {
                    // 1 flag stand for the removal of a username
                    // meaning someone left chat from another server

                    ServerHandler.allUsernames.remove(username);
                    ServerHandler.serverOnlyUsernames.remove(username);
                    ccpacket tempPacket = new ccpacket().leaveChat(username, group, multiPort);
                    multi.send(tempPacket.datapacket);

                } else {


                    ServerHandler.allUsernames.add(username);
                    ServerHandler.serverOnlyUsernames.add(username);
                    ccpacket tempPacket = new ccpacket().successfulJoin(username, group, multiPort);
                    multi.send(tempPacket.datapacket);
                }
            } else if (codeNum == (short) 8) {
                // vote result 
                byte[] tempUserName = new byte[usernameLength];
                byte[] tempCode = new byte[2];
                System.arraycopy(check, 4, tempUserName, 0, tempUserName.length);
                System.arraycopy(check, 2, tempCode, 0, tempCode.length);
                String victimName = new String(tempUserName);
                short code = getCode(tempCode);
                // if the vote was successful, remove the user, and add him to the banned list
                if (code == (short) 0) {

                    sh.kickUserFromChat(victimName);
                }
                multi.send(new DatagramPacket(check, check.length, group, multiPort));
                sh.clearVotes();

            } else if (codeNum == (short) 9) {
                // ping test, send an ack

                sendPingAck(temp.getAddress(), temp.getPort());
            
            } else if (codeNum == (short) 16) {
                // non-Judge server counting all the votes
                waitAndCountVotes(temp.getAddress(), temp.getPort(), check);
            } else if (codeNum == (short) 17) {

                byte[] temp1 = new byte[2];
                byte[] temp2 = new byte[2];
                System.arraycopy(check, 2, temp1, 0, 2);
                System.arraycopy(check, 4, temp2, 0, 2);

                short tempYes, tempNo;
                tempYes = getCode(temp1);
                tempNo = getCode(temp2);
                yesVotesFromOthers = (short) (yesVotesFromOthers + tempYes);
                noVotesFromOthers = (short) (noVotesFromOthers + tempNo);

            } else if (codeNum == (short) 19) {

                sendUsers(temp.getAddress(), temp.getPort());
            } else if (codeNum == (short) 22) {

                sendUsersForGuiKick(temp.getAddress(), temp.getPort());
            } else if (codeNum == (short) 24) {

                sendUsersForServer(temp.getAddress());
            } else if (codeNum == (short) 25) {

                updateAllUsers(check, temp.getLength());
            } else if (codeNum == (short) 26) {

                sendHeart(temp.getAddress(), temp.getPort());
            } else if (codeNum == (short) 28) {

                if (ServerHandler.parr.size() > 0) {
                    sendChatToServer(temp.getAddress());
                }
            } else if (codeNum == (short) 29) {

                otherChatMethod(check, temp.getLength());
            } else if (codeNum == (short) 30) {

                updateChatMethod(check, temp.getLength());
            }      

        }

    }

    static short getCode(byte[] b) {
        byte byte0 = b[0];
        byte byte1 = b[1];
        short codeNum = (short) ((byte1 << 8) + byte0);
        return codeNum;
    }

    //---------------------------------------------------------------------------------------------
    //                         Threads and Methods for handling packets
    //---------------------------------------------------------------------------------------------
    static void sendMessage(final byte[] b, final int packetSize, final InetAddress source) {
        Thread t = new Thread("sendMessage") {
            @Override
            public void run() {
                byte[] temp = new byte[packetSize - usernameLength - 2];
                byte[] tempUser = new byte[usernameLength];

                String message, username;
                System.arraycopy(b, usernameLength + 2, temp, 0, temp.length);
                message = new String(temp);
                System.arraycopy(b, 2, tempUser, 0, usernameLength);
                username = new String(tempUser);
                ccpacket messpack = new ccpacket().sendPacketThruMult(username, message, group, multiPort);
                ccpacket messpack1 = new ccpacket().sendPacketThruDatagram(username, message, server1, listenPort);
                ccpacket messpack2 = new ccpacket().sendPacketThruDatagram(username, message, server2, listenPort);
                try {
                    multi.send(messpack.datapacket);
                    if (!(source.equals(server1) || source.equals(server2))) {


                        ssock1.send(messpack1.datapacket);
                        ssock2.send(messpack2.datapacket);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };

        t.start();
    }

    static void unsuccessfulJoinMessage(final InetAddress add, final int port) {
        Thread t = new Thread("unsuccessfulJoinMessage") {
            @Override
            public void run() {

                ccpacket joined = new ccpacket().unsuccessfulJoin(add, port);

                try {
                    sock.send(joined.datapacket);
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };

        t.start();

    }

    static void sendPingAck(final InetAddress add, final int port) {
        Thread t = new Thread("sendPingAck") {
            @Override
            public void run() {
                ccpacket pingAck = new ccpacket().pingAck(add, port);

                try {
                    sock.send(pingAck.datapacket);
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }

            }

        };

        t.start();
    }

    static void printAllMessages(final String[][] messages, final InetAddress add, final int port) {
        Thread t = new Thread("printAllMessages") {
            @Override
            public void run() {

                ccpacket printAll = new ccpacket().printPacket(messages, add, port);

                try {
                    sock.send(printAll.datapacket);
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };

        t.start();

    }

    static void sendLatest(final String[] message, final InetAddress add, final int port) {
        Thread t = new Thread("sendLatest") {
            @Override
            public void run() {

                ccpacket late = new ccpacket().sendLatest(message, add, port);

                try {
                    sock.send(late.datapacket);
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };

        t.start();

    }

    static void rockTheVote(final String victim) {
        Thread t;
        t = new Thread("rockTheVote") {
            @Override
            public void run() {

                ccpacket judge1 = new ccpacket().voteSessionFromJudge(victim, server1, listenPort);
                ccpacket judge2 = new ccpacket().voteSessionFromJudge(victim, server2, listenPort);
                sh.clearVotes();
                ccpacket sendVote1 = new ccpacket().startKick(victim, group, multiPort);
                try {
                    System.out.println("Voting Session Has Started For " + victim);
                    multi.send(sendVote1.datapacket);
                    ssock1.send(judge1.datapacket);
                    ssock2.send(judge2.datapacket);
                    // dont need to ping or get acks since if the servers are not available
                    // their clients wont be able to vote anyways
                    clearVotesFromOthers();
                    Thread.sleep(30000);

                    noVotesFromOthers = (short) (noVotesFromOthers + sh.getNVotes());
                    yesVotesFromOthers = (short) (yesVotesFromOthers + sh.getYVotes());
                    boolean bool = sh.tallyVotes(yesVotesFromOthers, noVotesFromOthers);
                    if (bool == true) {
                        System.out.println("Voting to Kick " + victim + " was Successful");
                        ccpacket resultFromVote = new ccpacket().kickResults(victim, true, group, multiPort);
                        ccpacket resultFromVote1 = new ccpacket().kickResults(victim, true, server1, listenPort);
                        ccpacket resultFromVote2 = new ccpacket().kickResults(victim, true, server2, listenPort);
                        multi.send(resultFromVote.datapacket);
                        ssock1.send(resultFromVote1.datapacket);
                        ssock2.send(resultFromVote2.datapacket);
                        sh.kickUserFromChat(victim);
                    } else {
                        System.out.println("Voting to Kick " + victim + " was Unsuccessful");
                        ccpacket resultFromVote = new ccpacket().kickResults(victim, false, group, multiPort);
                        ccpacket resultFromVote1 = new ccpacket().kickResults(victim, false, server1, listenPort);
                        ccpacket resultFromVote2 = new ccpacket().kickResults(victim, false, server2, listenPort);
                        multi.send(resultFromVote.datapacket);
                        ssock1.send(resultFromVote1.datapacket);
                        ssock2.send(resultFromVote2.datapacket);
                    }

                    sh.clearVotes();
                } catch (IOException | InterruptedException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }

            }

        };

        t.start();

    }

    static public void waitAndCountVotes(final InetAddress add, final int port, final byte[] b) throws InterruptedException {
        // wait for 20 seconds before sending the results
        Thread t;
        t = new Thread("waitAndCountVotes") {

            @Override
            public void run() {

                String victimName;
                byte[] tempUser = new byte[12];
                System.arraycopy(b, 2, tempUser, 0, tempUser.length);
                victimName = new String(tempUser);
                try {
                    ccpacket sendVote = new ccpacket().startKick(victimName, group, multiPort);
                    multi.send(sendVote.datapacket);
                    Thread.sleep(25000);
                    ccpacket results = new ccpacket().voteResults(sh.getYVotes(), sh.getNVotes(), add, listenPort);
                    sock.send(results.datapacket);
                } catch (IOException | InterruptedException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        t.start();
    }

    static public void clearVotesFromOthers() {
        yesVotesFromOthers = 0;
        noVotesFromOthers = 0;
    }



    static public void sendUsers(final InetAddress add, final int port) {
        Thread t = new Thread("sendUsers") {
            @Override
            public void run() {
                String[] temp = sh.getUserName();
                ccpacket pack = new ccpacket().sendUsers(temp, add, port);

                try {
                    sock.send(pack.datapacket);
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };

        t.start();

    }

    static public void sendUsersForGuiKick(final InetAddress add, final int port) {
        Thread t = new Thread("sendUsers") {
            @Override
            public void run() {
                String[] temp = sh.getUserName();
                ccpacket pack = new ccpacket().sendUsersForGui(temp, add, port);

                try {
                    sock.send(pack.datapacket);
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };

        t.start();
    }

    public static void updateServerUsers(final short flag, final String username) {

        // reference the .txt file for which flag does what  
        Thread t;
        t = new Thread("updateServerUsers") {
            @Override
            public void run() {
                ccpacket update1 = new ccpacket().updateUsers(username, flag, server1, listenPort);
                ccpacket update2 = new ccpacket().updateUsers(username, flag, server2, listenPort);
                ccpacket chatMess;
                if (flag == (short) 0) {
                    chatMess = new ccpacket().successfulJoin(username, group, multiPort);
                } else {
                    chatMess = new ccpacket().leaveChat(username, group, multiPort);
                }
                try {
                    multi.send(chatMess.datapacket);
                    ssock1.send(update1.datapacket);
                    ssock2.send(update2.datapacket);
                } catch (IOException ex) {
                    Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        t.start();
    }

    public static void updateUsersFromServer() {

        Thread t;
        t = new Thread("updateUsersFromServer") {
            @Override
            public void run() {
                ccpacket update1 = new ccpacket().getAllUsersForServers(server1, listenPort);
                ccpacket update2 = new ccpacket().getAllUsersForServers(server2, listenPort);

                try {

                    ssock1.send(update1.datapacket);
                    Thread.sleep(10000);
                    if (sh.checkAllUsernames() == true) {
                        ssock2.send(update2.datapacket);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        t.start();
    }

    static void sendUsersForServer(final InetAddress add) {
        Thread t = new Thread("sendUsersForServer") {
            @Override
            public void run() {

                String[] usernames = sh.getUserName();
                ccpacket update = new ccpacket().sendAllUsersForServers(usernames, add, listenPort);

                try {
                    sock.send(update.datapacket);
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        };

        t.start();

    }

    static void updateAllUsers(byte[] b, int packetSize) {
        int numberOfusers = (packetSize - 2) / 12;
        String[] usernames = new String[numberOfusers];
        for (int i = 0; i < usernames.length; i++) {
            byte[] tempUser = new byte[12];
            System.arraycopy(b, 2 + (i * 12), tempUser, 0, tempUser.length);
            usernames[i] = new String(tempUser);
        }
        sh.updateAllUsers(usernames);
    }

    static void sendHeart(final InetAddress add, final int port) {

        Thread t = new Thread("sendHeart") {
            @Override
            public void run() {

                ccpacket update = new ccpacket().heatbeatSend(add, port);

                try {
                    sock.send(update.datapacket);
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        };

        t.start();

    }

    static void updateChat() {

        Thread t = new Thread("updateChat") {
            @Override
            public void run() {

                ccpacket update1 = new ccpacket().requestChatForServer(server1, listenPort);
                ccpacket update2 = new ccpacket().requestChatForServer(server2, listenPort);

                try {

                    System.out.println("Sending Update Request to Get Chat History - at " + server1.getCanonicalHostName());
                    ssock1.send(update1.datapacket);
                    Thread.sleep(3000);
                   // String[][] check = sh.getAllMessages();
                    if (otherChat.length != 0) {

                        System.out.println("Updating the Chat");
                        for (int i = 0; i < otherChat.length; i++) {
                            byte[] tempByte = new byte[512 + 12];
                            System.arraycopy(otherChat[0][i].getBytes(), 0, tempByte, 0, 12);
                            System.arraycopy(otherChat[1][i].getBytes(), 0, tempByte, 12, 512);
                            sh.addMessageUpdate(tempByte);
                        }

                    } else {

                        System.out.println("Sending Update Request to Get Chat History - at " + server2.getCanonicalHostName());
                        ssock2.send(update2.datapacket);
                        Thread.sleep(3000);
                        //check = sh.getAllMessages();
                        if (otherChat.length != 0) {

                            System.out.println("Updating the Chat");
                            for (int i = 0; i < otherChat.length; i++) {
                                byte[] tempByte = new byte[512 + 12];
                                System.arraycopy(otherChat[0][i].getBytes(), 0, tempByte, 0, 12);
                                System.arraycopy(otherChat[1][i].getBytes(), 0, tempByte, 12, 512);
                                sh.addMessageUpdate(tempByte);
                            }
                        } else {
                            System.out.println("Couldn't Update messages");
                        }
                    }

                    }catch (IOException | InterruptedException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
                }

            }

            ;

        t.start ();

        }

    static void sendChatToServer(final InetAddress add) {

        Thread t = new Thread("sendChatToServer") {
            @Override
            public void run() {
                String[][] temp = sh.getAllMessages();
                ccpacket chat = new ccpacket().sendChatForServer(temp, add, listenPort);

                try {
                    sock.send(chat.datapacket);
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        };

        t.start();

    }

    static void updateChatMethod(byte[] b, int packetSize) throws IOException {


        int tempNumberofMessages = (packetSize - 4) / 524;
        for (int i = 0; i < tempNumberofMessages; i++) {
            byte tempByte[] = new byte[512 + 12];
            System.arraycopy(b, (4 + ((i * 12) + (i * 512))), tempByte, 0, tempByte.length);
            sh.addMessage(tempByte);
        }
    }

    static void otherChatMethod(byte[] b, int packetSize) {


        int tempNumberofMessages = (packetSize - 4) / 524;
        otherChat = new String[2][tempNumberofMessages];

        for (int i = 0; i < tempNumberofMessages; i++) {
            byte tempByte[] = new byte[512 + 12];
            System.arraycopy(b, 4 + ((i * 12) + (i * 512)), tempByte, 0, tempByte.length);
            byte[] tempUsername = new byte[usernameLength];
            byte[] tempMessage = new byte[512];
            System.arraycopy(tempByte, 0, tempUsername, 0, tempUsername.length);
            System.arraycopy(tempByte, tempUsername.length, tempMessage, 0, tempMessage.length);
            String username = new String(tempUsername);
            String message = new String(tempMessage);
            otherChat[0][i] = username;
            otherChat[1][i] = message;
        }
    }

}
