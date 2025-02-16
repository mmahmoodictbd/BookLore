package com.adityachandel.booklore.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_permissions")
public class UserPermissionsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private BookLoreUserEntity user;

    @Column(name = "permission_upload", nullable = false)
    private boolean permissionUpload = false;

    @Column(name = "permission_download", nullable = false)
    private boolean permissionDownload = false;

    @Column(name = "permission_edit_metadata", nullable = false)
    private boolean permissionEditMetadata = false;

    @Column(name = "permission_admin", nullable = false)
    private boolean permissionAdmin;
}