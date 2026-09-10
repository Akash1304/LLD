package hotel.service;

import hotel.model.Room;
import hotel.model.RoomType;

import java.util.List;
import java.util.Optional;

public interface RoomInventoryService {
    Room addRoom(Room room);
    List<Room> getRoomsByType(RoomType type);
    Optional<Room> getRoom(String roomId);
}
