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


import java.io.*;

public class Pmraf extends RandomAccessFile {

    File destination;
    int blockSize;

    public Pmraf(int blockSize) throws IOException {
        super(new File("Pmap.dat"), "rw");
        this.blockSize = blockSize;
        destination = new File("Pmap.dat");

    }

    public Pmraf(int blockSize, File destination) throws IOException {

        super(destination, "rw");
        this.blockSize = blockSize;
        this.destination = destination;

    }

    public byte[] read(int blockNumber) {

        byte[] temp = new byte[blockSize];
        int offset = blockNumber * blockSize;

        try {
            super.seek(offset);
            super.readFully(temp);
            return temp;

        } catch (IOException e) {

            return null;
        }

    }

    //for when # of blocks is smaller than the capacity
    public void write(byte[] w, int location) {
        try {

            int offset = location * blockSize;
            super.seek(offset);
            super.write(w);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeAtEnd(byte[] w, int size) {
        try {

            //Try to shift here!!!!!
            for (int i = 0; i < size - 1; i++) {
                this.write(this.read(i + 1), i);
            }
            int offset = (size - 1) * blockSize;
            super.seek(offset);
            super.write(w);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
