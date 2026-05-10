package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import domain.Driver;
import domain.DriverStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import repository.DriverRepository;
import repository.PassengerRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserManagementRedisCacheUnitTest {

    @Mock
    private PassengerRepository passengerRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    private UserManagementService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new UserManagementService(passengerRepository, driverRepository, redisTemplate, objectMapper);
    }

    @Test
    void getAvailableDriversUsesDatabaseWhenCacheMissThenReadsFromCache() throws Exception {
        when(valueOps.get(anyString())).thenReturn(null);
        Driver d = new Driver();
        d.setName("Cache");
        d.setEmail("c@test");
        d.setPhone("+7");
        d.setLicenseNumber("L1");
        d.setStatus(DriverStatus.AVAILABLE);
        when(driverRepository.findTop20ByStatusOrderByIdAsc(DriverStatus.AVAILABLE)).thenReturn(List.of(d));

        var first = service.getAvailableDrivers();
        assertEquals(1, first.size());
        verify(driverRepository).findTop20ByStatusOrderByIdAsc(DriverStatus.AVAILABLE);
        verify(valueOps).set(anyString(), anyString());

        String cachedJson = objectMapper.writeValueAsString(first);
        when(valueOps.get(anyString())).thenReturn(cachedJson);

        service.getAvailableDrivers();

        verify(driverRepository, times(1)).findTop20ByStatusOrderByIdAsc(DriverStatus.AVAILABLE);
    }

    @Test
    void updateDriverStatusInvalidatesRedisKey() {
        when(valueOps.get(anyString())).thenReturn(null);

        Driver d = mock(Driver.class);
        lenient().when(d.getStatus()).thenReturn(DriverStatus.AVAILABLE);
        lenient().when(d.getId()).thenReturn(1L);
        lenient().when(d.getName()).thenReturn("x");
        lenient().when(d.getEmail()).thenReturn("x@y");
        lenient().when(d.getPhone()).thenReturn("1");
        lenient().when(d.getLicenseNumber()).thenReturn("L");
        when(driverRepository.findById(1L)).thenReturn(java.util.Optional.of(d));
        when(driverRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateDriverStatus(1L, DriverStatus.BUSY);

        verify(redisTemplate).delete(eq("taxi:drivers:available"));
    }
}
