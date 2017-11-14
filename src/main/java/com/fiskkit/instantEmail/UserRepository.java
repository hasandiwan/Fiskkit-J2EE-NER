package com.fiskkit.instantEmail;

import org.springframework.data.repository.CrudRepository;

import com.fiskkit.instantEmail.models.User;

public interface UserRepository extends CrudRepository<User, Long> {
}
