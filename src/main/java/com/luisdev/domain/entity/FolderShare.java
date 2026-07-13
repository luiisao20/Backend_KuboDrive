package com.luisdev.domain.entity;

import com.luisdev.domain.enums.FilePermission;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "folder_shares")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderShare {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_with_id", nullable = false)
    private User sharedWith;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FilePermission permissions;

    @Column(updatable = false, columnDefinition = "timestamptz")
    @CreationTimestamp
    private LocalDateTime createdAt;
}
