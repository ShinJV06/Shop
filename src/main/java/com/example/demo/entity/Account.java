package com.example.demo.entity;


import com.example.demo.entity.Enum.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import java.util.Date;



@Getter
@Setter
@Entity
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    /**
     * VARCHAR đủ dài — cột MySQL kiểu ENUM/VARCHAR quá ngắn sẽ gây lỗi "Data truncated" khi đổi sang CTV/BANNED.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32, columnDefinition = "varchar(32)")
    private Role role;

    @NotBlank(message = "Username cannot be blank")
    String username;

    @NotBlank(message = "Email cannot be blank")
    String email;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be valid")
    String phone;

    @NotBlank(message = "Password cannot be blank")
    String password;

    Date createdAt;

    String resetToken;

    Date resetTokenExpiredAt;

    @Column(nullable = false)
    private boolean locked = false;

    @Column(nullable = false)
    private double wallet = 0.0;

    private String provider; // google, facebook, local

    @PrePersist
    void prePersistDefaults() {
        if (role == null) {
            role = Role.USER;
        }
    }

}
