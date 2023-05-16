package S4;

import java.net.Socket;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.PriorityQueue;
import java.io.IOException;
import java.time.Instant;

public class S4Helper implements Runnable, Comparable<S4Helper> {

    public static final String serverName = "SERVER4";
    public static final String Quorum = " SERVER1 SERVER2"; // needs to forward related messages only to SERVER2
    public static ArrayList<S4Helper> serverThreads = new ArrayList<S4Helper>();
    public static ArrayList<S4Helper> clientThreads = new ArrayList<S4Helper>();
    public static PriorityQueue<S4Helper> clientWaitList = new PriorityQueue<S4Helper>();
    public static Object lock;
    public static BufferedWriter toS0;
    public static int receivedReplies = 0;// Only used to count Replies
    public static boolean locked = false;// True:critical section, processing client request, or sent reply
    public static String lockedBy = "";
    public CountDownLatch latch;
    private Socket socket;
    private String name;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private Queue<String> messageWaitlist = new LinkedList<String>();// Unread messages
    private Instant requestTime;// Only for clients to be sorted in the Priority Queue

    public int compareTo(S4Helper other) {
        return this.requestTime.compareTo(other.requestTime);
    }

    public S4Helper(Socket connection, BufferedWriter xS0, CountDownLatch xlatch, Object xlock) {
        try {
            this.socket = connection;
            if (toS0 == null && lock == null)// only needed once
            {
                toS0 = xS0;
                lock = xlock;
            }
            this.latch = xlatch;
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            writeToConnection(serverName); // Share name to connection
            this.name = bufferedReader.readLine();
            if (this.name.contains("SERVER")) {
                serverThreads.add(this);
            } else if (this.name.contains("CLIENT")) {
                clientThreads.add(this);
            } else {
                this.name = "CONNECTION TO DELETE";
                clientThreads.add(this);
                System.out.println("Connection name is not a SERVER or a CLIENT");
                closeEverything();
            }
            System.out.println(name + " has connected.");
        } catch (IOException e) {
            System.out.println("ERROR adding a connection");
            closeEverything();
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            this.latch.await();
        } catch (InterruptedException e) {
        }

        String incoming = "";
        String outgoing = "";
        String log = "";
        String[] splitter;
        while (this.socket.isConnected()) {
            try {
                if (!messageWaitlist.isEmpty() && !locked) {
                    incoming = messageWaitlist.remove();
                } else {
                    incoming = bufferedReader.readLine();
                }
                System.out.println(incoming + " from " + name+".");
                //System.out.println("locked="+locked+" locked by="+lockedBy+" clientWaitlist="+clientWaitList.size()+" messageWaitList="+messageWaitlist.size());
                // Client Connection
                if (this.name.contains("CLIENT")) {
                    if (incoming.contains("RELEASE")) {
                        log = "RECEIVED " + incoming + " from " + this.name;
                        writeToS0(log);
                        if (!lockedBy.contains("GRANT")) {
                            synchronized (lock) {
                                clientWaitList.removeIf(x -> x.name.contains(this.name));
                                if (lockedBy.contains(this.name) || lockedBy.contains("SERVER")) {
                                    System.out.println("Unlocked "+lockedBy);
                                    lockedBy = "";
                                    locked = false;
                                    receivedReplies=0;
                                }
                            }
                            // If this server sent the GRANT message
                        } else {
                            while (!clientWaitList.isEmpty() && (clientWaitList.peek().socket == null
                                    || !clientWaitList.peek().socket.isConnected())) {
                                synchronized (lock) {
                                    clientWaitList.poll(); // Remove disconnected clients at the head of queue
                                }
                            }
                            // Immediatly send out the next request that was queued
                            if (!clientWaitList.isEmpty()) {
                                synchronized (lock) {
                                    locked = true;
                                    lockedBy = clientWaitList.peek().name + "REQUEST";
                                    receivedReplies = 0;
                                    System.out.println("Locked by: " + lockedBy);
                                    for (S4Helper x : serverThreads) {
                                        // To Parent
                                        if (x.name.contains("SERVER2")) {
                                            outgoing = serverName + ": REQUEST" + Quorum;
                                            x.writeToConnection(outgoing);
                                            log = "SENT " + outgoing + " to " + x.name;
                                            writeToS0(log);
                                        } else {
                                            System.out.println("ERROR in logic at leaf: more than 1 server connected");
                                        }
                                    }
                                }
                            }else {
                                synchronized (lock) {
                                    lockedBy = "";
                                    locked = false;
                                }
                            }
                        }
                    } else if (incoming.contains("REQUEST")) {
                        log = "RECEIVED " + incoming + " from " + this.name;
                        writeToS0(log);
                        splitter = incoming.split("#", 2);
                        this.requestTime = Instant.parse(splitter[1]);
                        if (clientWaitList.isEmpty() && !locked) {
                            synchronized (lock) {
                                locked = true;
                                lockedBy = this.name + "REQUEST";
                                receivedReplies = 0;
                                System.out.println("Locked by: " + lockedBy);
                                for (S4Helper x : serverThreads) {
                                    // To Parent
                                    if (x.name.contains("SERVER2")) {
                                        outgoing = serverName + ": REQUEST" + Quorum;
                                        x.writeToConnection(outgoing);
                                        log = "SENT " + outgoing + " to " + x.name;
                                        writeToS0(log);
                                    } else {
                                        System.out.println("ERROR in logic at leaf: more than 1 server connected");
                                    }
                                }
                            }
                        }
                        synchronized (lock) {
                            clientWaitList.add(this);
                        }
                    }
                }
                // Server Connection
                else if (this.name.contains("SERVER")) {
                    if (incoming.contains("REQUEST")) {
                        log = "RECEIVED " + incoming + " from " + this.name;
                        writeToS0(log);
                        splitter = incoming.split(":", 2);
                        boolean skip = false;
                        // if request contains this node
                        if (splitter[1].contains(serverName)) {
                            if (locked) {
                                messageWaitlist.add(incoming);
                                skip = true;
                            } else {
                                // generating reply
                                incoming = incoming.replace(" " + serverName, "");
                                splitter = incoming.split(":", 2);
                                synchronized (lock) {
                                    locked = true;
                                    outgoing = "REPLY TO " + splitter[0];
                                    lockedBy = outgoing;
                                    receivedReplies = 0;
                                    System.out.println("Locked by: " + lockedBy);
                                    for (S4Helper x : serverThreads) {
                                        // parent
                                        if (x.name.contains("SERVER2")) {
                                            x.writeToConnection(outgoing);
                                            log = "SENT " + outgoing + " to " + x.name;
                                            writeToS0(log);
                                        } else {
                                            System.out.println("ERROR in logic at leaf: more than 1 server connected");
                                        }
                                    }
                                }
                            }
                        }
                        // if request contains more
                        if (!skip && splitter[1].contains("SERVER")) {
                            System.out.println("ERROR in logic at leaf: recieved a request not for leaf");
                        }
                    }
                    if (incoming.contains("REPLY")) {
                        log = "RECEIVED " + incoming + " from " + this.name;
                        writeToS0(log);
                        // for this server
                        if (incoming.contains(serverName)) {
                            synchronized (lock) {
                                if (!locked)
                                    System.out.println("Deadlock?");
                                receivedReplies++;
                                if (receivedReplies == 2) {
                                    receivedReplies = 0;
                                    locked = true;
                                    lockedBy = clientWaitList.peek().name + " GRANT";
                                    System.out.println("Locked by:" + lockedBy);
                                    outgoing = "GRANT";
                                    clientWaitList.peek().writeToConnection(outgoing);
                                    log = "SENT " + outgoing + " to " + clientWaitList.peek().name;
                                    writeToS0(log);
                                    clientWaitList.remove();
                                }
                            }
                            // Propogate reply to the left
                        } else {
                            System.out.println("ERROR in logic at leaf: Should not be recieving replies to propogate");

                        }
                    }
                } else {
                    System.out.println("Connection is not valid");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeToS0(String message) {
        try {
            synchronized (lock) {
                System.out.println("ToS0:"+message + ".");
                //System.out.println("locked="+locked+" locked by="+lockedBy+" clientWaitlist="+clientWaitList.size()+" messageWaitList="+messageWaitlist.size());
                System.out.flush();
                toS0.write(message);
                toS0.newLine();
                toS0.flush();
            }
        } catch (IOException e) {
            System.out.println("ERROR wrtiting to S0");
        }
    }

    public void writeToConnection(String message) {
        try {
            System.out.println("Sending:"+message+" to "+this.name+".");
            System.out.flush();
            this.bufferedWriter.write(message);
            this.bufferedWriter.newLine();
            this.bufferedWriter.flush();
        } catch (IOException e) {
            System.out.println("ERROR wrtiting to connection");
        }
    }

    public void closeEverything() {
        if (this.name.contains("SERVER")) {
            serverThreads.remove(this);
        } else if (this.name.contains("CLIENT")) {
            clientThreads.remove(this);
        }
        try {
            if (this.bufferedReader != null) {
                this.bufferedReader.close();
            }
            if (this.bufferedWriter != null) {
                this.bufferedWriter.close();
            }
            if (this.socket != null) {
                socket.close();
            }
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.out.println("ERROR with ending connection");
            e.printStackTrace();
        }
    }
}
