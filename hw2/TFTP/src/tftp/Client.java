
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Kyle
 */
public class Client {

    /**
     * @param args the command line arguments
     */
    static final int windowSize = 5;
    static int version = 0;
    static int port = 2717;
    static int dataBufSize = 512;
    static boolean slidingWindowParameter = false;
    static boolean done = false;
    static long startTime, endTime;
    static double totalTime;
    static String hostName = "moxie.oswego.edu";
    static String para[] = new String[3];
    static String fileName;
    static String urlRequest;
    static InetAddress serverAddress;
    static Scanner in = new Scanner(System.in);
    static Inet6Address v6;
    static InetAddress v4;
    static InetAddress serverv6;
    static Server serv = new Server();

    public static void main(String[] args) throws UnknownHostException, FileNotFoundException, SocketException, IOException {
        // TODO code application logic here

        String usage = "Parameters: [IP version 4|6][Sliding window y|n][URLRequest][fileName]";

        v4 = InetAddress.getByName(hostName);
        serverv6 = InetAddress.getByName("0:0:0:0:0:ffff:8103:1403");
        System.out.println("-----------------------------------------------");
        System.out.println(usage);
        System.out.print("[IP Version               (4|6)] ");
        para[0] = in.next();
        System.out.print("[Sliding Window           (y|n)] ");
        para[1] = in.next();
        System.out.print("[URL request with http://      ] ");
        urlRequest = in.next();
        System.out.print("[File to save as with extension] ");
        fileName = in.next();

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

        }

        // System.out.println("DEBUG: " + urlRequest);
        // make a read request for the client with the file name
        // this statement is used to set the address for the rest of the
        // protocol
        try {
            TFTPPacket request;
            OutputStream writeFile = new FileOutputStream(fileName);
            if (version == 4) {
                System.out.println("Selection: IPv4");
                serverAddress = v4;
            } else {
                System.out.println("Selection: IPv6");
                serverAddress = serverv6;
            }

            request = new TFTPPacket().readPacket(urlRequest, serverAddress, port);
//        try {
            // open a socket and send the request packet with the file
            DatagramSocket socket = new DatagramSocket();
            //  socket.setSoTimeout(6000);

            DatagramPacket sendReq = request.dataPacket;
            socket.send(sendReq);

//        } catch (Exception ex) {
//            ex.printStackTrace();
//            System.exit(-1);
//        }
            startTime = System.nanoTime();
            short countInter = 0;
            long totalPackets = 0;
            int loopBackCount = 0;
            if (slidingWindowParameter == false) {
                while (done == false) {
                 //   System.out.println("DEBUG: --- Top of loop --- ");
                    byte[] recData = new byte[dataBufSize + 128];
                    // the buffer is bigger the 512 since the client doesnt know what type
                    // of packet it will recieve, and the data itself is 512 bytes
                    byte[] check;
                    DatagramPacket recPack = new DatagramPacket(recData, recData.length);
                    socket.setSoTimeout(15000);
                    socket.receive(recPack);

                    check = recPack.getData();
                    byte byte0 = check[0];
                    byte byte1 = check[1];
                    byte byte2 = check[2];
                    byte byte3 = check[3];

                    // converts bytes to readable shorts
                    short codeNum = (short) ((byte1 << 8) + byte0);
                    short blockNum = (short) ((byte3 << 8) + byte2);
                    // checkBlocks coverts the short to an int since 
                    // shorts can only hold 128 posiitive values
                    long checkBlocks = ((long) blockNum + (loopBackCount * 128));
                 //   System.out.println("DEBUG: Code Number " + codeNum);
                 //   System.out.println("DEBUG: Block Number " + blockNum);
                 //   System.out.println("DEBUG: total packets " + totalPackets);

                    if (codeNum == (short) 3 && checkBlocks == totalPackets) {
                        // this is a normal data packet
                        //debug 
                      //  System.out.println("DEBUG: --- Recieved Packet ---");
                        byte[] temp = new byte[dataBufSize];
                        System.arraycopy(check, 4, temp, 0, temp.length);
                        //writeFile.write(temp, ((int)(countInter * dataBufSize)), temp.length);
                        writeFile.write(temp);
                        TFTPPacket ack = new TFTPPacket().ackPacket(countInter, serverAddress, port);
                        socket.send(ack.dataPacket);
                        if (countInter < 127) {
                            countInter++;
                        } else {
                            countInter = 0;
                            loopBackCount++;
                        }
                        totalPackets++;
                    } else if (codeNum == (short) 6) {
                        // this is the last data packet
                        // debug
                        System.out.println("DEBUG: --- Last Packet ---");
                        byte[] temp = new byte[dataBufSize];
                        System.arraycopy(check, 4, temp, 0, temp.length);
                        //writeFile.write(temp, ((int)(countInter * dataBufSize)), temp.length);
                        writeFile.write(temp);
                        TFTPPacket ack = new TFTPPacket().ackPacket(countInter, serverAddress, port);
                        socket.send(ack.dataPacket);
                        done = true;
                        totalPackets++;
                    } else {
                        System.out.println("Sending an Error Packet");
                        // if there the data packet is smaller than usual and not a terminal packet
                        TFTPPacket error = new TFTPPacket().errorPacket((short) 0, serverAddress, port);
                        socket.send(error.dataPacket);
                        // after sending an error
                        // set the stream to rewrite over the file
                        writeFile = new FileOutputStream(fileName, false);
                        countInter = 0;
                        totalPackets = 0;
                        loopBackCount = 0;
                    }
                }

            } else {
                // Sliding window algorithm
                slidingWindow:
                while (done == false) {
                    short codeNum = 0;
                    short blockNum = 0;
                    long checkBlocks = 0;
                    byte[] check = new byte[0];
                    //System.out.println("DEBUG: --- Top of loop --- ");

                    // this accepts the window
                    for (int i = 0; i < windowSize; i++) {
                        byte[] recData = new byte[dataBufSize + 128];
//                    byte[] check;
                        DatagramPacket recPack = new DatagramPacket(recData, recData.length);
                        socket.setSoTimeout(15000);
		
                        socket.receive(recPack);
                        
                        check = recPack.getData();
                        byte byte0 = check[0];
                        byte byte1 = check[1];
                        byte byte2 = check[2];
                        byte byte3 = check[3];

                        // converts bytes to readable shorts
                        codeNum = (short) ((byte1 << 8) + byte0);
                        blockNum = (short) ((byte3 << 8) + byte2);
                        // checkBlocks coverts the short to an int since 
                        // shorts can only hold 128 posiitive values
                        checkBlocks = ((long) blockNum + (loopBackCount * 128));
                       // System.out.println("DEBUG: Code Number " + codeNum);
                       // System.out.println("DEBUG: Block Number " + blockNum);
                       // System.out.println("DEBUG: total packets " + totalPackets);
                        byte[] temp = new byte[dataBufSize];
                        System.arraycopy(check, 4, temp, 0, temp.length);
                        //writeFile.write(temp, ((int)(countInter * dataBufSize)), temp.length);
                        writeFile.write(temp);
                        if (codeNum == (short) 6) {
                            // this is the last data packet
                            // debug
                          //  System.out.println("DEBUG: --- Last Packet ---");
//                            byte[] temp = new byte[dataBufSize];
//                            System.arraycopy(check, 4, temp, 0, temp.length);
//                            //writeFile.write(temp, ((int)(countInter * dataBufSize)), temp.length);
//                            writeFile.write(temp);
                            TFTPPacket ack = new TFTPPacket().ackPacket(countInter, serverAddress, port);
                            socket.send(ack.dataPacket);
                            done = true;
                            break slidingWindow;

                        } else if (codeNum != (short) 6 && countInter < 127) {
                            countInter++;
                        } else {
                            countInter = 0;
                            loopBackCount++;
                        }
                        totalPackets++;
                        System.out.println("DEBUG: Count Interations " + countInter);
                     //   System.out.println("DEBUG: Check Block " + checkBlocks);
                    }

                    if (done == false) {
                        if (codeNum == (short) 3 && checkBlocks + 1 == totalPackets) {
                       //     System.out.println("DEBUG: --- Recieved Packets ---");
                       //     System.out.println("DEBUG: --- Sending Ack Packet ---");
                            byte[] temp = new byte[dataBufSize];
                            System.arraycopy(check, 4, temp, 0, temp.length);
                            TFTPPacket ack = new TFTPPacket().ackPacket(countInter, serverAddress, port);
                            socket.send(ack.dataPacket);
                        } else {
                            System.out.println("Sending an Error Packet");
                            // if there the data packet is smaller than usual and not a terminal packet
                            TFTPPacket error = new TFTPPacket().errorPacket((short) 0, serverAddress, port);
                            socket.send(error.dataPacket);
                        // after sending an error
                            // set the stream to rewrite over the file
                            writeFile = new FileOutputStream(fileName, false);
                            countInter = 0;
                            totalPackets = 0;
                            loopBackCount = 0;
                        }

                    }
                }
            }
            endTime = System.nanoTime() - startTime;
            writeFile.close();
            if (done == true) {
                // displays information after a transfer
                File f = new File(fileName);
                totalTime = (double)endTime / 1000000000.0;
                System.out.println("File: " + fileName);
                System.out.println("File Size: " + f.length() + " bytes");
                System.out.println("Time Transfer (nano seconds): " + endTime);
                System.out.println("Time Transfer (seconds): " + totalTime);
            } else {
                System.out.println("Could not download file");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
