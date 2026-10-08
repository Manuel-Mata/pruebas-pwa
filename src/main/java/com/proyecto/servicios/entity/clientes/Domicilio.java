package com.proyecto.servicios.entity.clientes;

import jakarta.persistence.*;
import lombok.*;
import com.proyecto.servicios.enums.Pais;

@Entity
@Table(name = "domicilios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Domicilio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    private Cliente cliente;

    @Column(name = "cp", nullable = false, length = 5)
    private String cp;

    @Column(name = "calle", nullable = false, length = 100)
    private String calle;

    @Column(name = "numero_exterior", nullable = false, length = 20)
    private String numeroExterior;

    @Column(name = "numero_interior", length = 20)
    private String numeroInterior;

    @Column(name = "colonia", nullable = false, length = 100)
    private String colonia;

    @Column(name = "municipio", nullable = false, length = 50)
    private String municipio;

    @Column(name = "estado", nullable = false, length = 50)
    private String estado;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "pais", nullable = false, length = 50)
    private Pais pais = Pais.MEXICO;
}
