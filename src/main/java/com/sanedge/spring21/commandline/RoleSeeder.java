package com.sanedge.spring21.commandline;

import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.sanedge.spring21.enums.ERole;
import com.sanedge.spring21.models.Role;
import com.sanedge.spring21.repository.RoleRepository;

@Component
public class RoleSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public RoleSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        seedRoles();
    }

    private void seedRoles() {
        Arrays.stream(ERole.values())
                .forEach(role -> roleRepository.findByName(role)
                        .orElseGet(() -> {
                            Role newRole = new Role();

                            newRole.setName(role);

                            return roleRepository.save(newRole);
                        }));
    }
}
