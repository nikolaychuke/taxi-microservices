package service;

import jakarta.transaction.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import api.UserDtos;
import domain.Driver;
import domain.DriverStatus;
import domain.Passenger;
import repository.DriverRepository;
import repository.PassengerRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserManagementService {
    private static final String AVAILABLE_DRIVERS_CACHE_KEY = "taxi:drivers:available";

    private final PassengerRepository passengerRepository;
    private final DriverRepository driverRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public UserManagementService(PassengerRepository passengerRepository,
                                 DriverRepository driverRepository,
                                 StringRedisTemplate redisTemplate,
                                 ObjectMapper objectMapper) {
        this.passengerRepository = passengerRepository;
        this.driverRepository = driverRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public UserDtos.PassengerResponse createPassenger(UserDtos.PassengerCreateRequest request) {
        Passenger p = new Passenger();
        p.setName(request.name());
        p.setEmail(request.email());
        p.setPhone(request.phone());
        Passenger saved = passengerRepository.save(p);
        return new UserDtos.PassengerResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getPhone());
    }

    public UserDtos.DriverResponse createDriver(UserDtos.DriverCreateRequest request) {
        Driver d = new Driver();
        d.setName(request.name());
        d.setEmail(request.email());
        d.setPhone(request.phone());
        d.setLicenseNumber(request.licenseNumber());
        Driver saved = driverRepository.save(d);
        invalidateAvailableDriversCache();
        return toDriverResponse(saved);
    }

    public UserDtos.PassengerResponse getPassenger(Long id) {
        Passenger p = passengerRepository.findById(id).orElseThrow();
        return new UserDtos.PassengerResponse(p.getId(), p.getName(), p.getEmail(), p.getPhone());
    }

    public UserDtos.DriverResponse getDriver(Long id) {
        return toDriverResponse(driverRepository.findById(id).orElseThrow());
    }

    public UserDtos.DriverResponse updateDriverStatus(Long id, DriverStatus status) {
        Driver d = driverRepository.findById(id).orElseThrow();
        d.setStatus(status);
        Driver saved = driverRepository.save(d);
        invalidateAvailableDriversCache();
        return toDriverResponse(saved);
    }

    public boolean passengerExists(Long passengerId) {
        return passengerRepository.existsById(passengerId);
    }

    @Transactional
    public Long assignFreeDriverAtomically() {
        for (UserDtos.DriverResponse candidate : getAvailableDrivers()) {
            Driver locked = driverRepository.findByIdForUpdate(candidate.id()).orElse(null);
            if (locked != null && locked.getStatus() == DriverStatus.AVAILABLE) {
                locked.setStatus(DriverStatus.BUSY);
                invalidateAvailableDriversCache();
                return locked.getId();
            }
        }

        Driver fallback = driverRepository.findFirstAvailableForUpdate().orElse(null);
        if (fallback == null) {
            return null;
        }
        fallback.setStatus(DriverStatus.BUSY);
        invalidateAvailableDriversCache();
        return fallback.getId();
    }

    @Transactional
    public void setDriverStatus(Long driverId, DriverStatus status) {
        Driver d = driverRepository.findById(driverId).orElseThrow();
        d.setStatus(status);
        driverRepository.save(d);
        invalidateAvailableDriversCache();
    }

    public List<UserDtos.DriverResponse> getAvailableDrivers() {
        try {
            String cached = redisTemplate.opsForValue().get(AVAILABLE_DRIVERS_CACHE_KEY);
            if (cached != null && !cached.isBlank()) {
                return objectMapper.readValue(cached, new TypeReference<>() {});
            }
        } catch (Exception ignored) {
        }
        List<UserDtos.DriverResponse> fresh = driverRepository.findTop20ByStatusOrderByIdAsc(DriverStatus.AVAILABLE)
                .stream()
                .map(this::toDriverResponse)
                .toList();
        try {
            redisTemplate.opsForValue().set(AVAILABLE_DRIVERS_CACHE_KEY, objectMapper.writeValueAsString(fresh));
        } catch (Exception ignored) {
        }
        return fresh;
    }

    private void invalidateAvailableDriversCache() {
        try {
            redisTemplate.delete(AVAILABLE_DRIVERS_CACHE_KEY);
        } catch (Exception ignored) {
        }
    }

    private UserDtos.DriverResponse toDriverResponse(Driver d) {
        return new UserDtos.DriverResponse(d.getId(), d.getName(), d.getEmail(), d.getPhone(), d.getLicenseNumber(), d.getStatus());
    }
}
