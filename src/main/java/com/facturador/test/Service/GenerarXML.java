package com.facturador.test.Service;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
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

import org.apache.camel.CamelContext;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseBody;
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
import io.github.project.openubl.xsender.Constants;
import io.github.project.openubl.xsender.camel.StandaloneCamel;
import io.github.project.openubl.xsender.camel.utils.CamelData;
import io.github.project.openubl.xsender.camel.utils.CamelUtils;
import io.github.project.openubl.xsender.company.CompanyCredentials;
import io.github.project.openubl.xsender.company.CompanyURLs;
import io.github.project.openubl.xsender.files.BillServiceFileAnalyzer;
import io.github.project.openubl.xsender.files.BillServiceXMLFileAnalyzer;
import io.github.project.openubl.xsender.files.ZipFile;
import io.github.project.openubl.xsender.files.exceptions.UnsupportedXMLFileException;
import io.github.project.openubl.xsender.models.SunatResponse;
import io.github.project.openubl.xsender.sunat.BillConsultServiceDestination;
import io.github.project.openubl.xsender.sunat.BillServiceDestination;
import io.github.project.openubl.xsender.sunat.BillValidServiceDestination;

import io.quarkus.qute.Template;
import service.sunat.gob.pe.billconsultservice.StatusResponse;

@Service
public class GenerarXML {
        Defaults defaults = Defaults.builder()
                        .icbTasa(new BigDecimal("0.2"))
                        .igvTasa(new BigDecimal("0.18"))
                        .build();
        DateProvider dateProvider = () -> LocalDate.of(2019, 12, 24);
        // ruc emisor = 29496669593
        CompanyURLs companyURLs = CompanyURLs.builder()
                        // https://e-beta.sunat.gob.pe/ol-ti-itcpfegem-beta/billService
                        .invoice("https://e-beta.sunat.gob.pe/ol-ti-itcpfegem-beta/billService?wsdl")
                        .perceptionRetention(
                                        "https://e-beta.sunat.gob.pe/ol-ti-itemision-otroscpe-gem-beta/billService?wsdl")
                        .despatch("https://api-cpe.sunat.gob.pe/v1/contribuyente/gem")
                        .build();

        CompanyCredentials credentials = CompanyCredentials.builder()
                        .username("29496669593MODDATOS")
                        .password("MODDATOS")
                        .token("accessTokenParaGuiasDeRemision")
                        .build();

        public Boleta generarBoleta(Boleta boleta) throws KeyStoreException, UnrecoverableEntryException,
                        NoSuchAlgorithmException, CertificateException, IOException, InvalidAlgorithmParameterException,
                        XMLSignatureException, MarshalException, SAXException, ParserConfigurationException,
                        TransformerException,
                        UnsupportedXMLFileException {

                String xmlname = boleta.getRuc() + "-" + "01" + "-" + boleta.getSerie() + "-" + boleta.getNumero()
                                + ".xml";
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
                Invoice input = invoiceBuilder.build();

                ContentEnricher enricher = new ContentEnricher(defaults, dateProvider);
                enricher.enrich(input);

                Template template = TemplateProducer.getInstance().getInvoice();
                String xml = template.data(input).render();

                InputStream ksInputStream = new FileInputStream(new File("certificadoprueba.pfx"));
                CertificateDetails certificate = CertificateDetailsFactory.create(ksInputStream, "pruebita");
                String signatureID = "Miempresaprueba";
                X509Certificate certificatex = certificate.getX509Certificate();
                PrivateKey privateKey = certificate.getPrivateKey();

                Document signedXML = XMLSigner.signXML(xml, signatureID, certificatex, privateKey);

                // 12345678912-01-F001-100.xml

                DOMSource source = new DOMSource(signedXML);
                FileWriter writer = new FileWriter(xmlname);
                StreamResult resultXml = new StreamResult(writer);

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.transform(source, resultXml);
                // File file = new File(boleta.getRuc() + "-" + "01" + "-" + boleta.getSerie() +
                // "-" + boleta.getNumero()
                // + ".xml");
                enviarFactura(xmlname);

                return boleta;
        }

        public void enviarFactura(String name)
                        throws IOException, ParserConfigurationException, UnsupportedXMLFileException, SAXException {
                Path miXML = Paths.get(name);
                byte[] bytes = new byte[1024];

                FileInputStream fis = new FileInputStream(name);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                for (int readNum; (readNum = fis.read(bytes)) != -1;) {
                        bos.write(bytes, 0, readNum);
                        System.out.println("read " + readNum + " bytes,");
                }
                fis.close();

                BillServiceFileAnalyzer fileAnalyzer = new BillServiceXMLFileAnalyzer(miXML, companyURLs);
                ZipFile zipFile = fileAnalyzer.getZipFile();

                BillServiceDestination fileDestination = fileAnalyzer.getSendFileDestination();

                CamelData camelData = CamelUtils.getBillServiceCamelData(zipFile, fileDestination, credentials);
                CamelContext camelContext = StandaloneCamel.getInstance()
                                .getMainCamel()
                                .getCamelContext();

                camelContext.createProducerTemplate()
                                .requestBodyAndHeaders(
                                                Constants.XSENDER_BILL_SERVICE_URI,
                                                camelData.getBody(),
                                                camelData.getHeaders(),
                                                SunatResponse.class);

        }

        public Boleta consultarCDR(Boleta boleta) {
                BillConsultServiceDestination destination = BillConsultServiceDestination.builder()
                                // https://e-factura.sunat.gob.pe/ol-it-wsconscpegem/billConsultService?wsdl
                                .url("https://e-factura.sunat.gob.pe/ol-it-wsconscpegem/billConsultService?wsdl")
                                .operation(BillConsultServiceDestination.Operation.GET_STATUS)
                                .build();

                CamelData camelData = CamelUtils.getBillConsultService(
                                boleta.getRuc(), // RUC
                                "01", // Código de tipo de comprobante
                                boleta.getSerie(), // Serie del comprobante
                                boleta.getNumero(), // Número del comprobante
                                destination,
                                credentials);
                CamelContext camelContext = StandaloneCamel.getInstance()
                                .getMainCamel()
                                .getCamelContext();

                service.sunat.gob.pe.billconsultservice.StatusResponse sunatResponse = camelContext
                                .createProducerTemplate()
                                .requestBodyAndHeaders(
                                                Constants.XSENDER_BILL_CONSULT_SERVICE_URI,
                                                camelData.getBody(),
                                                camelData.getHeaders(),
                                                service.sunat.gob.pe.billconsultservice.StatusResponse.class);
                System.out.println(sunatResponse);
                return boleta;
        }

        public Boleta consultaXML(Boleta boleta) {
                String fileName = boleta.getRuc() + "-" + "01" + "-" + boleta.getSerie() + "-" + boleta.getNumero()
                                + ".xml";
                byte[] fileContent = readFileAsBytes(fileName);

                BillValidServiceDestination destination = BillValidServiceDestination.builder()
                                // https://e-factura.sunat.gob.pe/ol-it-wsconscpegem/billConsultService?wsdl
                                // https://e-factura.sunat.gob.pe/ol-it-wsconsvalidcpe/billValidService
                                .url("https://e-factura.sunat.gob.pe/ol-it-wsconscpegem/billConsultService?wsdl")
                                .build();

                CamelData camelData = CamelUtils.getBillValidService(
                                fileName,
                                fileContent,
                                destination,
                                credentials);
                CamelContext camelContext = StandaloneCamel.getInstance()
                                .getMainCamel()
                                .getCamelContext();

                service.sunat.gob.pe.billvalidservice.StatusResponse sunatResponse = camelContext
                                .createProducerTemplate()
                                .requestBodyAndHeaders(
                                                Constants.XSENDER_BILL_VALID_SERVICE_URI,
                                                camelData.getBody(),
                                                camelData.getHeaders(),
                                                service.sunat.gob.pe.billvalidservice.StatusResponse.class);
                return boleta;
        }

        private byte[] readFileAsBytes(String fileName) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try {
                        FileInputStream fis = new FileInputStream(fileName);
                        byte[] buf = new byte[1024];
                        for (int readNum; (readNum = fis.read(buf)) != -1;) {
                                bos.write(buf, 0, readNum);
                                System.out.println("read " + readNum + " bytes,");
                        }
                } catch (IOException e) {
                        e.printStackTrace();
                }
                return bos.toByteArray();
        }

}
