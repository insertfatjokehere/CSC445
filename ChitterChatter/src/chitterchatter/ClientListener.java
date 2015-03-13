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
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientListener {

    private Thread t;
    private Thread t2;
    public static String username;
    InetAddress address;
    InetAddress multicastAddress;

    public int port = 2717; //send and listen
    public int multicastPort = 2719; //listen to multicasts

    static byte[] buf, check;
    static final int maxBufSize = 10482;
    static final int messageBufSize = 526;
    long startTime = 0;
    long endTime = 0;

    private boolean connected = true;

    final GUI gui;

    DatagramSocket socket;
    MulticastSocket multiSocket;

    public ClientListener(final GUI gui) throws UnknownHostException, IOException {
        this.gui = gui;
        username = gui.getUserName();
//----------------------------------------------------------------------------------------------
        //       Randomly select a server to connect to
//----------------------------------------------------------------------------------------------        
        Random rand = new Random();

        int n = rand.nextInt(3) + 1;

        if (n == 1) {
            address = InetAddress.getByName("moxie.cs.oswego.edu"); // moxie's address
            multicastAddress = InetAddress.getByName("224.5.6.7");
        } else if (n == 2) {
            address = InetAddress.getByName("wolf.cs.oswego.edu"); // wolf's address
            multicastAddress = InetAddress.getByName("224.6.7.8");
        } else {
            address = InetAddress.getByName("pi.cs.oswego.edu"); // pi's address
            multicastAddress = InetAddress.getByName("224.7.8.9");
        }

//----------------------------------------------------------------------------------------------       
        //       manually select a server to connect to
//----------------------------------------------------------------------------------------------   
//        address = InetAddress.getByName("moxie.cs.oswego.edu"); // moxie's address
//        multicastAddress = InetAddress.getByName("224.5.6.7");
//        address = InetAddress.getByName("wolf.cs.oswego.edu"); // wolf's address
//        multicastAddress = InetAddress.getByName("224.6.7.8");
//        address = InetAddress.getByName("pi.cs.oswego.edu"); // pi's address
//        multicastAddress = InetAddress.getByName("224.7.8.9");
//----------------------------------------------------------------------------------------------   
        // start socket
        socket = new DatagramSocket(port);
        multiSocket = new MulticastSocket(multicastPort);

        ccpacket join = new ccpacket().joinPacket(username, address, port);
        socket.send(join.datapacket);

        buf = new byte[maxBufSize];
        DatagramPacket tempJoin = new DatagramPacket(buf, buf.length);

        // if you dont receive a packet, the socket will timeout
        socket.setSoTimeout(20000);
        try {
            socket.receive(tempJoin);
        } catch (SocketTimeoutException s) {
            String err = "***Could not join chat, connection is broken***";
            byte[] tempMess = new byte[err.getBytes().length + 2];
            tempMess[0] = (byte) ((short) 15 & 0xff);
            tempMess[1] = (byte) (((short) 15 >> 8) & 0xff);
            System.arraycopy(err.getBytes(), 0, tempMess, 2, err.getBytes().length);
            addMessage(tempMess, tempMess.length);
            socket.close();
        }
        check = tempJoin.getData();
        short codeNum = getCode(check);

        // unsuccessful join
        if (codeNum == (short) 15) {
            addMessage(check, tempJoin.getLength());
            socket.close();
        } else {
            // successful join
            if (socket.isClosed() == false) {
                socket.setSoTimeout(0);
            }
//------------------------------------------------------------------------------            
            // UNICAST (SERVER-CLIENT) CONNECTION
//------------------------------------------------------------------------------
            t = new Thread("UnicastServerListeningThread") {
                @Override
                public void run() {
                    System.out.println("Listening to server...");
                    for (;;) {

//------------------------------------------------------------------------------            
                        // read in a new packet
//------------------------------------------------------------------------------
                        try {
                            buf = new byte[maxBufSize];
                            DatagramPacket temp = new DatagramPacket(buf, buf.length);
                            socket.receive(temp);
                            check = temp.getData();
                            short codeNum = getCode(check);

//------------------------------------------------------------------------------
                            // Process the received packet based on the code  
//------------------------------------------------------------------------------
                            if (codeNum == (short) 01) {
                                // generic message    
                                // print the new text on the client's screen

                                addUserMessage(check, temp.getLength());

                            } else if (codeNum == (short) 2) {
                                // leave case  - print message saying username left
                                addMessage(check, temp.getLength());

                            } else if (codeNum == (short) 3) {
                                // join case || does not apply to ClientListener
                                addMessage(check, temp.getLength());

                            } else if (codeNum == (short) 4) {
                                // received chat session history from server
                                printAllMessages(check, temp.getLength());

                            } else if (codeNum == (short) 5) {
                                // start the vote to kick a user
                                addUserMessage(check, temp.getLength());
                            } else if (codeNum == (short) 8) {
                                // vote result
                                addUserMessage(check, temp.getLength());

                            } else if (codeNum == (short) 14) {
                                // receive latest packet
                                addUserMessage(check, temp.getLength());

                            } else if (codeNum == (short) 15) {
                                // join session DENIED
                                addMessage(check, temp.getLength());

                            } else if (codeNum == (short) 16) {
                                // judging a voting session
                                addMessage(check, temp.getLength());

                            } else if (codeNum == (short) 20) {
                                // receiving a list of all users to start a voting session
                                printUsers(check, temp.getLength());
                            } else if (codeNum == (short) 23) {
                                // send list of usernames to the GUI
                                sendUsernameStrings(check, temp.getLength());
                            } else if (codeNum == (short) 27) {
                                // heartbeat ack
                                setStatus(true);
                            }

//------------------------------------------------------------------------------                        
                        } catch (IOException ex) {
                            Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
                        } // end of try-catch
                    } // end of for(;;)

                }
            };

//------------------------------------------------------------------------------            
            // MULTICAST (CLIENT-SERVER-CLIENTSS) CONNECTION
//------------------------------------------------------------------------------        
            t2 = new Thread("MulticastServerListeningThread") {
                @Override
                public void run() {
                    for (;;) {

//------------------------------------------------------------------------------            
                        // read in a new packet
//------------------------------------------------------------------------------
                        try {
                            buf = new byte[messageBufSize];
                            DatagramPacket temp = new DatagramPacket(buf, buf.length);
                            multiSocket.receive(temp);
                            check = temp.getData();
                            short codeNum = getCode(check);

//------------------------------------------------------------------------------
                            // Process the received packet based on the code  
//------------------------------------------------------------------------------
                            if (codeNum == (short) 01) {
                                // generic message    

                                addUserMessage(check, temp.getLength());

                            } else if (codeNum == (short) 2) {
                                // leave case  - print message saying username left

                                addMessage(check, temp.getLength());

                            } else if (codeNum == (short) 3) {
                                // username joined session 
                                addMessage(check, temp.getLength());

                            } else if (codeNum == (short) 5) {
                                // start the vote to kick a user

                                addUserMessage(check, temp.getLength());
                                byte[] tempVic = new byte[12];
                                System.arraycopy(check, 2, tempVic, 0, tempVic.length);
                                String vic = new String(tempVic);
                                voteForVictim(vic);
                            } else if (codeNum == (short) 8) {
                                // vote result
                                gui.voting = false;
                                byte[] tempUser = new byte[12];
                                byte[] resultCode = new byte[2];
                                System.arraycopy(check, 2, resultCode, 0, 2);
                                short result = getCode(resultCode);
                                System.arraycopy(check, 4, tempUser, 0, 12);

                                if (result == (short) 0) {
                                    String victim = new String(tempUser);
                                    String mess = "***Vote for " + victim.trim() + " was successful***";
                                    byte[] tempMess = new byte[mess.getBytes().length + 2];
                                    System.arraycopy(mess.getBytes(), 0, tempMess, 2, mess.getBytes().length);
                                    addMessage(tempMess, tempMess.length);
                                    kickYa(victim);
                                } else {
                                    String victim = new String(tempUser);
                                    String mess = "***Vote for " + victim.trim() + " was unsuccessful***";
                                    byte[] tempMess = new byte[mess.getBytes().length + 2];
                                    System.arraycopy(mess.getBytes(), 0, tempMess, 2, mess.getBytes().length);
                                    addMessage(tempMess, tempMess.length);
                                }

                            } else if (codeNum == (short) 14) {
                                // receive latest packet

                                addUserMessage(check, temp.getLength());

                            } else if (codeNum == (short) 15) {
                                // join session DENIED
                                addMessage(check, temp.getLength());

                            } else if (codeNum == (short) 16) {
                                // judging a voting session
                                addMessage(check, temp.getLength());
                            }
//------------------------------------------------------------------------------                        
                        } catch (IOException ex) {
                            Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
                        } // end of try-catch
                    } // end of for(;;)
                }
            };

            // Sending ping request
            ccpacket ack = new ccpacket().ping(address, port);
            byte[] temp = new byte[256];
            DatagramPacket tempAck = new DatagramPacket(temp, temp.length);
            byte[] nullCheck = null;
            try {
                socket.send(ack.datapacket);
                socket.receive(tempAck);
                printMessage("***** Welcome to ChitterChatter! *****");

                if (!(tempAck.equals(nullCheck))) {
                    printMessage("You joined " + address.getHostName() + " group");

                } else {
                    printMessage("NO ACK WAS RECEIVED IN ALLOTED TIME");
                    return;
                }
            } catch (IOException ex) {
                printMessage("UNABLE TO CONNECT TO SERVER!");
            }

            if (socket.isClosed() == false) {
                multiSocket.joinGroup(multicastAddress);

                t.start();
                t2.start();
                // send acks to determine the status of your connection
                heartBleed();
            }
        }
    }

    public void MessageFromGUI(String messageFromGUI) {
        sendMessage(username, messageFromGUI);
    }

    public void sendMessage(final String username1, final String message1) {

        Thread th = new Thread("sendMessageThread") {
            @Override
            public void run() {
                // address and port will change after connection is established
                ccpacket messpack = new ccpacket().sendPacketThruDatagram(username1, message1, address, port);
                try {
                    socket.send(messpack.datapacket);
                } catch (IOException ex) {
                    if (socket.isClosed() == false) {
                        Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
                    } else {
                        gui.displayMsg("Server", "You are not connected to the server");
                    }
                }
            }
        };
        th.start();
    }

    private void printMessage(String text) {
        // This method receives a String message, no username
        String tempUsername = "Server";
        String message = text;

        // pass the two strings, username and message
        gui.displayMsg(tempUsername, message);
    }

    public void addUserMessage(byte[] b, int packetLength) throws IOException {
        byte[] tempUsername = new byte[12];
        byte[] tempMessage = new byte[packetLength - tempUsername.length - 2];
        System.arraycopy(b, 2, tempUsername, 0, tempUsername.length);
        System.arraycopy(b, 14, tempMessage, 0, tempMessage.length);
//        this.printMessage(tempUsername, tempMessage);
        String user = new String(tempUsername);
        user = user.trim();
        String mess = new String(tempMessage);
        gui.displayMsg(user, mess);

    }

    public void addMessage(byte[] b, int packetsize) throws IOException {
        byte[] tempMessage = new byte[packetsize - 2];

        System.arraycopy(b, 2, tempMessage, 0, tempMessage.length);
        String message = new String(tempMessage);
        this.printMessage(message);
    }

    private void printUsers(byte[] b, int packetsize) throws IOException {
//        byte [] tempListOfUsers = new byte[packetsize - 2];
        int numberOfUsers = (packetsize - 2) / 12;
        ArrayList<String> usernamesList = new ArrayList<String>();

        for (int i = 0; i < numberOfUsers; i++) {
            byte[] tempUser = new byte[12];
            System.arraycopy(b, 2 + (i * 12), tempUser, 0, 12);
            String temp = new String(tempUser);
            usernamesList.add(temp);
        }

        gui.printDemUsers("Server", usernamesList);

    }

    private void printAllMessages(byte[] b, int packetsize) throws IOException {
        // byte [] tempListOfUsers = new byte[packetsize - 2];
        int numberOfMessages = (packetsize - 2) / 524;

        gui.displayMsg("Server", "Printing the previous messages: ");
        ArrayList<String> msgs = new ArrayList<String>();

        for (int i = 0; i < numberOfMessages; i++) {
            byte[] tempUser = new byte[12];
            byte[] tempMessage = new byte[512];
            System.arraycopy(b, 2 + ((i * 12) + (i * 512)), tempUser, 0,
                    tempUser.length);
            String temp = new String(tempUser);
            System.arraycopy(b, 14 + ((i * 12) + (i * 512)), tempMessage, 0,
                    tempMessage.length);
            String mess = new String(tempMessage);
            mess = mess.trim();
            mess = "   (" + (i + 1) + "/"
                    + numberOfMessages + ") - " + mess;
            msgs.add(mess);
        }
        gui.printSomeMsgs("Server", msgs);

    }

    static void getMessage(byte[] b) {
        byte byte0 = b[0];
        byte byte1 = b[1];
        short codeNum = (short) ((byte1 << 8) + byte0);
    }

    static short getCode(byte[] b) {
        byte byte0 = b[0];
        byte byte1 = b[1];
        short codeNum = (short) ((byte1 << 8) + byte0);
        return codeNum;
    }

    void sendUsernameStrings(byte[] b, int packetSize) {
        int numberOfUsernames = (packetSize - 2) / 12;
        String[] results = new String[numberOfUsernames];
        for (int i = 0; i < numberOfUsernames; i++) {
            byte[] temp = new byte[12];
            System.arraycopy(b, 2 + (i * 12), temp, 0, temp.length);
            results[i] = new String(temp);
        }

        gui.kick(results);
    }

    void kickYa(String victim) {
        gui.voting = false;
        gui.kickUser(victim);
    }

// receives a list of the last 20 messages in the chat history    
    public void getChat() {

        Thread th = new Thread("Get Chat thread") {
            @Override
            public void run() {
                // address and port will change after connection is established
                ccpacket printPack = new ccpacket().printRequest(address, port);
                try {
                    socket.send(printPack.datapacket);
                } catch (IOException ex) {
                    Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        th.start();
    }

    // this method sends a getUSers packet request to server
    public void reqUsernames() {

        Thread th = new Thread("reqUsernames") {
            @Override
            public void run() {
                // address and port will change after connection is established
                ccpacket printPack = new ccpacket().getUsers(address, port);
                try {
                    socket.send(printPack.datapacket);
                } catch (IOException ex) {
                    Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        th.start();
    }

    public void sendLeavePacket() {
        Thread th = new Thread("sendleavePacket") {
            @Override
            public void run() {
                // address and port will change after connection is established
                ccpacket leavePack = new ccpacket().leavePacket(username, address, port);
                try {
                    if (socket.isClosed() == false) {
                        socket.send(leavePack.datapacket);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        th.start();

    }

    public void getUsernamesForGui() {
        Thread th = new Thread("getUsernamesForGui") {
            @Override
            public void run() {
                // address and port will change after connection is established
                ccpacket leavePack = new ccpacket().getUsersForGui(address, port);
                try {
                    socket.send(leavePack.datapacket);
                } catch (IOException ex) {
                    Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        th.start();

    }

    public void startKickUserVote(final String kickUser) {
        Thread th = new Thread("getUsernamesForGui") {
            @Override
            public void run() {
                // address and port will change after connection is established
                ccpacket leavePack = new ccpacket().startKick(kickUser, address, port);
                try {
                    socket.send(leavePack.datapacket);
                } catch (IOException ex) {
                    Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        th.start();
    }

    void voteForVictim(String victim) {
        gui.kickRequest(victim);
    }

    public void sendVoteFromClient(final String username, final boolean b) {
        Thread th = new Thread("sendVoteFromClient") {
            @Override
            public void run() {

                ccpacket leavePack = new ccpacket().kickPacket(username, b, address, port);
                try {
                    socket.send(leavePack.datapacket);
                } catch (IOException ex) {
                    Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        th.start();
    }

    boolean getStatus() {
        return this.connected;
    }

    void setStatus(boolean b) {
        this.connected = b;
    }

    public void heartBleed() {
        Thread th;
        th = new Thread("heartBleed") {
            @Override
            public void run() {
                try {
                    for (;;) {
                        Thread.sleep(20000);
                        if (socket.isClosed() == false) {
                            ccpacket hb = new ccpacket().heatbeatRequest(address, port);
                            if (getStatus() == true) {
                                setStatus(false);
                                socket.send(hb.datapacket);
                            } else {
                                gui.displayMsg("Server", "Switching Over to a new Server...");
                                setStatus(false);
                                changeToNewServer();
                            }
                        }
                    }
                } catch (IOException | InterruptedException ex) {
                    Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        th.start();
    }

    public void changeToNewServer() {
        Thread th;
        th = new Thread("changeToNewServer") {
            @Override
            public void run() {
                InetAddress server1 = null;
                InetAddress server2 = null;
                try {
                    multiSocket.leaveGroup(multicastAddress);
                } catch (IOException ex) {
                    //Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
                }

                String mg1 = null;
                String mg2 = null;
                ccpacket hb1, hb2;
                if (address.getCanonicalHostName().equalsIgnoreCase("moxie.cs.oswego.edu")) {
                    try {
                        server1 = InetAddress.getByName("pi.cs.oswego.edu");
                        server2 = InetAddress.getByName("wolf.cs.oswego.edu");
                        mg1 = "224.7.8.9";
                        mg2 = "224.6.7.8";
                    } catch (UnknownHostException ex) {
                        Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else if (address.getCanonicalHostName().equalsIgnoreCase("pi.cs.oswego.edu")) {
                    try {
                        server1 = InetAddress.getByName("wolf.cs.oswego.edu");
                        server2 = InetAddress.getByName("moxie.cs.oswego.edu");
                        mg1 = "224.6.7.8";
                        mg2 = "224.5.6.7";
                    } catch (UnknownHostException ex) {
                        Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    try {
                        server1 = InetAddress.getByName("moxie.cs.oswego.edu");
                        server2 = InetAddress.getByName("pi.cs.oswego.edu");
                        mg1 = "224.5.6.7";
                        mg2 = "224.7.8.9";
                    } catch (UnknownHostException ex) {
                        Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }

                if (server1 != null && server2 != null) {
                    hb1 = new ccpacket().heatbeatRequest(server1, Server.listenPort);
                    hb2 = new ccpacket().heatbeatRequest(server2, Server.listenPort);
                    try {
                        socket.send(hb1.datapacket);
                        Thread.sleep(10000);
                        if (getStatus() == false) {
                            setStatus(false);
                            socket.send(hb2.datapacket);
                            Thread.sleep(10000);
                            if (getStatus() == false) {
                                gui.displayMsg("System", "Couldn't connect to any other server, client window is now going to close");
                                gui.displayMsg("System", "Good Bye!");
                                try {
                                    Thread.sleep(20000);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                System.exit(0);
                            } else {
                                address = server2;
                                multicastAddress = InetAddress.getByName(mg2);
                                multiSocket.joinGroup(multicastAddress);
                                gui.displayMsg("System", "Switched to Server " + address.getCanonicalHostName());
                                setStatus(true);
                            }
                        } else {
                            address = server1;
                            multicastAddress = InetAddress.getByName(mg1);
                            multiSocket.joinGroup(multicastAddress);
                            gui.displayMsg("System", "Switched to Server " + address.getCanonicalHostName());
                            setStatus(true);
                        }
                    } catch (IOException | InterruptedException ex) {
                        Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    gui.displayMsg("System", "Couldnt connect to any other server, client window is now going to close...");
                    gui.displayMsg("System", "Good Bye!");
                    try {
                        Thread.sleep(20000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    System.exit(0);
                }
            }
        };
        th.start();
    }

} // end of ClientListener class
