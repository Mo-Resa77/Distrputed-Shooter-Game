//6th class and final one ..............
package shooter;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Main game class that starts and manages game nodes Handles command line
 * arguments and user input
 */
public class MoResa_Shooter_Game {

    public static void main(String[] args) throws Exception {
        // Check if all required arguments are provided
        if (args.length < 4) {
            System.out.println("Usage: <id> <ip> <port> <service>");
            return;
        }

        // Parse command line arguments
        int id = Integer.parseInt(args[0]);      // Player ID (0-3)
        String ip = args[1];                     // IP address
        int port = Integer.parseInt(args[2]);    // Port number
        String service = args[3].toUpperCase();  // Player name (A,B,C,D)

        // Create node configuration map
        // Format: "service" -> "id:ip:port"
        Map<String, String> config = new HashMap<>();
        config.put("A", "0:127.0.0.1:2001");  // Player A configuration
        config.put("B", "1:127.0.0.1:3001");  // Player B configuration
        config.put("C", "2:127.0.0.1:4001");  // Player C configuration
        config.put("D", "3:127.0.0.1:5001");  // Player D configuration

        // Display startup information
        System.out.println("Starting node " + service + " (id=" + id + ", port=" + port + ")");

        try {
            // Create and initialize the game node
            Node node = new Node(id, ip, port, service, config);

            // Create RMI registry for this node
            Registry registry = LocateRegistry.createRegistry(port);

            // Register the node in the registry
            registry.rebind(service, node);

            // Display available commands
            System.out.println("\nNode " + service + " is running!");
            System.out.println("Commands:");
            System.out.println("  shoot <player>  - Shoot at another player (A,B,C,D)");
            System.out.println("  heal <player>   - Heal another player (A,B,C,D)");
            System.out.println("  move <direction> - Move (up,down,left,right)");
            System.out.println("  quit            - Exit the game");

            // Start command input loop
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("\nEnter command: ");
                String line = scanner.nextLine().trim();

                // Check for quit command
                if (line.equalsIgnoreCase("quit")) {
                    break;
                }

                // Parse command and target
                String[] parts = line.split(" ");  // Split input into command and target
                if (parts.length == 2) {  // Check if we have both command and target
                    String cmd = parts[0].toLowerCase();  // Get command (shoot, heal, move)
                    String target = parts[1];  // Get target (player or direction)

                    try {
                        // Process different commands
                        switch (cmd) {
                            case "shoot":
                                // Create and send shoot action
                                // Example: shoot B
                                // service = current player (e.g., "A")
                                // target = target player (e.g., "B")
                                // Action.SHOOT = action type 1
                                node.sendAction(new Action(service, target.toUpperCase(), Action.SHOOT));
                                break;

                            case "heal":
                                // Create and send heal action
                                // Example: heal C
                                // service = current player (e.g., "A")
                                // target = target player (e.g., "C")
                                // Action.HEAL = action type 2
                                node.sendAction(new Action(service, target.toUpperCase(), Action.HEAL));
                                break;

                            case "move":
                                // Create and send move action
                                // Example: move up
                                // service = current player (e.g., "A")
                                // target = direction (e.g., "up")
                                // Action.MOVE = action type 3
                                node.sendAction(new Action(service, target.toLowerCase(), Action.MOVE));
                                break;

                            default:
                                System.out.println("Unknown command: " + cmd);
                        }
                    } catch (Exception e) {
                        // Handle command execution errors
                        System.out.println("Error executing command: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    // Handle invalid command format
                    System.out.println("Invalid command format. Use: <command> <target>");
                }
            }

            // Cleanup and shutdown
            System.out.println("Shutting down node " + service);
            scanner.close();
            System.exit(0);

        } catch (Exception e) {
            // Handle node startup errors
            System.err.println("Error starting node: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
