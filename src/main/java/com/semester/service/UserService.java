package com.semester.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.semester.mapper.userMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final userMapper usernamemapper;

    @Autowired
    public UserService(userMapper usernamemapper) {
        this.usernamemapper = usernamemapper;
    }

    public boolean userExists(String username, String password) {
        String storedPassword = usernamemapper.getPassword(username);
        return storedPassword != null && storedPassword.equals(password);
    }

    public String getPassword(String username) {
        return usernamemapper.getPassword(username);
    }

    public boolean insert(String username, String password, String name) {
        return usernamemapper.insert(username, password, name);
    }
}
