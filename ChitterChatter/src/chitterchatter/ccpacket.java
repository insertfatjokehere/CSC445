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
import java.net.DatagramPacket;
import java.net.InetAddress;

public class ccpacket {

    // final short values that distinguishes different packets
    protected final short sendCode = 1;
    protected final short leaveCode = 2;
    protected final short joinCode = 3;
    protected final short printCode = 4;
    protected final short startKickVote = 5;
    protected final short voteKickCode = 6;
    protected final short updateUsers = 7;
    protected final short voteResultCode = 8;
    protected final short pingCode = 9;
    protected final short pingAckCode = 10;
    protected final short sendLatestCode = 14;
    protected final short unsuccessfulJoinCode = 15;
    protected final short startKickVoteFromJudge = 16;
    protected final short resultsForJudge = 17;
    protected final short checkSumCode = 18;
    protected final short getUsersCode = 19;
    protected final short sendUsersCode = 20;
    protected final short joinSuccCode = 21;
    protected final short getUsersCodeForGui = 22;
    protected final short sendUserCodeForGui = 23;
    protected final short getAllUsersCode = 24;
    protected final short sendAllUsersCode = 25;
    protected final short heartbeatCode = 26;
    protected final short heartbeatSend = 27;
    protected final short requestChatForServerCode = 28;
    protected final short sendChatForServerCode = 29;
    protected final short updateChatForServerCode = 30;

    public final int messageLength = 512;

    public final int usernameLength = 12;

    // variables that change between packets
    byte[] data;
    DatagramPacket datapacket;

    public ccpacket sendPacketThruMult(String username, String mes, InetAddress add, int port) {
        ccpacket send = new ccpacket();

        send.data = new byte[usernameLength + mes.getBytes().length + 2];
        send.data[0] = (byte) (sendCode & 0xff);
        send.data[1] = (byte) ((sendCode >> 8) & 0xff);
        System.arraycopy(username.getBytes(), 0, send.data, 2, usernameLength);
        System.arraycopy(mes.getBytes(), 0, send.data, 2 + usernameLength, mes.getBytes().length);

        send.datapacket = new DatagramPacket(send.data, send.data.length, add, port);
        return send;
    }

    public ccpacket sendPacketThruDatagram(String username, String mes, InetAddress add, int port) {
        ccpacket send = new ccpacket();

        send.data = new byte[usernameLength + mes.getBytes().length + 2];
        send.data[0] = (byte) (sendCode & 0xff);
        send.data[1] = (byte) ((sendCode >> 8) & 0xff);
        byte[] useTemp = new byte[usernameLength];
        useTemp = username.getBytes();
        System.arraycopy(useTemp, 0, send.data, 2, usernameLength);
        System.arraycopy(mes.getBytes(), 0, send.data, 2 + usernameLength, mes.getBytes().length);

        send.datapacket = new DatagramPacket(send.data, send.data.length, add, port);
        return send;
    }

    public ccpacket leavePacket(String username, InetAddress add, int port) {
        ccpacket leave = new ccpacket();

        leave.data = new byte[usernameLength + 2];
        leave.data[0] = (byte) (leaveCode & 0xff);
        leave.data[1] = (byte) ((leaveCode >> 8) & 0xff);

        System.arraycopy(username.getBytes(), 0, leave.data, 2, usernameLength);

        leave.datapacket = new DatagramPacket(leave.data, leave.data.length, add, port);
        return leave;

    }

    public ccpacket leaveChat(String username, InetAddress add, int port) {

        ccpacket leave = new ccpacket();
        String quit1 = "***";
        String quit2 = " has left chat***";
        leave.data = new byte[quit1.getBytes().length + username.getBytes().length + quit2.getBytes().length + 2];
        leave.data[0] = (byte) (leaveCode & 0xff);
        leave.data[1] = (byte) ((leaveCode >> 8) & 0xff);
        // append the username to the chat
        String mes = quit1 + username + quit2;
        System.arraycopy(mes.getBytes(), 0, leave.data, 2, mes.getBytes().length);

        leave.datapacket = new DatagramPacket(leave.data, leave.data.length, add, port);
        return leave;

    }

    public ccpacket joinPacket(String username, InetAddress add, int port) {
        ccpacket join = new ccpacket();

        join.data = new byte[usernameLength + 2];
        join.data[0] = (byte) (joinCode & 0xff);
        join.data[1] = (byte) ((joinCode >> 8) & 0xff);
        System.arraycopy(username.getBytes(), 0, join.data, 2, usernameLength);
        join.datapacket = new DatagramPacket(join.data, join.data.length, add, port);

        return join;
    }

    public ccpacket successfulJoin(String username, InetAddress add, int port) {
        ccpacket join = new ccpacket();
        String part1 = "*** ";
        String part2 = " has join chat***";
        join.data = new byte[part1.getBytes().length + username.getBytes().length + part2.getBytes().length + 2];
        join.data[0] = (byte) (joinCode & 0xff);
        join.data[1] = (byte) ((joinCode >> 8) & 0xff);
        String mess = part1 + username + part2;
        System.arraycopy(mess.getBytes(), 0, join.data, 2, mess.getBytes().length);
        join.datapacket = new DatagramPacket(join.data, join.data.length, add, port);

        return join;
    }

    public ccpacket updateUsers(String username, short flag, InetAddress add, int port) {
        ccpacket update = new ccpacket();

        update.data = new byte[usernameLength + 4];
        update.data[0] = (byte) (updateUsers & 0xff);
        update.data[1] = (byte) ((updateUsers >> 8) & 0xff);
        update.data[2] = (byte) (flag & 0xff);
        update.data[3] = (byte) ((flag >> 8) & 0xff);
        System.arraycopy(username.getBytes(), 0, update.data, 4, usernameLength);
        update.datapacket = new DatagramPacket(update.data, update.data.length, add, port);

        return update;
    }

    public ccpacket printRequest(InetAddress add, int portNum) {
        ccpacket print = new ccpacket();

        print.data = new byte[2];
        print.data[0] = (byte) (printCode & 0xff);
        print.data[1] = (byte) ((printCode >> 8) & 0xff);

        print.datapacket = new DatagramPacket(print.data, print.data.length, add, portNum);
        return print;
    }

    public ccpacket printPacket(String[][] messages, InetAddress add, int port) {
        ccpacket print = new ccpacket();

        print.data = new byte[4 + ((usernameLength + messageLength) * messages[0].length)];
        print.data[0] = (byte) (printCode & 0xff);
        print.data[1] = (byte) ((printCode >> 8) & 0xff);

        short tempCode = (short) messages[0].length;

        print.data[2] = (byte) (tempCode & 0xff);
        print.data[3] = (byte) ((tempCode >> 8) & 0xff);

        for (int i = 0; i < messages[0].length; i++) {
            System.arraycopy(messages[0][i].getBytes(), 0, print.data, ((i * usernameLength) + (i * messageLength) + 4), usernameLength);
            System.arraycopy(messages[1][i].getBytes(), 0, print.data, (((i * usernameLength) + (i * messageLength) + 4) + usernameLength), messageLength);
        }

        print.datapacket = new DatagramPacket(print.data, print.data.length, add, port);
        return print;
    }

    public ccpacket startKick(String username, InetAddress add, int port) {
        ccpacket startKick = new ccpacket();
        String part1 = "***Vote for kicking ";
        String part2 = " is in session***";
        startKick.data = new byte[part1.getBytes().length + (2 * usernameLength) + part2.getBytes().length + 2];
        String mes = part1 + username + part2;

        startKick.data[0] = (byte) (startKickVote & 0xff);
        startKick.data[1] = (byte) ((startKickVote >> 8) & 0xff);
        System.arraycopy(username.getBytes(), 0, startKick.data, 2, username.getBytes().length);
        System.arraycopy(mes.getBytes(), 0, startKick.data, 14, mes.getBytes().length);

        startKick.datapacket = new DatagramPacket(startKick.data, startKick.data.length, add, port);

        return startKick;

    }

    public ccpacket kickPacket(String username, boolean b, InetAddress add, int port) {
        ccpacket kick = new ccpacket();
        short result;
        if (b == true) {
            result = 0;
        } else {
            result = 1;
        }

        kick.data = new byte[usernameLength + 4];
        kick.data[0] = (byte) (voteKickCode & 0xff);
        kick.data[1] = (byte) ((voteKickCode >> 8) & 0xff);
        kick.data[2] = (byte) (result & 0xff);
        kick.data[3] = (byte) ((result >> 8) & 0xff);
        System.arraycopy(username.getBytes(), 0, kick.data, 4, username.getBytes().length);

        kick.datapacket = new DatagramPacket(kick.data, kick.data.length, add, port);

        return kick;
    }

    public ccpacket kickResults(String username, boolean b, InetAddress add, int port) {
        short result;
        if (b == true) {
            result = 0;
        } else {
            result = 1;
        }
        ccpacket kick = new ccpacket();

        kick.data = new byte[2 + usernameLength + 2];
        kick.data[0] = (byte) (voteResultCode & 0xff);
        kick.data[1] = (byte) ((voteResultCode >> 8) & 0xff);
        kick.data[2] = (byte) (result & 0xff);
        kick.data[3] = (byte) ((result >> 8) & 0xff);
        System.arraycopy(username.getBytes(), 0, kick.data, 4, usernameLength);

        kick.datapacket = new DatagramPacket(kick.data, kick.data.length, add, port);

        return kick;
    }

    public ccpacket ping(InetAddress add, int port) {
        ccpacket ping = new ccpacket();

        ping.data = new byte[2];
        ping.data[0] = (byte) (pingCode & 0xff);
        ping.data[1] = (byte) ((pingCode >> 8) & 0xff);

        ping.datapacket = new DatagramPacket(ping.data, ping.data.length, add, port);

        return ping;
    }

    public ccpacket pingAck(InetAddress add, int port) {

        ccpacket ping = new ccpacket();

        ping.data = new byte[2];
        ping.data[0] = (byte) (pingAckCode & 0xff);
        ping.data[1] = (byte) ((pingAckCode >> 8) & 0xff);

        ping.datapacket = new DatagramPacket(ping.data, ping.data.length, add, port);

        return ping;
    }

    public ccpacket sendLatest(String[] mess, InetAddress add, int port) {
        ccpacket late = new ccpacket();

        late.data = new byte[2 + usernameLength + mess[1].getBytes().length];
        late.data[0] = (byte) (sendLatestCode & 0xff);
        late.data[1] = (byte) ((sendLatestCode >> 8) & 0xff);
        System.arraycopy(mess[0].getBytes(), 0, late.data, 2, usernameLength);
        System.arraycopy(mess[1].getBytes(), 0, late.data, 2 + usernameLength, mess[1].getBytes().length);
        late.datapacket = new DatagramPacket(late.data, late.data.length, add, port);

        return late;

    }

    public ccpacket unsuccessfulJoin(InetAddress add, int port) {
        ccpacket join = new ccpacket();
        String mess = "***Chould not join chat, either username is taken or you're banned***";
        join.data = new byte[2 + mess.getBytes().length];
        join.data[0] = (byte) (unsuccessfulJoinCode & 0xff);
        join.data[1] = (byte) ((unsuccessfulJoinCode >> 8) & 0xff);
        System.arraycopy(mess.getBytes(), 0, join.data, 2, mess.getBytes().length);

        join.datapacket = new DatagramPacket(join.data, join.data.length, add, port);
        return join;
    }

    public ccpacket voteSessionFromJudge(String username, InetAddress add, int port) {

        ccpacket vote = new ccpacket();
        String part1 = "***Vote for kicking ";
        String part2 = " is in session***";
        vote.data = new byte[part1.getBytes().length + (2 * usernameLength) + part2.getBytes().length + 2];
        String mes = part1 + username + part2;

        vote.data[0] = (byte) (startKickVoteFromJudge & 0xff);
        vote.data[1] = (byte) ((startKickVoteFromJudge >> 8) & 0xff);

        System.arraycopy(username.getBytes(), 0, vote.data, 2, usernameLength);
        System.arraycopy(mes.getBytes(), 0, vote.data, usernameLength + 2, mes.getBytes().length);

        vote.datapacket = new DatagramPacket(vote.data, vote.data.length, add, port);

        return vote;
    }

    public ccpacket voteResults(short yvotes, short nvotes, InetAddress add, int port) {
        ccpacket vote = new ccpacket();

        vote.data = new byte[6];
        vote.data[0] = (byte) (resultsForJudge & 0xff);
        vote.data[1] = (byte) ((resultsForJudge >> 8) & 0xff);
        vote.data[2] = (byte) (yvotes & 0xff);
        vote.data[3] = (byte) ((yvotes >> 8) & 0xff);
        vote.data[4] = (byte) (nvotes & 0xff);
        vote.data[5] = (byte) ((nvotes >> 8) & 0xff);

        vote.datapacket = new DatagramPacket(vote.data, vote.data.length, add, port);

        return vote;
    }

    public ccpacket checksum(byte[][] message, InetAddress add, int port) {
        ccpacket check = new ccpacket();

        check.data = new byte[(usernameLength * ServerHandler.numOfStoredMessage) + 2];
        check.data[0] = (byte) (checkSumCode & 0xff);
        check.data[1] = (byte) ((checkSumCode >> 8) & 0xff);

        for (int i = 0; i < ServerHandler.numOfStoredMessage; i++) {
            System.arraycopy(message[i], 0, check.data, (i * usernameLength) + 2, usernameLength);
        }

        check.datapacket = new DatagramPacket(check.data, check.data.length, add, port);

        return check;
    }

    public ccpacket getUsers(InetAddress add, int port) {

        ccpacket users = new ccpacket();

        users.data = new byte[2];
        users.data[0] = (byte) (getUsersCode & 0xff);
        users.data[1] = (byte) ((getUsersCode >> 8) & 0xff);

        users.datapacket = new DatagramPacket(users.data, users.data.length, add, port);

        return users;
    }

    public ccpacket sendUsers(String[] s, InetAddress add, int port) {

        ccpacket users = new ccpacket();

        users.data = new byte[2 + (s.length * usernameLength)];
        users.data[0] = (byte) (sendUsersCode & 0xff);
        users.data[1] = (byte) ((sendUsersCode >> 8) & 0xff);

        for (int i = 0; i < s.length; i++) {
            System.arraycopy(s[i].getBytes(), 0, users.data, 2 + (usernameLength * i), usernameLength);
        }

        users.datapacket = new DatagramPacket(users.data, users.data.length, add, port);

        return users;
    }

    public ccpacket joinSuccPack(InetAddress add, int port) {

        ccpacket join = new ccpacket();

        join.data = new byte[2];
        join.data[0] = (byte) (joinSuccCode & 0xff);
        join.data[1] = (byte) ((joinSuccCode >> 8) & 0xff);

        join.datapacket = new DatagramPacket(join.data, join.data.length, add, port);

        return join;
    }

    public ccpacket getUsersForGui(InetAddress add, int port) {

        ccpacket users = new ccpacket();

        users.data = new byte[2];
        users.data[0] = (byte) (getUsersCodeForGui & 0xff);
        users.data[1] = (byte) ((getUsersCodeForGui >> 8) & 0xff);

        users.datapacket = new DatagramPacket(users.data, users.data.length, add, port);

        return users;
    }

    public ccpacket sendUsersForGui(String[] s, InetAddress add, int port) {

        ccpacket users = new ccpacket();

        users.data = new byte[2 + (s.length * usernameLength)];
        users.data[0] = (byte) (sendUserCodeForGui & 0xff);
        users.data[1] = (byte) ((sendUserCodeForGui >> 8) & 0xff);

        for (int i = 0; i < s.length; i++) {
            System.arraycopy(s[i].getBytes(), 0, users.data, 2 + (usernameLength * i), usernameLength);
        }

        users.datapacket = new DatagramPacket(users.data, users.data.length, add, port);

        return users;
    }

    public ccpacket getAllUsersForServers(InetAddress add, int port) {

        ccpacket request = new ccpacket();

        request.data = new byte[2];
        request.data[0] = (byte) (getAllUsersCode & 0xff);
        request.data[1] = (byte) ((getAllUsersCode >> 8) & 0xff);

        request.datapacket = new DatagramPacket(request.data, request.data.length, add, port);

        return request;
    }

    public ccpacket sendAllUsersForServers(String[] usernames, InetAddress add, int port) {
        ccpacket request = new ccpacket();

        request.data = new byte[2 + (usernameLength * usernames.length)];
        request.data[0] = (byte) (sendAllUsersCode & 0xff);
        request.data[1] = (byte) ((sendAllUsersCode >> 8) & 0xff);

        for (int i = 0; i < usernames.length; i++) {
            System.arraycopy(usernames[i].getBytes(), 0, request.data, (2 + (i * usernameLength)), usernameLength);
        }

        request.datapacket = new DatagramPacket(request.data, request.data.length, add, port);

        return request;
    }

    public ccpacket heatbeatRequest(InetAddress add, int port) {
        ccpacket hb = new ccpacket();

        hb.data = new byte[2];
        hb.data[0] = (byte) (heartbeatCode & 0xff);
        hb.data[1] = (byte) ((heartbeatCode >> 8) & 0xff);

        hb.datapacket = new DatagramPacket(hb.data, hb.data.length, add, port);

        return hb;
    }

    public ccpacket heatbeatSend(InetAddress add, int port) {
        ccpacket hb = new ccpacket();

        hb.data = new byte[2];
        hb.data[0] = (byte) (heartbeatSend & 0xff);
        hb.data[1] = (byte) ((heartbeatSend >> 8) & 0xff);

        hb.datapacket = new DatagramPacket(hb.data, hb.data.length, add, port);

        return hb;
    }

    public ccpacket requestChatForServer(InetAddress add, int port) {
        ccpacket chat = new ccpacket();

        chat.data = new byte[2];
        chat.data[0] = (byte) (requestChatForServerCode & 0xff);
        chat.data[1] = (byte) ((requestChatForServerCode >> 8) & 0xff);

        chat.datapacket = new DatagramPacket(chat.data, chat.data.length, add, port);

        return chat;
    }

    public ccpacket sendChatForServer(String[][] messages, InetAddress add, int port) {
        ccpacket chat = new ccpacket();

        chat.data = new byte[4 + ((usernameLength + messageLength) * messages[0].length)];
        chat.data[0] = (byte) (sendChatForServerCode & 0xff);
        chat.data[1] = (byte) ((sendChatForServerCode >> 8) & 0xff);

        short tempCode = (short) messages[0].length;

        chat.data[2] = (byte) (tempCode & 0xff);
        chat.data[3] = (byte) ((tempCode >> 8) & 0xff);

        for (int i = 0; i < messages[0].length; i++) {
            System.arraycopy(messages[0][i].getBytes(), 0, chat.data, ((i * usernameLength) + (i * messageLength) + 4), usernameLength);

            System.arraycopy(messages[1][i].getBytes(), 0, chat.data, (((i * usernameLength) + (i * messageLength) + 4) + usernameLength), messageLength);
        }

        chat.datapacket = new DatagramPacket(chat.data, chat.data.length, add, port);

        return chat;
    }

    public ccpacket updateChatForServer(String[][] messages, InetAddress add, int port) {
        ccpacket chat = new ccpacket();

        chat.data = new byte[4 + ((usernameLength + messageLength) * messages[0].length)];
        chat.data[0] = (byte) (updateChatForServerCode & 0xff);
        chat.data[1] = (byte) ((updateChatForServerCode >> 8) & 0xff);

        short tempCode = (short) messages[0].length;

        chat.data[2] = (byte) (tempCode & 0xff);
        chat.data[3] = (byte) ((tempCode >> 8) & 0xff);

        for (int i = 0; i < messages[0].length; i++) {
            System.arraycopy(messages[0][i].getBytes(), 0, chat.data, ((i * usernameLength) + (i * messageLength) + 4), usernameLength);
            System.arraycopy(messages[1][i].getBytes(), 0, chat.data, (((i * usernameLength) + (i * messageLength) + 4) + usernameLength), messageLength);
        }

        chat.datapacket = new DatagramPacket(chat.data, chat.data.length, add, port);

        return chat;
    }
}
