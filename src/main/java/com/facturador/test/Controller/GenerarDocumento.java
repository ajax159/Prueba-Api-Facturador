package com.facturador.test.Controller;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.SAXException;

import com.facturador.test.Model.Boleta;
import com.facturador.test.Service.GenerarXML;

import io.github.project.openubl.xsender.files.exceptions.UnsupportedXMLFileException;
import lombok.RequiredArgsConstructor;
import service.sunat.gob.pe.billconsultservice.StatusResponse;

@RestController
@RequiredArgsConstructor
public class GenerarDocumento {

    @Autowired
    private GenerarXML generarXML;

    @PostMapping("/boleta")
    public Boleta postMethodName(@RequestBody Boleta boleta) throws KeyStoreException, UnrecoverableEntryException,
            NoSuchAlgorithmException, CertificateException, InvalidAlgorithmParameterException, IOException,
            XMLSignatureException, MarshalException, SAXException, ParserConfigurationException, TransformerException,
            UnsupportedXMLFileException {
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
