package com.securebank.storage;

import com.securebank.model.User;
import java.util.Map;

public interface UserRepository {
    Map<String, User> loadAll();

    void saveAll(Map<String, User> users);
}