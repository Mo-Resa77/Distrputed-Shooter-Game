// " fifth class " 
package shooter;

import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;


/*This is the main class that implements the game:
Manages player data
Processes game actions
Handles RMI communication
Implements game rules*/
public class Node extends UnicastRemoteObject /*export for stub*/ implements NodeI {

    private static final long serialVersionUID = 1L;

    private int id, port, lClock = 0; // for starting temp value
    private String ip, service;
    private boolean isHost = false;
    private int leaderId = -1;

    private Map<String, FighterData> players /*ركز*/ = new ConcurrentHashMap<>();
    private Map<String, Integer> lastValidHealth = new ConcurrentHashMap<>();
    private PriorityQueue<Action> actionQueue = new PriorityQueue<>();
    private Map<String, String> nodeConfigs;
// Stores all players in the game
// Key: String - Player's service name (e.g., "A", "B", "C", "D")
// Value: FighterData - Player's information including:
//   - id: Player's unique ID
//   - ip: Player's IP address
//   - port: Player's port number
//   - service: Player's service name
//   - x, y: Player's position on grid
//   - health: Player's current health
// ConcurrentHashMap is used for thread-safe operations
    ///////////////////////////////////////////////////////////////

    // Tracks the last known valid health of each player
// Key: String - Player's service name (e.g., "A", "B", "C", "D")
// Value: Integer - Player's health value (0-100)
// Used to maintain consistent health values across nodes
// ConcurrentHashMap ensures thread-safe health updates
//    /////////////////////////////////////////////////////////////
    // Stores pending game actions in order of execution
// Elements: Action objects containing:
//   - actionId: Unique identifier for the action
//   - sender: Who performed the action
//   - target: Who is targeted
//   - actionType: SHOOT(1), HEAL(2), or MOVE(3)
//   - logicalClock: For ordering actions
//   - pid: Process ID of sender
//   - receiptTime: When action was received
// Actions are ordered by logical clock, receipt time, and pid
//    /////////////////////////////////////////////////////////////////////
    // Stores network configuration for all nodes
// Key: String - Node's service name (e.g., "A", "B", "C", "D")
// Value: String - Node's configuration in format "id:ip:port"
// Example values:
//   "A" -> "0:127.0.0.1:2001"
//   "B" -> "1:127.0.0.1:3001"
//   "C" -> "2:127.0.0.1:4001"
//   "D" -> "3:127.0.0.1:5001"
// Used for node communication and leader election
    ///////////////////////////////////////////////////////////////////////
    public Node(int id, String ip, int port, String service, Map<String, String> nodeConfigs) throws RemoteException {
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.service = service;
        this.nodeConfigs = nodeConfigs;
        this.leaderId = 0;
        this.isHost = (id == 0); // node 0 is the host 

        // object of fighter
        // First, each Node creates itself as a player
        FighterData self = new FighterData(id, ip, port, service, (int) (Math.random() * 10), (int) (Math.random() * 10)); // postions randomly on grid //
        players.put(service, self);
        lastValidHealth.put(service, 100);

        System.out.println("=== Node Initialization ===");
        System.out.println("Service: " + service);
        System.out.println("Is Host: " + isHost);
        System.out.println("Position: (" + self.getX() + "," + self.getY() + ")");
        System.out.println("Health: " + self.getHealth());

        // الهوست فقط اللي يقدر يعمل ده 
        // If this Node is the host (id == 0), it creates all other players
        if (isHost) {
            System.out.println("\n=== Host Initializing Players ===");
            // Position players close together (all within 3 units of each other)
            players.put("A", new FighterData(0, "127.0.0.1", 2001, "A", 1, 1));  // A at (1,1)
            players.put("B", new FighterData(1, "127.0.0.1", 3001, "B", 1, 2));  // B at (1,2)
            players.put("C", new FighterData(2, "127.0.0.1", 4001, "C", 2, 2));  // C at (2,2)
            players.put("D", new FighterData(3, "127.0.0.1", 5001, "D", 2, 1));  // D at (2,1)

            /*
            
            Creates a 10x10 grid
            Places players at their positions
            Shows empty spaces as dots
            Displays player positions separately
            Sets initial health to 100
            
             */
            System.out.println("\nGame Grid (10x10):");
            System.out.println("  0 1 2 3 4 5 6 7 8 9");
            for (int y = 0; y < 10; y++) {
                System.out.print(y + " ");
                for (int x = 0; x < 10; x++) {
                    boolean found = false;
                    for (FighterData player : players.values()) {
                        if (player.getX() == x && player.getY() == y) {
                            System.out.print(player.getService() + " ");
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        System.out.print(". ");
                    }
                }
                System.out.println();
            }

            for (String key : players.keySet()) { // key is the node char Set health to 100 , Get Player A's data and so on 
                lastValidHealth.put(key, 100);
                FighterData player = players.get(key);
                System.out.println("Player " + key + " at (" + player.getX() + "," + player.getY() + ")");
            }
        }

        System.out.println("\n�� Current leader is: " + getLeaderService());
        new Thread(this::checkLeaderAlive).start();

        /*Checks if the leader (host) is still alive
Runs every 5 seconds
Starts new leader election if current leader fails
Ensures fault tolerance in the game */
    }

    public int getPort() {
        return port;
    }

    public String getService() {
        return service;
    }

    public int getLogicalClock() {
        return lClock;
    }

    public int getId() {
        return id;
    }

    //_________________________________________________________________________________________________________________________________________________________________
    // Action Processing
    @Override     // بتتنفد عن السرفر الليدر ريموتلي     ----all actions sended encrypted 
    public void sendAction(Action action) throws RemoteException {
        lClock++;  // Increment logical clock
        action.logicalClock = lClock;  // Set action's clock
        action.pid = this.id;  // Set process ID

        try {
            System.out.println("📤 Sending action: " + action);
            if (action.actionType == Action.MOVE) {
                // Handle MOVE action
                if (isHost) {
                    // If this node is host, process directly
                    actionQueue.add(action);
                    processActions();
                } else {
                    // If not host, send to leader
                    NodeI leader = getNode(getLeaderService());
                    String encryptedAction = encryptAction(action);
                    leader.receiveAction(encryptedAction);
                }
            } else {
                // Handle SHOOT or HEAL actions
                String encryptedAction = encryptAction(action);
                if (isHost) {
                    // If host, multicast to all
                    multicastAction(encryptedAction);
                } else {
                    // If not host, send to leader to multicast
                    NodeI leader = getNode(getLeaderService());
                    leader.multicastAction(encryptedAction);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error in sendAction: " + e.getMessage());
            startElection();
        }
    }

    @Override
    public void multicastAction(String encryptedAction) throws RemoteException {
        try {
            // 1. Decrypt the received encrypted action
            Action action = decryptAction(encryptedAction);

            // 2. Add the decrypted action to the local action queue
            actionQueue.add(action);
            System.out.println("📥 Action multicast received and added: " + action);

            // 3. Broadcast the action to all other nodes
            for (String nodeId : nodeConfigs.keySet()) {
                // Skip sending to self to avoid infinite loop  ----> مش منطقي اعمل ملتي كاست لنفسي 
                if (!nodeId.equals(this.service)) {
                    try {
                        // Get reference to the target node
                        NodeI node = getNode(nodeId);
                        if (node != null) {
                            // Send the encrypted action to the node
                            node.receiveAction(encryptedAction);
                        }
                    } catch (Exception ignored) {
                        // Ignore errors for individual nodes
                        // This allows the multicast to continue even if one node fails
                    }
                }
            }

            // 4. Process the actions in the queue
            processActions();

        } catch (Exception e) {
            // Handle any errors in the multicast process
            System.err.println("❌ Error in multicastAction: " + e.getMessage());
        }
    }

    @Override
    public void receiveAction(String encryptedAction) throws RemoteException {
        try {
            Action action = decryptAction(encryptedAction);
            if (!actionQueue.contains(action)) {
                actionQueue.add(action);
                System.out.println("📥 Action received and queued: " + action);
            }
            processActions();
        } catch (Exception e) {
            System.err.println("❌ Error in receiveAction: " + e.getMessage());
        }
    }

    //__________________________________________________________________________________________________________________________________________________________-
    // Game Logic
    private void processActions() {
        if (!isHost) {
            System.out.println("⚠ Not processing actions - this node is not the host");
            return;
        }

        System.out.println("\n=== Processing Actions ===");
        System.out.println("Actions in queue: " + actionQueue.size());

        while (!actionQueue.isEmpty()) {
            Action action = actionQueue.poll();
            System.out.println("\n🎯 Processing: " + action.actionType
                    + " from " + action.sender
                    + " to " + action.target);  //Processes actions in order
            //Shows action details (type, sender, target 

            FighterData sender = players.get(action.sender);
            FighterData target = players.get(action.target);

            if (sender == null) {
                System.out.println("❌ Sender not found: " + action.sender);
                continue;
            }
            if (target == null && action.actionType != Action.MOVE) {
                System.out.println("❌ Target not found: " + action.target);
                continue;
            }                       //Checks if players exist ممتش
            //MOVE actions don't need target validatio

            if (action.actionType == Action.MOVE) {
                sender.updatePosition(action.target);
                System.out.println("✅ " + sender.getService()
                        + " moved to " + action.target
                        + " at (" + sender.getX() + "," + sender.getY() + ")");

                // Show updated map after movement
                System.out.println("\nUpdated Game Grid:");
                System.out.println("  0 1 2 3 4 5 6 7 8 9");
                for (int y = 0; y < 10; y++) {
                    System.out.print(y + " ");
                    for (int x = 0; x < 10; x++) {
                        boolean found = false;
                        for (FighterData player : players.values()) {
                            if (player.getX() == x && player.getY() == y) {
                                System.out.print(player.getService() + " ");
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            System.out.print(". ");
                        }
                    }
                    System.out.println();
                }  // Updates player position
                //   Shows new grid layou

            } else {
                double distance = Math.sqrt(
                        Math.pow(sender.getX() - target.getX(), 2)
                        + Math.pow(sender.getY() - target.getY(), 2)
                );
                /*  If Player A is at (1,1) and Player B is at (1,2):
                         distance = √((1-1)² + (1-2)²)
                         distance = √(0 + 1)
                         distance = 1.0 */

                System.out.println("📏 Distance: " + String.format("%.2f", distance));

                if (distance <= 3) {  // If target is within 3 units
                    // Determine if shooting (-10) or healing (+10)
                    int delta = action.actionType == Action.SHOOT ? -10 : 10;

                    // Get current health before change
                    int oldHealth = target.getHealth();

                    // Update health
                    target.updateHealth(delta);
                    System.out.println("✅ " + action.actionType + " successful!"
                            + "\n   " + target.getService() + " health: "
                            + oldHealth + " → " + target.getHealth());

                    lastValidHealth.put(target.getService(), target.getHealth());

                    //Shows health change
                    //Updates health tracking map
                    if (target.getHealth() <= 0) {
                        System.out.println("💀 " + target.getService() + " has been eliminated (killed)!");
                        players.remove(target.getService());

                        // Check for game over
                        if (players.size() <= 1) {
                            System.out.println("\n🏆 Game Over! Winner: "
                                    + players.keySet().iterator().next());
                        }
                    }
                } else {
                    System.out.println("❌ Action failed: Target out of range (max range: 3)");
                }
            }
        }
    }

    //_______________________________________________________________________________________________________________________________________________ 
    /**
     * Continuously monitors the leader's status Runs in a separate thread to
     * check if leader is alive If leader dies, starts a new leader election
     */
    private void checkLeaderAlive() {
        // Run forever to continuously monitor leader
        while (true) {
            try {
                // Wait 5 seconds between checks
                // This prevents too frequent checking
                Thread.sleep(5000);

                // Only non-host nodes need to check leader
                // Host node is the leader itself
                if (!isHost) {
                    // Try to get reference to current leader بوليمرفيزم
                    NodeI leader = getNode(getLeaderService());

                    // If leader is not responding (null)
                    // This means leader has died or is unreachable
                    if (leader == null) {
                        // Start new leader election process
                        startElection();
                    }
                }
            } catch (Exception e) {
                // If any error occurs during check:
                // - Network error
                // - Timeout
                // - Connection refused
                // Start new leader election
                startElection();
            }
        }
    }

    /**
     * Starts a new leader election process Finds the node with highest ID to be
     * the new leader If this node has highest ID, it becomes the leader
     */
    private void startElection() {
        // Log election start
        System.out.println("🔁 Election started...");

        // Start with this node's ID as maximum
        int maxId = this.id;

        // Check all nodes in the configuration
        for (String entry : nodeConfigs.values()) {
            // Parse node information
            // Format: "id:ip:port"
            String[] parts = entry.split(":");
            int otherId = Integer.parseInt(parts[0]);

            // If found a node with higher ID
            if (otherId > this.id) {
                try {
                    // Try to contact that node
                    NodeI node = getNodeById(otherId);
                    if (node != null) {
                        // If node is alive, it should be leader
                        System.out.println("⚖ Higher ID node found: " + otherId);
                        return;  // Exit election
                    }
                } catch (Exception ignored) {
                    // If node is dead, continue checking others
                }
            }
        }

        // If no higher ID node is alive
        // This node becomes the leader
        this.isHost = true;
        this.leaderId = this.id;
        System.out.println("👑 I am the new host.");
    }

//___________________________________________________________________________________________________________________________________________
    /**
     * Node Communication Methods These methods handle finding and connecting to
     * other nodes in the game
     */
    /**
     * Gets a reference to a remote node using its service name Used to
     * establish RMI connection with other nodes
     *
     * @param serviceName The service name of the node (A, B, C, D)
     * @return NodeI reference to the remote node, or null if not found
     */

    /*Purpose: Connects to another player in the game
What it does:
Takes a player's name (A, B, C, D)
Finds their IP and port
Creates a connection to that player */
    private NodeI getNode(String serviceName) {
        try {
            // Get node configuration from map
            // Format: "id:ip:port" (e.g., "0:127.0.0.1:2001")
            String[] parts = nodeConfigs.get(serviceName).split(":");

            // Create registry connection to the node's location
            Registry registry = LocateRegistry.getRegistry(
                    parts[1], // IP address (e.g., "127.0.0.1")
                    Integer.parseInt(parts[2]) // Port number (e.g., 2001)
            );

            // Look up the node in the registry by service name
            return (NodeI) registry.lookup(serviceName);
        } catch (Exception e) {
            // Log error if node not found or connection fails
            System.err.println("⚠ Failed to get node " + serviceName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Finds a node by its ID number Used during leader election to find nodes
     * with higher IDs
     *
     * @param searchId The ID to search for (0, 1, 2, 3)
     * @return NodeI reference to the remote node, or null if not found
     */

    /* Purpose: Finds a player by their ID number
What it does:
Takes a player ID (0, 1, 2, 3)
Finds which player has that ID
Connects to that player */
    private NodeI getNodeById(int searchId) {
        // Search through all node configurations
        for (Map.Entry<String, String> entry : nodeConfigs.entrySet()) {
            // Parse node information from configuration
            String[] parts = entry.getValue().split(":");
            if (Integer.parseInt(parts[0]) == searchId) {
                // If ID matches, get node reference using service name
                return getNode(entry.getKey());
            }
        }
        return null;
    }

    /**
     * Gets the service name of the current leader Used to find which node is
     * currently the leader
     *
     * @return Service name of the leader (A, B, C, D), or null if not found
     */

    /* Purpose: Finds who is the current leader
What it does:
Looks at the leader ID
Finds which player has that ID
Returns that player's name */
    private String getLeaderService() {
        // Search through all node configurations
        for (Map.Entry<String, String> entry : nodeConfigs.entrySet()) {
            // Parse node information
            if (Integer.parseInt(entry.getValue().split(":")[0]) == leaderId) {
                // If ID matches leaderId, return service name
                return entry.getKey();
            }
        }
        return null;
    }

//____________________________________________________________________________________________________________________________________________
    /**
     * Encrypts an Action object for secure transmission over the network
     * Process: 1. Convert Action object to bytes 2. Convert bytes to Base64
     * string 3. Encrypt the Base64 string
     *
     * @param action The game action to encrypt
     * @return Encrypted string of the action
     * @throws Exception If encryption fails
     */
    private String encryptAction(Action action) throws Exception {
        // Create a byte array output stream to hold the serialized action
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        // Create an object output stream to write the Action object
        ObjectOutputStream out = new ObjectOutputStream(bos);

        // Write the Action object to bytes
        out.writeObject(action);
        out.flush();

        // Convert the bytes to Base64 string and encrypt it
        return Encryptor.encrypt(Base64.getEncoder().encodeToString(bos.toByteArray()));
    }

    /**
     * Decrypts an encrypted Action string back to an Action object Process: 1.
     * Decrypt the encrypted string 2. Convert Base64 string back to bytes 3.
     * Convert bytes back to Action object
     *
     * @param encryptedAction The encrypted action string
     * @return The decrypted Action object
     * @throws Exception If decryption fails
     */
    private Action decryptAction(String encryptedAction) throws Exception {
        // Decrypt the encrypted string using the Encryptor
        String decrypted = Encryptor.decrypt(encryptedAction);

        // Convert the decrypted Base64 string back to bytes
        byte[] bytes = Base64.getDecoder().decode(decrypted);

        // Create an input stream from the bytes
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));

        // Read and return the Action object
        return (Action) in.readObject();
    }

}
