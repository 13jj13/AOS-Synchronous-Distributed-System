/*
    Name: Jennifer Ward
    Project: 1
    Course: CS 6378.002 - Advanced Operating Systems
    Description: This program is used to handle the following tasks.
    Distributed system: This program takes a process/node ID and config file location as an input and is used
    to implement a distributed system consisting of n nodes, number 0 to n-1, arranged in a certain topology.
    A process/node can only exchange messages with its neighbors. All channels are bidirectional, reliable, and
    satisfy the FIFO property. Each channel is implemented using a reliable socket connection via SCTP. For each
    channel, the socket connection is created at the beginning of the program and stays intact until the end of
    the program. All messages between neighboring nodes are exchanged over these connections.
    Synchronizer: This program also implements a synchronizer to simulate a synchronous distributed system. All nodes execute
    a sequence of rounds. In each round, a node sends one message to each of its neighbors, then waits to receive one
    message from each of its neighbors sent in that round and then advances to the next round. Any message received
    from a future round is buffered until the node has moved to that round.
    Distributed Algorithm: The synchronizer is then used to determine the k-hop neighbors and eccentricity of each node.
    The eccentricity of a node is defined as the max distance between a node to all other nodes in the topology.
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.LinkedList;
import java.util.Scanner;

public class DistributedSystem
{

    // Holds all of the nodes and their information (i.e. nodeID, hostname, port number, etc.).
    public static Node[] allNodes = null;
    // Holds the neighbors of each node as a LinkedList.
    public static LinkedList<Integer>[] neighbors = null;

    public static void main(String[] args)
    {
        // Launcher script executes this program n times, one time for each node/process.
        // It will pass the process/node ID and config file location as commandline arguments to the program.
        try
        {
            // Check if no commandline argument was passed or more than two arguments were passed.
            if(args.length <= 0 || args.length > 2)
            {
                // Exit out of program.
                return;
            }

            // Otherwise, first commandline argument is nodeID.
            int nodeID = Integer.parseInt(args[0]);

            // Second commandline argument is config file location.
            String filename = args[1];

            // Read all info from config file.
            readConfigFile(filename);

            //System.out.println("Node: " + nodeID + " on machine " + allNodes[nodeID].hostName + " on port " + allNodes[nodeID].listeningPort);

            // Holds number of rounds used in the synchronizer for the distributed algorithm to calculate eccentricity.
            int numOfRounds = allNodes.length - 2;

            // Create instance of ClientServer - pass the node/process this program instance handles and the number of nodes
            // in the distributed system topology. ClientServer used to synchronize.
            SCTPClientServer cs = new SCTPClientServer(allNodes[nodeID], allNodes.length);

            // Create server - pass the ClientServer for synchronization, the node/process for this program instance,
            // and the number of rounds needed for the distributed algorithm to calculate the eccentricity.
            SCTPServer server = new SCTPServer(cs, allNodes[nodeID], numOfRounds);

            // Start server of this node.
            Thread serverThread = new Thread(server);
            serverThread.start();


            // Determine if neighbors are clients or servers.
            // If node for this program instance is client (i.e. client node id > neighbor node id), then create client
            // with neighbor server info and connect channel.
            // Iterate through all neighbors of node.
            for(int neighborID : neighbors[nodeID])
            {
                // If node for this program instance is the client.
                if(nodeID > neighborID)
                {
                    // Create client where nodeID is client, neighborID is server.
                    // Pass ClientServer for synchronization, the server and client node information, and number of rounds.
                    SCTPClient client = new SCTPClient(cs, allNodes[neighborID], allNodes[nodeID], numOfRounds);

                    // Connect client to server.
                    Thread clientThread = new Thread(client);
                    clientThread.start();
                }
            }

        }

        catch (Exception e)
        {
            System.out.println("Error occurred.");
            e.printStackTrace();
        }


    }

    /*
        Method: readConfigFile
        Description: Reads the config file and extracts the number of nodes in the distributed system, the node
            information (i.e. nodeID, hostname, listening port) for each node, and the neighbors for each node.
        Parameters: String filename of where config file is located.
        Returns: Nothing.
     */
    public static void readConfigFile(String filename)
    {
        try {
            // Path of config file.
            Path path = Paths.get(filename);

            // Maximum allowed size for the config file.
            long max_file_size = 100000;

            // Check if file is too large.
            if(Files.size(path) > max_file_size)
            {
                // Print error statement and end program.
                System.out.println("The config file is larger than 100kB, which is too large for this program.");

                return;
            }

            // Open and read in information from config file.
            File config_file_obj = new File(filename);
            Scanner config_reader = new Scanner(config_file_obj);

            // Holds count of number of valid lines in config file.
            int line_num = 0;

            // Holds number of nodes in distributed system.
            int n = 0;

            // Holds number of valid lines in config file.
            int max_line_num = 0;

            // Holds the index for allNodes array which corresponds to the nodeID
            int all_nodes_index = 0;

            // Holds the index for neighbors array which corresponds to the nodeID
            int neighbors_index = 0;

            // While the file is not empty
            while(config_reader.hasNextLine())
            {

                // Otherwise, read line, trim leading/trailing white space, and split around space delimiter/white space.
                String[] line = config_reader.nextLine().trim().split("\\s+");

                // Check that line is valid, i.e. first token of line is an unsigned integer.
                if(!line[0].matches("\\d+"))
                {
                    // Skip this line and go to next line in config file because first token is not an unsigned int.
                    continue;
                }

                // Increment number of valid lines.
                line_num++;

                // If first valid line in config file.
                if(line_num == 1)
                {
                    // Number of nodes in distributed system.
                    n = Integer.parseInt(line[0]);

                    // Number of nodes is now known so can create array of Nodes and array of Neighbors.
                    allNodes = new Node[n];
                    neighbors = new LinkedList[n];

                    // Total number of valid lines in config file which is 2n+1.
                    max_line_num = 2*n+1;
                }

                // Else, if the line is one of the next n lines in the config file where nodeID, hostname, and port is given.
                else if(line_num > 1 && line_num <= n+1)
                {
                    // Create a node with the information from the line in the config file and add to allNodes
                    // Information : nodeID, hostname, listening port
                    allNodes[all_nodes_index] =  new Node(Integer.parseInt(line[0]), line[1], Integer.parseInt(line[2]));

                    // Increment all nodes index to get the next node information.
                    all_nodes_index++;
                }

                // Else, if the line is one of the next n lines in the config file where neighbors of each node is given.
                else if(line_num > n+1 && line_num <= max_line_num)
                {
                    // Create new linked list for neighbors of this node.
                    neighbors[neighbors_index] = new LinkedList<>();

                    // Iterate through each neighbor to add for the node.
                    for(String id : line)
                    {
                        // Check that it is not the beginning of a comment.
                        if(id.equals("#"))
                        {
                            // If beginning of a comment, ignore the rest of the line.
                            break;
                        }
                        // Add neighbor for the node to the LinkedList for that node.
                        neighbors[neighbors_index].add(Integer.parseInt(id));
                    }

                    // Add neighbors to the information stored for this node.
                    allNodes[neighbors_index].addNeighbors(neighbors[neighbors_index]);

                    // Increment neighbors index for the next node.
                    neighbors_index++;
                }
            }

        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

    }

}
