/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Kyle
 */
import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Scanner;

public class Server {

    static final int windowSize = 5;

    static String usage = "Parameters: [IP version 4|6][Sliding window Y|N][Drop Y|N]";
    static int version = 0;
    static int port = 2717;
    static int iterations;
    static int dataBufSize = 512;
    static int clientPort;
    static Random randomGen = new Random();
    static int randomInt;
    static int randomBlockLoop = -1;
    static long fileSize;
    static boolean slidingWindowParameter = false;
    static boolean dropPacket = false;
    static boolean done = false;
    static InetAddress[] inet;
    static String hostName = "moxie.oswego.edu";
    static String hostv6 = "0:0:0:0:0:ffff:8103:1403";
    static String requestURL;
    static InetAddress v6;
    static InetAddress v4;
    static InetAddress clientAdd;
    static String para[] = new String[3];
    static Scanner in = new Scanner(System.in);
    static RandomAccessFile raf;
    static byte[] sendPacket;

    //v6 = getInet6AddressByName(hostName);
    public static void main(String[] args) throws UnknownHostException, SocketException, FileNotFoundException, IOException {

        v6 = InetAddress.getByName(hostv6);
        v4 = InetAddress.getByName(hostName);

        System.out.println("-----------------------------------------------");
        System.out.println("IPv4 address of server " + v4.getHostAddress());
        System.out.println("IPv6 address of server " + hostv6);
        System.out.println("-----------------------------------------------");
        System.out.println(usage);
        System.out.print("[IP Version     (4|6)] ");
        para[0] = in.next();
        System.out.print("[Sliding Window (y|n)] ");
        para[1] = in.next();
        System.out.println("---Note: Dropping packets is only done if Sliding window is False---");
        System.out.print("[Drop Packets   (y|n)] ");
        para[2] = in.next();

        if (para.length != 3) {
            System.out.println(usage);
            return;
        } else {
            version = Integer.parseInt(para[0]);
            if (para[1].equalsIgnoreCase("y")) {
                slidingWindowParameter = true;
            } else if (para[1].equalsIgnoreCase("n")) {
                slidingWindowParameter = false;
            } else {
                System.out.println(usage);
                return;

            }

            if (para[2].equalsIgnoreCase("y")) {
                dropPacket = true;
            } else if (para[2].equalsIgnoreCase("n")) {
                dropPacket = false;
            } else {
                System.out.println(usage);
                return;

            }
        }

        // v6 = getInet6AddressByName(hostName);
        v4 = InetAddress.getByName(hostName);

        DatagramSocket socket = new DatagramSocket(port);
        // socket.setSoTimeout(6000);
        // temp buffer to read off of
        byte[] buf = new byte[512];
        DatagramPacket requestFromClient = new DatagramPacket(buf, buf.length);

        // recieve the request
        socket.receive(requestFromClient);

        clientAdd = requestFromClient.getAddress();
        clientPort = requestFromClient.getPort();

        System.out.println("Client Address: " + clientAdd);
        System.out.println("Client Port Number " + clientPort);

        // processes the packet
        byte[] clientRequest = requestFromClient.getData();
        byte[] getUrl = new byte[clientRequest.length - 2];
        // this array copy is used to get the url reques from the client
        System.arraycopy(clientRequest, 2, getUrl, 0, getUrl.length);

        // convert bytes to string
        requestURL = new String(getUrl);
        // debug
        System.out.println("DEBUG url request: " + requestURL);
        // cache the url for later use
        downloadFile(requestURL);
        // file name on server is arbitraty
        File download = new File("downloaded.html");
        fileSize = download.length();

        // debug
        System.out.println("DEBUG File name: " + download.getName());
        System.out.println("DEBUG File size: " + fileSize + " bytes");

        // the random access file allows easy parsing of bytes
        raf = new RandomAccessFile("downloaded.html", "rw");

        // counts how many packets need to make based on the file size
        iterations = (int) Math.ceil(fileSize / dataBufSize);
        iterations = iterations * 2;
        System.out.println("DEBUG iterations: " + iterations);

        // if statement used to randomly choose a packet between 
        // range of 1 and iterations -1 (zero is not included to 
        // cause less timeouts
        if (dropPacket == true) {
            randomInt = randomGen.nextInt(iterations - 2);
            randomInt++;
            System.out.println("DEBUG: Random packet drop " + randomInt);
            //    randomBlockLoop = randomGen.nextInt()
        } else {
            randomInt = -1;
        }

        if (slidingWindowParameter == false) {
            // sequentional sending
            boolean sentTerm = false;
            short countInter = 0;

            int totalPackets = 0;

            transfer:
            while (done == false) {

                if (totalPackets > iterations) {
                    // you are at the last packet
                    sendPacket = new byte[0];
                    TFTPPacket temp = new TFTPPacket().terminationPacket(countInter, clientAdd, clientPort, sendPacket);
                    socket.send(temp.dataPacket);

                    break;
                } else {
                 //   System.out.println("DEBUG -- top of the loop --- ");
                    if (totalPackets + 1 == iterations) {
                        System.out.println("DEBUG: --------------- Last packet ------------------ ");
                        sendPacket = new byte[(int) fileSize - (totalPackets * dataBufSize)];
                        // sendPacket = new byte[dataBufSize];
                        // fileSize - (countInter * dataBufSize) guarentees that the last 
                        // packet is no bigger than it needs to be

                        // reads the last of the file              
                        // raf.readFully(sendPacket, ((int)(countInter * dataBufSize)), sendPacket.length);
                        raf.read(sendPacket);
                        TFTPPacket temp = new TFTPPacket().terminationPacket(countInter, clientAdd, clientPort, sendPacket);
                        socket.send(temp.dataPacket);
                        sentTerm = true;

                    } else if (totalPackets == randomInt) {
                        System.out.println("DEBUG: --- Dropping packet --- ");
                        // purposely make it half the size  
                        sendPacket = new byte[dataBufSize / 2];
                        // raf.readFully(sendPacket, ((int)(countInter * dataBufSize)), sendPacket.length);
//                      raf.read(sendPacket);
                     TFTPPacket temp = new TFTPPacket().errorPacket((short) 0, clientAdd, clientPort);
//                        TFTPPacket temp = new TFTPPacket().dataPacket(countInter, clientAdd, clientPort, sendPacket);
                    //    System.out.println("DEBUG: packet size: " + temp.dataPacket.getLength());
                        socket.send(temp.dataPacket);
                        randomInt = -1;
                        totalPackets++;
                    } else {
                    //    System.out.println("DEBUG: --- Normal Packet --- ");
                        System.out.println("DEBUG: --- Interations --- " + totalPackets);
                        sendPacket = new byte[dataBufSize];
                        //raf.readFully(sendPacket, ((int)(countInter * dataBufSize)), sendPacket.length - ((int)(countInter * dataBufSize)));
                        raf.read(sendPacket);
                        TFTPPacket temp = new TFTPPacket().dataPacket(countInter, clientAdd, clientPort, sendPacket);
                        System.out.println("DEBUG: packet size: " + temp.dataPacket.getLength());
                        socket.send(temp.dataPacket);
                        totalPackets++;
//                      countInter++;
//                      System.out.println("DEBUG: --- Interations check 2 --- " + countInter);
                    }

                 //   System.out.println("DEBUG: --- waiting for ACK --- ");
                    byte[] ack = new byte[512];
                    byte[] ackCheck;
                    DatagramPacket lastAck = new DatagramPacket(ack, ack.length);
                    socket.receive(lastAck);
                    ackCheck = lastAck.getData();
                    byte byte0 = ackCheck[0];
                    byte byte1 = ackCheck[1];
                    byte byte2 = ackCheck[2];
                    byte byte3 = ackCheck[3];
                    short codeNum = (short) ((byte1 << 8) + byte0);
                    short blockNum = (short) ((byte3 << 8) + byte2);
                //    System.out.println("DEBUG: Code Number " + codeNum);
                //     System.out.println("DEBUG: Block Number " + blockNum);
                    if (codeNum == (short) 4 && blockNum == countInter) {
                  //      System.out.println("DEBUG: --- Recieved ACK ---");
                        // checks wether the last packet recieved was an ack rather 
                        // then an error code, other words resend if no true  
                        if (sentTerm == true) {
                            done = true;
                        }
                        if (countInter != 127) {
                            countInter++;
                        } else {
                            countInter = 0;
                        }
                         totalPackets++;
                    } else {
                        System.out.println("Resenting all the packets");
			raf = new RandomAccessFile("downloaded.html", "rw");
                        // resend all the packets since there was an error
                        countInter = 0;
                        totalPackets = 0;
                    }
                }
            }

        } else {
            int slideIterations = iterations;
            boolean sentTerm = false;
            short countInter = 0;
            byte[] ack = new byte[512];
            byte[] ackCheck;
            long checkBlocks;
            int loopBackCount = 0;
            slidingWindow:
            while (done == false) {
                // Sliding window algorithm
                // the iteration count decrements since
                // special measures need to be done for the 
                // last sliding window

                if (slideIterations < windowSize) {
                    // send the last array of packets
                    for (int i = slideIterations; i > 0; i--) {
                        sendPacket = new byte[dataBufSize];
                        raf.read(sendPacket);
                        if (i == 1) {
                            System.out.println("DEBUG: ------------ Last Packet ------------");
                            TFTPPacket term = new TFTPPacket().terminationPacket(countInter, clientAdd, clientPort, sendPacket);
                    //        System.out.println("DEBUG: packet size: " + term.dataPacket.getLength());
                            socket.send(term.dataPacket);
                            DatagramPacket lastAck = new DatagramPacket(ack, ack.length);
                            socket.setSoTimeout(15000);
                            socket.receive(lastAck);
                            break slidingWindow;
                        } else {

                            TFTPPacket temp = new TFTPPacket().dataPacket(countInter, clientAdd, clientPort, sendPacket);
                            System.out.println("DEBUG: sending packet " + i + " of the last window");
                //            System.out.println("DEBUG: packet size: " + temp.dataPacket.getLength());
                            socket.send(temp.dataPacket);
                        }
                        slideIterations--;
                    }

                } else {
                    // normal transfer of packekts
                    for (int i = 0; i < windowSize; i++) {
                        sendPacket = new byte[dataBufSize];
                        raf.read(sendPacket);
                        TFTPPacket temp = new TFTPPacket().dataPacket(countInter, clientAdd, clientPort, sendPacket);
                //        System.out.println("DEBUG: packet size: " + temp.dataPacket.getLength());
                        socket.send(temp.dataPacket);
                        slideIterations--;
                        // used to keep errors from happening
                        // when storing blockNums in packets
                        if (countInter != 127) {
                            countInter++;
                        } else {
                            countInter = 0;
                            loopBackCount++;
                        }
                    }
                }

                System.out.println("DEBUG: --- waiting for ACK --- ");
                DatagramPacket lastAck = new DatagramPacket(ack, ack.length);
                socket.setSoTimeout(15000);
                socket.receive(lastAck);
                ackCheck = lastAck.getData();
                byte byte0 = ackCheck[0];
                byte byte1 = ackCheck[1];
                byte byte2 = ackCheck[2];
                byte byte3 = ackCheck[3];
                short codeNum = (short) ((byte1 << 8) + byte0);
                short blockNum = (short) ((byte3 << 8) + byte2);
                checkBlocks = ((long) blockNum + (loopBackCount * 128));
//                System.out.println("DEBUG: Code Number " + codeNum);
//                System.out.println("DEBUG: Block Number " + blockNum);
//                System.out.println("DEBUG: Checks Blocks " + checkBlocks);
                if (codeNum == (short) 4 && blockNum == countInter) {
              //      System.out.println("DEBUG: --- Recieved ACK ---");
                    // checks wether the last packet recieved was an ack rather 
                    // then an error code, other words resend if no true  

                } else if (codeNum == (short) 6) {

              //      System.out.println("DEBUG: Recieved Ack on transfer");
                    done = true;

                } else {
                    System.out.println("Resenting all the packets");
                    // resend all the packets since there was an error
                    countInter = 0;
                    slideIterations = iterations;
                    loopBackCount = 0;
                }

            }
        }

    }

    static Inet6Address getInet6AddressByName(String host) throws UnknownHostException, SecurityException {
        for (InetAddress addr : InetAddress.getAllByName(host)) {
            if (addr instanceof Inet6Address) {
                return (Inet6Address) addr;
            }
        }
        throw new UnknownHostException("No IPv6 address found for " + host);
    }

    // downloadFile() takes a url and downloads the page it wants and saves it as
    // downloaded.html, the url is defaulted to a random site on my moxie account
    static void downloadFile(String request) {
        URL urlLink;
        URLConnection con;
        DataInputStream dis;
        FileOutputStream fos;
        byte[] data;

        try {
            urlLink = new URL(request);
            con = urlLink.openConnection();
            dis = new DataInputStream(con.getInputStream());
            data = new byte[con.getContentLength()];
            for (int i = 0; i < data.length; i++) {
                data[i] = dis.readByte();
            }
            dis.close();
            fos = new FileOutputStream(new File("downloaded.html"));
            fos.write(data);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

    }

}
