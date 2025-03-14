package com.facturador.test.Service;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.facturador.test.Model.Boleta;
import com.facturador.test.Model.Items;

import io.github.project.openubl.xbuilder.content.catalogs.Catalog6;
import io.github.project.openubl.xbuilder.content.models.common.Cliente;
import io.github.project.openubl.xbuilder.content.models.common.Proveedor;
import io.github.project.openubl.xbuilder.content.models.standard.general.DocumentoVentaDetalle;
import io.github.project.openubl.xbuilder.content.models.standard.general.Invoice;
import io.github.project.openubl.xbuilder.enricher.ContentEnricher;
import io.github.project.openubl.xbuilder.enricher.config.DateProvider;
import io.github.project.openubl.xbuilder.enricher.config.Defaults;
import io.github.project.openubl.xbuilder.renderer.TemplateProducer;
import io.github.project.openubl.xbuilder.signature.CertificateDetails;
import io.github.project.openubl.xbuilder.signature.CertificateDetailsFactory;
import io.github.project.openubl.xbuilder.signature.XMLSigner;
import io.github.project.openubl.xsender.camel.utils.CamelUtils;
import io.github.project.openubl.xsender.company.CompanyCredentials;
import io.github.project.openubl.xsender.company.CompanyURLs;
import io.github.project.openubl.xsender.files.BillServiceFileAnalyzer;
import io.github.project.openubl.xsender.files.BillServiceXMLFileAnalyzer;
import io.github.project.openubl.xsender.files.ZipFile;
import io.github.project.openubl.xsender.files.exceptions.UnsupportedXMLFileException;
import io.github.project.openubl.xsender.sunat.BillServiceDestination;
import io.quarkus.qute.Template;

@Service
public class GenerarXML {
    Defaults defaults = Defaults.builder()
            .icbTasa(new BigDecimal("0.2"))
            .igvTasa(new BigDecimal("0.18"))
            .build();
    DateProvider dateProvider = () -> LocalDate.of(2019, 12, 24);

    CompanyURLs companyURLs = CompanyURLs.builder()
            .invoice("https://e-beta.sunat.gob.pe/ol-ti-itcpfegem-beta/billService")
            .perceptionRetention("https://e-beta.sunat.gob.pe/ol-ti-itemision-otroscpe-gem-beta/billService")
            .despatch("https://api-cpe.sunat.gob.pe/v1/contribuyente/gem")
            .build();

    CompanyCredentials credentials = CompanyCredentials.builder()
            .username("12345678959MODDATOS")
            .password("MODDATOS")
            .token("accessTokenParaGuiasDeRemision")
            .build();

    public Boleta generarBoleta(Boleta boleta) throws KeyStoreException, UnrecoverableEntryException,
            NoSuchAlgorithmException, CertificateException, IOException, InvalidAlgorithmParameterException,
            XMLSignatureException, MarshalException, SAXException, ParserConfigurationException, TransformerException,
            UnsupportedXMLFileException {
        @SuppressWarnings("rawtypes")
        Invoice.InvoiceBuilder invoiceBuilder = Invoice.builder()
                .serie(boleta.getSerie())
                .numero(boleta.getNumero())
                .proveedor(Proveedor.builder()
                        .ruc(boleta.getRuc())
                        .razonSocial(boleta.getRazonSocial())
                        .build())
                .cliente(Cliente.builder()
                        .nombre(boleta.getNombre())
                        .numeroDocumentoIdentidad(boleta.getDoc())
                        .tipoDocumentoIdentidad(Catalog6.RUC.toString())
                        .build());
        for (Items item : boleta.getItems()) {
            invoiceBuilder.detalle(DocumentoVentaDetalle.builder()
                    .descripcion(item.getDescripcion())
                    .cantidad(item.getCantidad())
                    .precio(item.getPrecio())
                    .unidadMedida(item.getUnidad())
                    .build());
        }
        // .detalle(DocumentoVentaDetalle.builder()
        // .descripcion("Item1")
        // .cantidad(new BigDecimal("10"))
        // .precio(new BigDecimal("100"))
        // .unidadMedida("KGM")
        // .build())
        // .detalle(DocumentoVentaDetalle.builder()
        // .descripcion("Item2")
        // .cantidad(new BigDecimal("10"))
        // .precio(new BigDecimal("100"))
        // .unidadMedida("KGM")
        // .build())
        Invoice input = invoiceBuilder.build();

        ContentEnricher enricher = new ContentEnricher(defaults, dateProvider);
        enricher.enrich(input);

        Template template = TemplateProducer.getInstance().getInvoice();
        String xml = template.data(input).render();
        // create xml file
        InputStream ksInputStream = new FileInputStream(new File("certificadoprueba.pfx"));
        CertificateDetails certificate = CertificateDetailsFactory.create(ksInputStream, "pruebita");
        String signatureID = "Miempresaprueba";
        X509Certificate certificatex = certificate.getX509Certificate();
        PrivateKey privateKey = certificate.getPrivateKey();

        Document signedXML = XMLSigner.signXML(xml, signatureID, certificatex, privateKey);

        DOMSource source = new DOMSource(signedXML);
        FileWriter writer = new FileWriter("12345678959-01-F001-00000001.xml");
        StreamResult resultXml = new StreamResult(writer);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.transform(source, resultXml);
        File file = new File("12345678959-01-F001-00000001.xml");
        generarFactura();

        return boleta;
    }

    public void generarFactura()
            throws IOException, ParserConfigurationException, UnsupportedXMLFileException, SAXException {
        Path miXML = Paths.get("12345678959-01-F001-00000001.xml"); // El XML puede ser "Path, InputStream, o bytes[]"
        BillServiceFileAnalyzer fileAnalyzer = new BillServiceXMLFileAnalyzer(miXML, companyURLs);

        // Archivo ZIP
        ZipFile zipFile = fileAnalyzer.getZipFile();

        // Configuración para enviar xml y Configuración para consultar ticket
        BillServiceDestination fileDestination = fileAnalyzer.getSendFileDestination();

        CamelUtils.getBillServiceCamelData(zipFile, fileDestination, credentials);

    }

}
