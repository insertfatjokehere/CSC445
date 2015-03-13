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

import java.io.Serializable;
import java.io.IOException;

public class PNode implements Serializable {

    byte[] key;
    byte[] value;
    int number;

    public PNode(byte[] key, byte[] value) {

        this.key = key;
        this.value = value;

    }

    public PNode(byte[] key, byte[] value, int number) {

        this.key = key;
        this.value = value;
        this.number = number;

    }

    protected String getKey() {

        return new String(key);

    }

    protected String getValue() {

        return new String(value);

    }

    //location is the node number
    protected void save(int location, Pmraf file) throws IOException {

        String combine = new String(key) + "!@#$" + new String(value);
        byte[] bCombine = combine.getBytes();
        file.write(bCombine, location);

    }

    protected void save(Pmraf file, int size) throws IOException {

        String combine = new String(key) + "!@#$" + new String(value);
        byte[] bCombine = combine.getBytes();
        file.writeAtEnd(bCombine, size);

    }

    protected PNode load(int location, Pmraf file) {

        byte[] temp = file.read(location);

        String userName = new String(temp, 0, 12);

        String message = new String(temp, 16, 512);

        return new PNode(userName.getBytes(), message.getBytes());

    }

}
