package com.banking.poc.identitybootstrap.user;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/demo/users")
public class DemoUserController {

    static final String DEMO_BOOTSTRAP_SECRET_HEADER = "X-Demo-Bootstrap-Secret";

    private final DemoUserService demoUserService;
    private final String bootstrapSecret;

    public DemoUserController(
            DemoUserService demoUserService,
            @Value("${demo.bootstrap.secret}") String bootstrapSecret) {
        this.demoUserService = demoUserService;
        this.bootstrapSecret = bootstrapSecret;
    }

    @PostMapping
    public ResponseEntity<DemoUserCreatedResponse> createDemoUser(
            @RequestHeader(name = DEMO_BOOTSTRAP_SECRET_HEADER, required = false) String providedSecret,
            @Valid @RequestBody DemoUserRequest request) {
        if (!bootstrapSecret.equals(providedSecret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(demoUserService.createDemoUser(request));
    }
}
