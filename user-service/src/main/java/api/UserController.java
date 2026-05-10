package api;

import service.UserManagementService;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {
    private final UserManagementService service;

    public UserController(UserManagementService service) {
        this.service = service;
    }

    @PostMapping("/passengers")
    public UserDtos.PassengerResponse registerPassenger(@RequestBody UserDtos.PassengerCreateRequest request) {
        return service.createPassenger(request);
    }

    @GetMapping("/passengers/{id}")
    public UserDtos.PassengerResponse getPassenger(@PathVariable Long id) {
        return service.getPassenger(id);
    }

    @PostMapping("/drivers")
    public UserDtos.DriverResponse registerDriver(@RequestBody UserDtos.DriverCreateRequest request) {
        return service.createDriver(request);
    }

    @GetMapping("/drivers/{id}")
    public UserDtos.DriverResponse getDriver(@PathVariable Long id) {
        return service.getDriver(id);
    }

    @PatchMapping("/drivers/{id}/status")
    public UserDtos.DriverResponse updateDriverStatus(@PathVariable Long id, @RequestBody UserDtos.DriverStatusUpdateRequest request) {
        return service.updateDriverStatus(id, request.status());
    }
}
