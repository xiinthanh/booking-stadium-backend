package com.ouroboros.pestadiumbookingbe.service;

import org.springframework.stereotype.Service;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

@Service
public class SupabaseService {

    private static final String DB_URL = System.getenv("POSTGRES_URL_TRANSACTION_POOLER");
    private static final String DB_USER = System.getenv("POSTGRES_USERNAME");
    private static final String DB_PASSWORD = System.getenv("POSTGRES_PASSWORD");

    public void storeUser(String email, String name) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Check if user exists
            String checkQuery = "SELECT id FROM public.profiles WHERE email = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setString(1, email);
                var resultSet = checkStmt.executeQuery();

                if (!resultSet.next()) {
                    // Insert new user
                    String insertQuery = "INSERT INTO public.profiles (id, email, full_name) VALUES (gen_random_uuid(), ?, ?)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                        insertStmt.setString(1, email);
                        insertStmt.setString(2, name);
                        insertStmt.executeUpdate();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to store user in Supabase");
        }
    }
}
