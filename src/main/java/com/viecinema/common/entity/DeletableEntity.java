package com.viecinema.common.entity;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@MappedSuperclass // Say that this is not an entity
@SQLDelete(sql = "UPDATE movies SET deleted_at = NOW() WHERE movie_id = ?") // soft delete
@Where(clause = "deleted_at IS NULL") // Automatically add this condition when querying this entity
@Getter
@Setter
public class DeletableEntity extends BaseEntity {
    private LocalDateTime deletedAt;
}
