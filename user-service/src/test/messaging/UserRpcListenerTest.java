package messaging;

import domain.DriverStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.UserManagementService;

import java.util.Map;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserRpcListenerTest {

    @Mock
    private UserManagementService userManagementService;

    private UserRpcListener listener;

    @BeforeEach
    void init() {
        listener = new UserRpcListener(userManagementService);
    }

    @Test
    void assignDriverDelegatesToAtomicAssign() {
        listener.assignDriver(Map.of("request", "assign"));
        verify(userManagementService).assignFreeDriverAtomically();
    }

    @Test
    void updateDriverStatusParsesEnum() {
        listener.updateDriverStatus(Map.of("driverId", 42L, "status", "AVAILABLE"));
        verify(userManagementService).setDriverStatus(42L, DriverStatus.AVAILABLE);

        listener.updateDriverStatus(Map.of("driverId", 1L, "status", "BUSY"));
        verify(userManagementService).setDriverStatus(1L, DriverStatus.BUSY);
    }
}
