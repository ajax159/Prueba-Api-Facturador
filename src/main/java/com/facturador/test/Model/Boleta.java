package com.facturador.test.Model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Boleta {

    private String serie;
    private Integer numero;
    private String ruc;
    private String razonSocial;
    private String nombre;
    private String doc;
    private List<Items> items;
}
