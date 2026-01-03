package com.viecinema.movie.entity;

import com.viecinema.common.entity.DeletableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "actors")
public class Actor extends DeletableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "actor_id", nullable = false)
    private Integer id;

    @Size(max = 100)
    @NotNull
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 50)
    @Column(name = "nationality", length = 50)
    private String nationality;

    @Lob
    @Column(name = "bio")
    private String bio;

    @Size(max = 255)
    @Column(name = "photo_url")
    private String photoUrl;
}