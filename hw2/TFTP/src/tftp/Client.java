
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
    static int version = 0;
    static int port = 2717;
    static int dataBufSize = 512;
    static boolean slidingWindowParameter = false;
    static boolean dropPacket = false;
    static boolean done = false;
    static long startTime, endTime, totalTime;
    static String hostName = "moxie.oswego.edu";
    static String para[] = new String[3];
    static String fileName;
    static String urlRequest;
    static InetAddress serverAddress;
    static Scanner in = new Scanner(System.in);
    static Inet6Address v6;
//     static InetAddress v4 = InetAddress.getLocalHost();
//     static InetAddress serverv6 = InetAddress.getByName("0:0:0:0:0:ffff:8103:1403");
    static InetAddress v4;
    static InetAddress serverv6;
    static Server serv = new Server();

    public static void main(String[] args) throws UnknownHostException, FileNotFoundException, SocketException, IOException {
        // TODO code application logic here

        String usage = "Parameters: [IP version 4|6][Sliding window y|n][Drop y|n]";
//        int version = 0;
//        int port = 2717;
//        boolean slidingWindowParameter = false;
//        boolean dropPacket = false;
//        long startTime, endTime, totalTime;
//        String hostName = "moxie.oswego.edu";
//        String para[] = new String[3];
//        String fileName;
//        Scanner in = new Scanner(System.in);
//        Inet6Address v6;
        v4 = InetAddress.getLocalHost();
        serverv6 = InetAddress.getByName("0:0:0:0:0:ffff:8103:1403");
//        Server serv = new Server();

        // v6 = getInet6AddressByName(hostName);
//        System.out.println("-----------------------------------------------");
//        System.out.println("IPv4 address of client " + v4.getCanonicalHostName());
//        //  System.out.println("IPv4 address of server " + v6.getHostAddress());
        System.out.println("-----------------------------------------------");
        System.out.println(usage);
        System.out.print("[IP Version               (4|6)] ");
        para[0] = in.next();
        System.out.print("[Sliding Window           (y|n)] ");
        para[1] = in.next();
        System.out.println("---Note: Dropping packets is only done if Sliding window is n---");
        System.out.print("[Drop Packets             (y|n)] ");
        para[2] = in.next();
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

        // System.out.println("DEBUG: " + urlRequest);
        // make a read request for the client with the file name
        // this statement is used to set the address for the rest of the
        // protocol
        TFTPPacket request;
        OutputStream writeFile = new FileOutputStream(fileName);
        if (version == 4) {
            serverAddress = v4;
        } else {
            serverAddress = serverv6;
        }

        request = new TFTPPacket().readPacket(urlRequest, serverAddress, port);
//        try {
        // open a socket and send the request packet with the file
        DatagramSocket socket = new DatagramSocket();

        DatagramPacket sendReq = request.dataPacket;
        socket.send(sendReq);

//        } catch (Exception ex) {
//            ex.printStackTrace();
//            System.exit(-1);
//        }
        startTime = System.nanoTime();
        int countInter = 0;
        if (slidingWindowParameter == false) {
            while (done == false) {
                byte[] recData = new byte[dataBufSize + 128];
                // the buffer is bigger the 512 since the client doesnt know what type
                // of packet it will recieve, and the data itself is 512 bytes
                byte[] check;
                DatagramPacket recPack = new DatagramPacket(recData, recData.length);
                socket.receive(recPack);

                check = recPack.getData();

                if ((int) check[2] == 3 && (int) check[3] == countInter && check.length > dataBufSize) {
                    // this is a normal data packet
                    byte[] temp = new byte[dataBufSize];
                    System.arraycopy(check, 4, temp, 0, temp.length);
                    writeFile.write(temp, (countInter * dataBufSize), temp.length);

                    TFTPPacket ack = new TFTPPacket().ackPacket(countInter, serverAddress, port);
                    socket.send(ack.dataPacket);
                    countInter++;
                } else if ((int) check[1] == -1) {
                    // this is the last data packet
                    byte[] temp = new byte[dataBufSize];
                    System.arraycopy(check, 4, temp, 0, temp.length);
                    writeFile.write(temp, (countInter * dataBufSize), temp.length);

                    TFTPPacket ack = new TFTPPacket().ackPacket(countInter, serverAddress, port);
                    socket.send(ack.dataPacket);
                    done = true;
                } else {
                    // if there the data packet is smaller than usual and not a terminal packet
                    TFTPPacket error = new TFTPPacket().errorPacket(0, serverAddress, port);
                    socket.send(error.dataPacket);
                    // after sending an error
                    // set the stream to rewrite over the file
                    writeFile = new FileOutputStream(fileName, false);
                    countInter = 0;
                }
            }

        } else {

        }

    }
}
