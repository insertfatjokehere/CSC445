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


import java.util.ArrayList;
import java.io.*;

public class PArray {

    int size;
    int arrayCapacity;

    PNode root;
    Pmraf file;
    //First integer returns the capacity
    //second integer returns the number of nodes

    private static ArrayList<Integer> hdrInfo;

    public PArray(int arrayCapacity, Pmraf file) throws IOException, ClassNotFoundException {

        //Finish this part
        readInfo();
        if (hdrInfo.size() != 0) {
            arrayCapacity = hdrInfo.get(0).intValue();

            size = hdrInfo.get(1).intValue();


        } else {
            this.arrayCapacity = arrayCapacity;

            this.size = size;
        }
        this.file = file;

        if (file.length() != 0) {

            readRoot();

        } else {

            root = null;
            if (size != 0) {
                System.err.println("hdrInfo is not consistent comparing with the fmraf file!");
            }
        }

    }

    public int size() {

        return size;

    }

    public void put(byte[] k, byte[] v) throws IOException {

        add(k, v);
    }

    private void add(byte[] k, byte[] v) throws IOException {

        if (root == null) {
            root = new PNode(k, v);
            root.save(0, file);
            size++;
        } else {
            if (size == arrayCapacity) {

                new PNode(k, v).save(file, size);
                root = root.load(0, file);

            } else {

                new PNode(k, v).save(size, file);
                size++;
            }

        }
        saveInfo();

    }

    public String getByKey(byte[] key) {

        for (int i = 0; i < size; i++) {

            PNode temp = root.load(i, file);
            if (temp.key == key) {
                return temp.getValue();
            }

        }
        return null;

    }

    public String[] getByOrder(int location) {
        PNode tempNode = root.load(location, file);
        String[] temp = {tempNode.getKey(), tempNode.getValue()};
        return temp;

    }

    //return the userName with the message in a string array
    public String[] getLastest() {

        PNode temp = root.load(size, file);
        String[] sTemp = {temp.getKey(), temp.getValue()};
        return sTemp;
    }

    //only returns all the messages
    public String[][] getAll() {

        String[][] temp = new String[2][size];
        for (int i = 0; i < size; i++) {
            PNode tempNode = root.load(i, file);
            temp[0][i] = tempNode.getKey();
            temp[1][i] = tempNode.getValue();
        }

        return temp;
    }

    public void readRoot() {

        byte[] temp = file.read(0);
        String userName = new String(temp, 0, 12);

        String message = new String(temp, 16, 512);

        root = new PNode(userName.getBytes(), message.getBytes());

    }

    private void readInfo() throws IOException, ClassNotFoundException {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream("hdrInfo.dat"));

            @SuppressWarnings("unchecked")
            ArrayList<Integer> temp = (ArrayList<Integer>) ois.readObject();
            hdrInfo = temp;
        } catch (FileNotFoundException e) {
            hdrInfo = new ArrayList<Integer>();
        } catch (EOFException eof) {
            hdrInfo = new ArrayList<Integer>();
        }
    }

    protected void saveInfo() throws IOException {

        hdrInfo.add(new Integer(arrayCapacity));
        //hdrInfo.add(new Integer(blockCapacity));
        hdrInfo.add(new Integer(size));
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("hdrInfo.dat"));
        oos.writeObject(hdrInfo);
        oos.close();

    }
}
