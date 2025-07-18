// " third class "

/* This class manages player information:
Stores player position (x, y)
Tracks health (starts at 100)
Contains network information (ip, port)
Implements Serializable for RMI transmission */
package shooter;

import java.io.Serializable;

public class FighterData implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String ip;
    private int port;
    private String service;
    private int x, y;
    private int health = 100;

    public FighterData(int id, String ip, int port, String service, int x, int y) {
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.service = service;
        this.x = x;
        this.y = y;
    }

    public void updatePosition(String direction) {
        switch (direction.toLowerCase()) {  // Convert direction to lowercase
            case "up":
                if (y > 0) {
                    y--;  // Move up if not at top edge
                }
                break;
            case "down":
                if (y < 9) {
                    y++;  // Move down if not at bottom edge
                }
                break;
            case "left":
                if (x > 0) {
                    x--;  // Move left if not at left edge
                }
                break;
            case "right":
                if (x < 9) {
                    x++;  // Move right if not at right edge
                }
                break;
        }
        System.out.println("Position updated to (" + x + "," + y + ")");
    }

    public void updateHealth(int delta) {
        health = Math.max(0, Math.min(100, health + delta)); // عشان ديما اخرها 100
    }

    public int getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getService() {
        return service;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getHealth() {
        return health;
    }
}
