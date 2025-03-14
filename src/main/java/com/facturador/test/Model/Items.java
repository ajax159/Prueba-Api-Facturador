package com.facturador.test.Model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Items {

    private String descripcion;
    private BigDecimal cantidad;
    private BigDecimal precio;
    private String unidad;
}
