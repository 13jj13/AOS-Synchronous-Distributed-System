
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

// Object to handle synchronization between nodes and each node's channels with other nodes.
public class SCTPClientServer {

    // Information known to the node (i.e. shared between all channels node is part of).

    // Variable to keep track of which round number the node is on.
    static private int currentRoundNumber = 0;
    // Holds number of nodes in topology.
    static private int numOfNodes;

    // Each ClientServer is associated with one node. NodeInfo is the node and its information.
    static Node nodeInfo;

    // Keeps track of messages sent from node and received by node for each round.
    static HashMap<Integer, Boolean> messagesSentThisRound;
    static HashMap<Integer, Boolean> messagesReceivedThisRound;

    // Max number of hops a node can take to reach all other nodes.
    static int maxHop = 0;

    // Keeps track of the k-hop neighbors of this node.
    // Max number of hops is n-1
    static LinkedList<Integer>[] kHopNeighbors;
    // khopneighbors[0] = node.neighbors or 1 hop neighbors
    // khopneighbors[1] = 2 hop neighbors
    // khopneighbors[2] = 3 hop neighbors
    // ... and so on

    // Keeps track of which nodes are in kHopNeighbors for this node.
    static boolean[] nodeCounted;

    // Constructor
    public SCTPClientServer(Node nodeInfo, int numOfNodes)
    {
        this.nodeInfo = nodeInfo;
        this.numOfNodes = numOfNodes;

        messagesSentThisRound = new HashMap<>();
        messagesReceivedThisRound = new HashMap<>();

        // Make mappings between neighbor ID keys and values (initialize to false)
        for(int neighborID : nodeInfo.neighbors)
        {
            messagesSentThisRound.put(neighborID, false);
            messagesReceivedThisRound.put(neighborID, false);
        }

        // Maximum number of hops that a node could have to farthest node is n-1
        maxHop = numOfNodes - 1;

        // Create array (of size of number of nodes in topology) of linkedlists
        kHopNeighbors = new LinkedList[maxHop];

        // Add 1-hop neighbors for this node - these are the neighbors of this node.
        kHopNeighbors[0] = nodeInfo.neighbors;
        for(int i = 1; i < maxHop; i++)
        {
            kHopNeighbors[i] = new LinkedList<>();
        }

        nodeCounted = new boolean[numOfNodes];
        // This node is already counted.
        nodeCounted[nodeInfo.nodeID] = true;
        // This node's neighbors are already counted - iterate through neighbor IDs.
        for(int neighborID : nodeInfo.neighbors)
        {
            // Filling 1-hop neighbors
            nodeCounted[neighborID] = true;
        }

    }

    /*
        Method: messageSent
        Description: Updates number of message sent this round for this node and checks if all messages have been
            sent and received for this round.
        Parameters: Integer destination nodeID for the sent message.
        Returns: Nothing
     */
    public synchronized void messageSent(int destNodeID) throws Exception
    {

        // Update that a message was sent to destination node ID for this round.
        messagesSentThisRound.replace(destNodeID, true);

        // Check if all messages have been sent and received this round for this node.
        if(isAllTrue(messagesReceivedThisRound) && isAllTrue(messagesSentThisRound))
        {
            // Go to the next round for this node.
            goToNextRound();
        }

    }


    /*
        Method: messageReceived
        Description: Updates number of messages received this round by this node, updates k-hop neighbors for the node,
            and checks if all messages have been sent and received for this node for this round.
        Parameters: Integer source node ID of node that sent message and the received message.
        Returns: Nothing
     */
    public synchronized void messageReceived(int sourceNodeID, Message receivedMessage) throws Exception
    {

        // Update that a message was received from this source node ID for this round.
        messagesReceivedThisRound.replace(sourceNodeID, true);

        // For each k-hop neighbor node ID of the source node ID (i.e. node that this message was received from).
        for(int msgNeighborID : receivedMessage.kHopNeighbors[currentRoundNumber])
        {
            // Compare each of those node IDs with list of received neighbors - if false, add to khopneighbors[round+1] list
            // If k-hop neighbor node ID of the source node has not been reached already.
            if(!nodeCounted[msgNeighborID])
            {
                // Add this node to the k-hop neighbor for this node.
                kHopNeighbors[currentRoundNumber+1].add(msgNeighborID);
                // Mark that this node ID has been counted for this node now.
                nodeCounted[msgNeighborID] = true;
            }
        }

        // Check if all messages have been sent and received this round for this node.
        if(isAllTrue(messagesReceivedThisRound) && isAllTrue(messagesSentThisRound))
        {
            // Go to the next round for this node.
            goToNextRound();
        }


    }

    /*
        Method: bufferMessage
        Description: Tells thread channel to wait if message received from future round until the node reaches that round.
        Parameters: None
        Returns: Nothing
     */
    public synchronized void bufferMessage() throws Exception
    {

        System.out.println("BUFFER: Message from future round received. Buffering message.");
        // Wait until node moves to next round.
        wait();

    }

    /*
        Method: getCurrentRoundNumber
        Description: Returns the current round number for the node.
        Parameters: None
        Returns: Integer current round number for the node.
     */
    public synchronized int getCurrentRoundNumber()
    {
        return currentRoundNumber;
    }

    /*
        Method: getKHopNeighbors
        Description: Returns the node's current list array of k-hop neighbors.
        Parameters: None
        Returns: List array of k-hop neighbors.
     */
    public synchronized LinkedList[] getKHopNeighbors()
    {
        return kHopNeighbors;
    }

    /*
        Method: isAllTrue
        Description: Determines if all the values for the hashmap are true.
        Parameters: Hashmap to check.
        Returns: Boolean - true if all values are true, false if at least one value is false.
     */
    private boolean isAllTrue(HashMap<Integer, Boolean> messages)
    {
        // Iterate through each value for each key in the map.
        for(boolean value : messages.values())
        {
            // If a value is false, then return false.
            if(!value)
            {
                return false;
            }
        }
        // If all values are true, then return true.
        return true;
    }

    /*
        Method: goToNextRound
        Description: Move node to next round unless all k-hop neighbors and eccentricity has been found for the node.
        Parameters: None
        Returns: Nothing
     */
    public void goToNextRound() throws IOException {
        System.out.println("ROUND DONE: All messages sent and received for node " + nodeInfo.nodeID + " at round " + currentRoundNumber);
        System.out.println();

        // If current round number equals the max number of rounds (i.e. maxHop-2)
        if(currentRoundNumber == maxHop-2)
        {
            // Display the node, k-hop neighbors of the node, and the eccentricity of the node.
            // Write those displayed values to an output file as well - config-nodeID.txt
            String filename = "config-" + nodeInfo.nodeID + ".txt";
            String filepath = "Documents/AOS/Projects/Project1/";

            //check for write-ability and open output file
            File outFile = new File(filepath + filename);
            PrintWriter output = new PrintWriter(outFile);

            if(outFile.canWrite()) {

                System.out.println("Output files stored at: " + outFile.getAbsolutePath());

                // Node ID
                String printline = "Node " + nodeInfo.nodeID;
                System.out.println(printline);
                output.write(printline + "\n");
                int eccentricity = 0;

                // Loop through each k-hop neighbor list
                for (int i = 0; i < maxHop; i++) {
                    // Print all k-hop neighbor lists for the node.
                    printline = i + 1 + "-hop neighbors --> " + kHopNeighbors[i];
                    System.out.println(printline);
                    output.write(printline + "\n");
                    // Determine the eccentricity - it will be the last non-empty list of the k-hop neighbors.
                    if (!kHopNeighbors[i].isEmpty()) {
                        eccentricity = i + 1;
                    }
                }

                // Display eccentricity of the node.
                printline = "Eccentricity: " + eccentricity;
                System.out.println(printline);
                output.write(printline + "\n");

                output.close();
            }

            // Do not move to next round - return.
            return;
        }

        // Move to next round.
        currentRoundNumber++;
        // Reset messages received and sent for new round with a value of false.
        messagesReceivedThisRound.replaceAll((key, value) -> value = false);
        messagesSentThisRound.replaceAll((key, value) -> value = false);
        System.out.println("\n\nNEW ROUND: Node " + nodeInfo.nodeID + " moved to round " + currentRoundNumber);
        // Notify all threads associated with this node that they are starting a new round. Any buffered messages will now
        // be processed.
        notifyAll();
        System.out.println("NOTIFIED: All threads notified of new round.");


    }



}
