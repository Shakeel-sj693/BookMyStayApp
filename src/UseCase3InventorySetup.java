import java.util.HashMap;
import java.util.Map;

public class RoomInventory {
    private Map<String, Integer> inventory;

    public RoomInventory() {
        inventory = new HashMap<>();
    }

    public void addRoom(String roomType, int quantity) {
        inventory.put(roomType, inventory.getOrDefault(roomType, 0) + quantity);
    }

    public void removeRoom(String roomType, int quantity) {
        if (inventory.containsKey(roomType)) {
            int currentQuantity = inventory.get(roomType);
            if (currentQuantity > quantity) {
                inventory.put(roomType, currentQuantity - quantity);
            } else {
                inventory.remove(roomType);
            }
        }
    }

    public int getRoomCount(String roomType) {
        return inventory.getOrDefault(roomType, 0);
    }

    public Map<String, Integer> getAllRooms() {
        return inventory;
    }
}