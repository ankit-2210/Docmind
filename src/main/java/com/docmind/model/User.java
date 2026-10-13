package com.docmind.model;


import com.docmind.utils.Role;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true,nullable = false)
    private String username;

    @Column(unique = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(unique = true,nullable = false)
    private String email;

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private Set<DocumentMetadata> documentMetadataSet = new HashSet<>();

}
