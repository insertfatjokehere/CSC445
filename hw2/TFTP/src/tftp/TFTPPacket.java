/*
 TFTP Formats

 Type   Op #     Format without header

         2 bytes    string   1 byte     string   1 byte
        -----------------------------------------------
 RRQ/  | 01/02 |  Filename  |   0  |    Mode    |   0  |
 WRQ    -----------------------------------------------
         2 bytes    2 bytes       n bytes
        ---------------------------------
 DATA  | 03    |   Block #  |    Data    |
        ---------------------------------
          2 bytes    2 bytes
        -------------------
 ACK   |   04    |   Block # |
        --------------------
         2 bytes  2 bytes        string    1 byte
        ----------------------------------------
 ERROR | 05    |  ErrorCode |   ErrMsg   |   0  |
        ----------------------------------------

 */

/**
 *
 * @author Kyle
 */
import java.net.DatagramPacket;
import java.net.InetAddress;

public class TFTPPacket {

    public static final int readCode = 1;
    public static final int dataCode = 3;
    public static final int ackCode = 4;
    public static final int errorCode = 5;
    public static final int maxPacketSize = 512;
    public static final String[] errorMess = {"Error in transfer", "File not found.", "Access violation.", "Disk full or allocation exceeded.",
        "Illegal TFTP operation.", "Unknown transfer ID.", "File already exists.", "No such user."};
    private int type;
    int port;
    int blockNum;
    String urlRequest;
    InetAddress address;
    DatagramPacket dataPacket;
    byte[] dataBytes;
    byte[] mode = {(byte) 'o', (byte) 'c', (byte) 't', (byte) 'e', (byte) 't'};

//        public TFTPPacket(InetAddress add, int portNum, int flag){
//		type = flag;
//		port = portNum;
//		address = add;
//	}
    public TFTPPacket readPacket(String urlFromClient, InetAddress inet, int port) {
        TFTPPacket request = new TFTPPacket();
        request.type = readCode;
        request.urlRequest = urlFromClient;
        // the 4 counts the opcode and the 2 other bytes in the RRQ request
        request.dataBytes = new byte[request.urlRequest.length()
                //+ mode.length 
                //+ 4];
                + 2];

        // this takes care of the first 2 bytes
        request.dataBytes[0] = 0;
        request.dataBytes[1] = (byte) request.type;

        // this takes care of the filename and the next byte
        System.arraycopy(request.urlRequest.getBytes(), 0, request.dataBytes, 2, request.urlRequest.length());
        //request.dataBytes[request.urlRequest.length() + 2] = 0;

        // this addes the octet mode and the last 0 in the block
        // System.arraycopy(request.mode, 0, request.dataBytes, request.urlRequest.length() + 3, request.mode.length);
        // request.dataBytes[request.urlRequest.length() + mode.length + 3] = 0;
        // System.out.println("DEBUG: Mode length " + request.mode.length);
        request.dataPacket = new DatagramPacket(request.dataBytes, request.dataBytes.length, inet, port);

        return request;
    }
    /*
     public TFTPPacket readPacket(String urlFromClient, InetAddress inet, int port) {
     TFTPPacket request = new TFTPPacket();
     request.type = readCode;
     request.urlRequest = urlFromClient;
     // the 4 counts the opcode and the 2 other bytes in the RRQ request
     request.dataBytes = new byte[request.urlRequest.length() 
     + mode.length 
     + 4];
                

     // this takes care of the first 2 bytes
     request.dataBytes[0] = 0;
     request.dataBytes[1] = (byte) request.type;

     // this takes care of the filename and the next byte
     System.arraycopy(request.urlRequest.getBytes(), 0, request.dataBytes, 2, request.urlRequest.length());
     request.dataBytes[request.urlRequest.length() + 2] = 0;

     // this addes the octet mode and the last 0 in the block
     System.arraycopy(request.mode, 0, request.dataBytes, request.urlRequest.length() + 3, request.mode.length);
     request.dataBytes[request.urlRequest.length() + mode.length + 3] = 0;
     // System.out.println("DEBUG: Mode length " + request.mode.length);

     request.dataPacket = new DatagramPacket(request.dataBytes, request.dataBytes.length, inet, port);

     return request;
     }
    
     */

    public TFTPPacket ackPacket(int blockNum, InetAddress inet, int port) {
        TFTPPacket ack = new TFTPPacket();
        ack.type = ackCode;
        ack.dataBytes = new byte[4];
        ack.dataBytes[0] = 0;
        ack.dataBytes[1] = (byte) ack.type;
        ack.dataBytes[3] = (byte) blockNum;
        ack.dataPacket = new DatagramPacket(ack.dataBytes, ack.dataBytes.length, inet, port);

        return ack;
    }

    public TFTPPacket dataPacket(int blockNum, InetAddress inet, int port, byte[] info) {
        TFTPPacket dataP = new TFTPPacket();
        dataP.type = dataCode;
        dataP.dataBytes = new byte[info.length + 4];
        dataP.dataBytes[0] = 0;
        dataP.dataBytes[1] = (byte) dataP.type;
        dataP.dataBytes[3] = (byte) blockNum;
        System.arraycopy(info, 0, dataP.dataBytes, 4, info.length);

        dataP.dataPacket = new DatagramPacket(dataP.dataBytes, dataP.dataBytes.length, inet, port);
        return dataP;
    }

    public TFTPPacket terminationPacket(int blockNum, InetAddress inet, int port, byte[] info) {
        TFTPPacket term = new TFTPPacket().dataPacket(blockNum, inet, port, info);
       // a -1 type is used to indicate the last packet
        // block number needs to be the same since it needs to check
        term.type = -1;
        term.dataBytes[1] = (byte) term.type;

        return term;
    }

    public TFTPPacket errorPacket(int errorNum, InetAddress inet, int port) {
        TFTPPacket error = new TFTPPacket();
        error.type = errorCode;
        error.dataBytes = new byte[maxPacketSize];
        error.dataBytes[0] = 0;
        error.dataBytes[1] = (byte) error.type;
        error.dataBytes[2] = 0;
        error.dataBytes[3] = (byte) errorNum;
        System.arraycopy(errorMess[errorNum].getBytes(), 0, error.dataBytes, 4, errorMess[errorNum].length());
        error.dataBytes[errorMess[errorNum].length() + 4] = 0;
        
        error.dataPacket = new DatagramPacket(error.dataBytes, error.dataBytes.length, inet, port);

        return error;
    }
}
