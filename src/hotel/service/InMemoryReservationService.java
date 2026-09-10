package hotel.service;

import hotel.model.Guest;
import hotel.model.Reservation;
import hotel.model.ReservationStatus;
import hotel.model.Room;
import hotel.model.RoomType;
import hotel.strategy.PricingStrategy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryReservationService implements ReservationService {
    private final Map<String, Reservation> reservations = new ConcurrentHashMap<>();
    private final Map<String, List<Reservation>> activeByRoom = new ConcurrentHashMap<>();
    private final RoomInventoryService roomInventoryService;
    private final AtomicLong idCounter = new AtomicLong(1);
    // Only reservations on the SAME room can overlap in date, so the
    // natural lock granularity for the check-then-act in bookRoom (scan
    // this room's reservations for a date conflict, then commit a new
    // one) is "per room" -- not a lock across the whole hotel, which
    // would serialize bookings for unrelated rooms/room types.
    private final Map<String, Object> roomLocks = new ConcurrentHashMap<>();

    public InMemoryReservationService(RoomInventoryService roomInventoryService) {
        this.roomInventoryService = roomInventoryService;
    }

    private Object lockFor(String roomId) {
        return roomLocks.computeIfAbsent(roomId, k -> new Object());
    }

    @Override
    public Reservation bookRoom(Guest guest, RoomType type, LocalDate checkIn, LocalDate checkOut, PricingStrategy pricingStrategy) throws NoRoomAvailableException {
        for (Room room : roomInventoryService.getRoomsByType(type)) {
            synchronized (lockFor(room.getId())) {
                if (isRoomFree(room.getId(), checkIn, checkOut)) {
                    double price = pricingStrategy.calculatePrice(type, checkIn, checkOut);
                    Reservation reservation = Reservation.builder()
                            .id("RES-" + idCounter.getAndIncrement())
                            .guest(guest).room(room)
                            .checkIn(checkIn).checkOut(checkOut)
                            .totalPrice(price)
                            .build();
                    reservations.put(reservation.getId(), reservation);
                    activeByRoom.computeIfAbsent(room.getId(), k -> new CopyOnWriteArrayList<>()).add(reservation);
                    return reservation;
                }
            }
        }
        throw new NoRoomAvailableException("No available " + type + " room for " + checkIn + " -> " + checkOut);
    }

    private boolean isRoomFree(String roomId, LocalDate checkIn, LocalDate checkOut) {
        for (Reservation r : activeByRoom.getOrDefault(roomId, List.of())) {
            if (r.getStatus() == ReservationStatus.CANCELLED || r.getStatus() == ReservationStatus.CHECKED_OUT) continue;
            if (r.overlaps(checkIn, checkOut)) return false;
        }
        return true;
    }

    @Override
    public Reservation checkIn(String reservationId) throws InvalidReservationStateException {
        Reservation r = requireReservation(reservationId);
        if (!r.tryTransition(ReservationStatus.BOOKED, ReservationStatus.CHECKED_IN)) {
            throw new InvalidReservationStateException("Cannot check in reservation in state " + r.getStatus());
        }
        return r;
    }

    @Override
    public Reservation checkOut(String reservationId) throws InvalidReservationStateException {
        Reservation r = requireReservation(reservationId);
        if (!r.tryTransition(ReservationStatus.CHECKED_IN, ReservationStatus.CHECKED_OUT)) {
            throw new InvalidReservationStateException("Cannot check out reservation in state " + r.getStatus());
        }
        return r;
    }

    @Override
    public Optional<Reservation> cancelReservation(String reservationId) {
        Reservation r = reservations.get(reservationId);
        if (r == null || !r.tryTransition(ReservationStatus.BOOKED, ReservationStatus.CANCELLED)) return Optional.empty();
        return Optional.of(r);
    }

    @Override
    public List<Reservation> getReservationsForGuest(String guestId) {
        List<Reservation> result = new ArrayList<>();
        for (Reservation r : reservations.values()) {
            if (r.getGuest().getId().equals(guestId)) result.add(r);
        }
        return result;
    }

    private Reservation requireReservation(String reservationId) throws InvalidReservationStateException {
        Reservation r = reservations.get(reservationId);
        if (r == null) throw new InvalidReservationStateException("Unknown reservation: " + reservationId);
        return r;
    }
}
