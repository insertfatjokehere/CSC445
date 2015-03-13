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
//        int count = 0;
    static int port = 2717;
    static int iterations;
    static int dataBufSize = 512;
    static int clientPort;
    static Random randomGen = new Random();
    static int randomInt = -1;
    static long fileSize;
    static boolean slidingWindowParameter = false;
    static boolean dropPacket = false;
    static boolean done = false;
    static InetAddress[] inet;
    static String hostName = "moxie.oswego.edu";
    static String hostv6 = "0:0:0:0:0:ffff:8103:1403";
    static String requestURL;
    //String requestURL;
    static InetAddress v6;
    static InetAddress v4;
    static InetAddress clientAdd;
    static String para[] = new String[3];
    static Scanner in = new Scanner(System.in);
    static RandomAccessFile raf;
    static byte[] sendPacket;
//    static byte[] content;

    //v6 = getInet6AddressByName(hostName);
    public static void main(String[] args) throws UnknownHostException, SocketException, FileNotFoundException, IOException {

//        String usage = "Parameters: [IP version 4|6][Sliding window Y|N][Drop Y|N]";
//        int version = 0;
////        int count = 0;
//        int port = 2717;
//        int clientPort;
//        boolean slidingWindowParameter = false;
//        boolean dropPacket = false;
//        InetAddress[] inet;
//        String hostName = "moxie.oswego.edu";
//        String hostv6 = "0:0:0:0:0:ffff:8103:1403";
//        String requestURL = "http://cs.oswego.edu/~kbashfor/isc150/js8/digitalclock.html";
//        //String requestURL;
//        InetAddress v6;
//        InetAddress v4;
//        InetAddress clientAdd;
//        String para[] = new String[3];
//        Scanner in = new Scanner(System.in);
//
//        //v6 = getInet6AddressByName(hostName);
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
        System.out.println("---Note: Dropping packets is only done if Sliding window is n---");
        System.out.print("[Drop Packets   (y|n)] ");
        para[2] = in.next();

        if (para.length != 3) {
            System.out.println(usage);
            return;
        } else {
            version = Integer.parseInt(para[0]);
            if (para[1].equalsIgnoreCase("y")) {
                dropPacket = true;
            } else if (para[1].equalsIgnoreCase("n")) {
                dropPacket = false;
            } else {
                System.out.println(usage);
                return;

            }

            if (para[2].equalsIgnoreCase("y")) {
                slidingWindowParameter = true;
            } else if (para[2].equalsIgnoreCase("n")) {
                slidingWindowParameter = false;
            } else {
                System.out.println(usage);
                return;

            }
        }

        // v6 = getInet6AddressByName(hostName);
        v4 = InetAddress.getByName(hostName);

        DatagramSocket socket = new DatagramSocket(port);
        // temp buffer to read off of
        byte[] buf = new byte[512];
        DatagramPacket requestFromClient = new DatagramPacket(buf, buf.length);

        // recieve the request
        socket.receive(requestFromClient);

        clientAdd = requestFromClient.getAddress();
        clientPort = requestFromClient.getPort();

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
        File download = new File("download.html");
        fileSize = download.length();

        // the random access file allows easy parsing of bytes
        raf = new RandomAccessFile("download.html", "rw");

        // counts how many packets need to make based on the file size
        iterations = (int) Math.ceil(fileSize / dataBufSize);
        System.out.println("DEBUG iterations: " + iterations);
        
        // if statement used to randomly choose a packet between 
        // range of 1 and iterations -1 (zero is not included to 
        // cause less timeouts
        if(dropPacket == true){
          randomInt = randomGen.nextInt(iterations - 2);
          randomInt++;
        }

        if (slidingWindowParameter == false) {
            // sequentional sending
            boolean sentTerm = false;
            int countInter = 0;
            while (done == false) {
                // you are at the last packet  
                if (countInter + 1 == iterations) {
                    System.out.println("DEBUG: --- Last packet --- ");
                    sendPacket = new byte[(int) fileSize - (countInter * dataBufSize)];
                    // fileSize - (countInter * dataBufSize) guarentoos that the last 
                    // packet is no bigger than it needs to be
                    
                    // reads the last of the file              
                    raf.readFully(sendPacket, (countInter * dataBufSize), sendPacket.length);
                    TFTPPacket temp = new TFTPPacket().terminationPacket(countInter, clientAdd, clientPort, sendPacket);
                    socket.send(temp.dataPacket);
                    sentTerm = true;
//                    byte[] ack = new byte[4];
//                    byte[] ackCheck = new byte[4];
//                    DatagramPacket lastAck = new DatagramPacket(ack, ack.length);
//                    ackCheck = lastAck.getData();
//                    if (ackCheck[1] == (int) 4) {
//                        // checks wether the last packet recieved was an ack rather 
//                        // then an error code, other words resend if no true  
//                        done = true;
//                    } else {
//                        // resend all the packets since there was an error
//                        countInter = 0;
//                    }
                } else 
//                {
//                    sendPacket = new byte[dataBufSize];
//                    raf.readFully(sendPacket, (countInter * dataBufSize), sendPacket.length);
//                    TFTPPacket temp = new TFTPPacket().dataPacket(countInter, clientAdd, clientPort, sendPacket);
//                    socket.send(temp.dataPacket);
////                    countInter++;
//                }

                
                    if(randomInt == countInter){
                        System.out.println("DEBUG: --- Dropping packet --- ");
                      // purposely make it half the size  
                      sendPacket = new byte[dataBufSize / 2];
                      raf.readFully(sendPacket, (countInter * dataBufSize), sendPacket.length);
                      TFTPPacket temp = new TFTPPacket().dataPacket(countInter, clientAdd, clientPort, sendPacket);
                        System.out.println("DEBUG: packet size: " + temp.dataPacket.getLength());
                      socket.send(temp.dataPacket);
                      randomInt = -1;
                    } else {
                        System.out.println("DEBUG: --- Normal Packet --- ");
                      sendPacket = new byte[dataBufSize];
                      raf.readFully(sendPacket, (countInter * dataBufSize), sendPacket.length);
                      TFTPPacket temp = new TFTPPacket().dataPacket(countInter, clientAdd, clientPort, sendPacket);
                       System.out.println("DEBUG: packet size: " + temp.dataPacket.getLength());
                      socket.send(temp.dataPacket);
                    
                    }
                    
                    byte[] ack = new byte[512];
                    byte[] ackCheck;
                    DatagramPacket lastAck = new DatagramPacket(ack, ack.length);
                    socket.receive(lastAck);
                    ackCheck = lastAck.getData();
                     if ((int)ackCheck[1] ==  4 && (int)ackCheck[3] == countInter) {
                        // checks wether the last packet recieved was an ack rather 
                        // then an error code, other words resend if no true  
                        if(sentTerm == true)
                            done = true;
                        else
                            countInter++;
                    } else {
                        // resend all the packets since there was an error
                        countInter = 0;
                    }
            }

        } else {

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
