package com.example.booking.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "seat",
        uniqueConstraints = @UniqueConstraint(columnNames = {"show_id", "seatNumber"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String seatNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "show_id")
    private Show show;

    @Column(nullable = false)
    private boolean isBooked;
}