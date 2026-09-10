package hotel.service;

import hotel.model.Room;
import hotel.model.RoomType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class InMemoryRoomInventoryService implements RoomInventoryService {
    private final Map<String, Room> rooms = new LinkedHashMap<>();

    @Override
    public Room addRoom(Room room) {
        rooms.put(room.getId(), room);
        return room;
    }

    @Override
    public List<Room> getRoomsByType(RoomType type) {
        return rooms.values().stream().filter(r -> r.getType() == type).collect(Collectors.toList());
    }

    @Override
    public Optional<Room> getRoom(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }
}
