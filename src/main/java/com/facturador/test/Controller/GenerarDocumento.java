package com.facturador.test.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.facturador.test.Model.Boleta;
import com.facturador.test.Service.GenerarXML;

import lombok.RequiredArgsConstructor;
import service.sunat.gob.pe.billconsultservice.StatusResponse;

@RestController
@RequiredArgsConstructor
public class GenerarDocumento {

    @Autowired
    private GenerarXML generarXML;

    @PostMapping("/boleta")
    public Boleta postMethodName(@RequestBody Boleta boleta) throws Exception {
        generarXML.generarBoleta(boleta);
        return boleta;
    }

    @PostMapping("/consultarcdr")
    public Boleta consultartBol(@RequestBody Boleta boleta) {
        generarXML.consultarCDR(boleta);

        return boleta;
    }

    @PostMapping("/consultarxml")
    public Boleta consultartxml(@RequestBody Boleta boleta) {
        generarXML.consultaXML(boleta);

        return boleta;
    }

}
