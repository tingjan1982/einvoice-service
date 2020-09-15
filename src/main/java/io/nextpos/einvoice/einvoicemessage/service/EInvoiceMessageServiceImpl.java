package io.nextpos.einvoice.einvoicemessage.service;

import com.tradevan.gateway.client.einv.parse.ParserHelper;
import com.tradevan.gateway.einv.msg.EINVPayload;
import com.tradevan.gateway.einv.msg.v32.C0401;
import com.tradevan.gateway.einv.msg.v32.C0401Body.AmountType;
import com.tradevan.gateway.einv.msg.v32.C0401Body.DetailsType;
import com.tradevan.gateway.einv.msg.v32.C0401Body.MainType;
import com.tradevan.gateway.einv.msg.v32.C0401Body.ProductItem;
import com.tradevan.gateway.einv.msg.v32.UtilBody.DonateMarkEnum;
import com.tradevan.gateway.einv.msg.v32.UtilBody.InvoiceTypeEnum;
import com.tradevan.gateway.einv.msg.v32.UtilBody.RoleDescriptionType;
import com.tradevan.gateway.einv.msg.v32.UtilBody.TaxTypeEnum;
import io.micrometer.core.instrument.util.StringUtils;
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoice.ElectronicInvoiceRepository;
import io.nextpos.einvoice.shared.config.TurnkeyConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.number.NumberStyleFormatter;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileWriter;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class EInvoiceMessageServiceImpl implements EInvoiceMessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EInvoiceMessageServiceImpl.class);

    private final ElectronicInvoiceRepository electronicInvoiceRepository;

    private final TurnkeyConfigProperties turnkeyConfigProperties;

    @Autowired
    public EInvoiceMessageServiceImpl(ElectronicInvoiceRepository electronicInvoiceRepository, TurnkeyConfigProperties turnkeyConfigProperties) {
        this.electronicInvoiceRepository = electronicInvoiceRepository;
        this.turnkeyConfigProperties = turnkeyConfigProperties;
    }

    @Override
    public ElectronicInvoice createEInvoice(String id) {

        final ElectronicInvoice electronicInvoice = electronicInvoiceRepository.findById(id).orElseThrow(() -> {
            throw new RuntimeException("EInvoice not found: " + id);
        });

        final EINVPayload einvPayload = this.constructEInvoicePayload(electronicInvoice);
        this.sendPayloadToPath(electronicInvoice, einvPayload);

        electronicInvoice.setInvoiceStatus(ElectronicInvoice.InvoiceStatus.MIG_CREATED);

        return electronicInvoiceRepository.save(electronicInvoice);
    }

    EINVPayload constructEInvoicePayload(ElectronicInvoice electronicInvoice) {

        final C0401 message = new C0401();
        MainType mainType = new MainType();
        mainType.setInvoiceNumber(electronicInvoice.getInternalInvoiceNumber());
        mainType.setInvoiceDate(electronicInvoice.getInvoiceCreatedDate());
        mainType.setInvoiceTime(electronicInvoice.getInvoiceCreatedDate());

        // invoice metadata
        mainType.setInvoiceType(InvoiceTypeEnum.SixGeneralTaxType); // 7 or 8
        mainType.setDonateMark(DonateMarkEnum.NotDonated);
        mainType.setPrintMark("Y");
        mainType.setRandomNumber(electronicInvoice.getRandomNumber());
        // if carrier is specified
//        mainType.setCarrierType("");
//        mainType.setCarrierId1("");
//        mainType.setCarrierId2("");

        RoleDescriptionType seller = new RoleDescriptionType();
        seller.setIdentifier(electronicInvoice.getSellerUbn());
        seller.setName(electronicInvoice.getSellerName());
        mainType.setSeller(seller);
        RoleDescriptionType buyer = new RoleDescriptionType();

        if (StringUtils.isNotBlank(electronicInvoice.getBuyerUbn())) {
            buyer.setIdentifier(electronicInvoice.getBuyerUbn());
            buyer.setName(electronicInvoice.getBuyerUbn());
        } else {
            buyer.setIdentifier("0000000000");
            buyer.setName(electronicInvoice.getRandomNumber());
        }

        mainType.setBuyer(buyer);
        message.setMain(mainType);

        DetailsType details = new DetailsType();

        final AtomicInteger index = new AtomicInteger(1);
        NumberStyleFormatter formatter = new NumberStyleFormatter("000");
        final NumberFormat nf = formatter.getNumberFormat(Locale.getDefault());

        final List<ProductItem> productItems = electronicInvoice.getInvoiceItems().stream()
                .map(li -> {
                    final ProductItem productItem = new ProductItem();
                    final String seqNumber = nf.format(index.getAndIncrement());
                    productItem.setSequenceNumber(seqNumber);
                    productItem.setDescription(li.getProductName());
                    productItem.setQuantity(String.valueOf(li.getQuantity()));
                    productItem.setUnitPrice(li.getUnitPrice().toString());
                    productItem.setAmount(li.getSubTotal().toString());

                    return productItem;
                }).collect(Collectors.toList());

        details.setProductItemList(productItems);
        message.setDetails(details);

        AmountType amount = new AmountType();
        amount.setSalesAmount(electronicInvoice.getSalesAmountWithoutTax().toString());
        amount.setFreeTaxSalesAmount("0");
        amount.setZeroTaxSalesAmount("0");
        amount.setTaxType(TaxTypeEnum.Taxable);
        amount.setTaxRate("0.05");
        amount.setTaxAmount(electronicInvoice.getTaxAmount().toString());
        amount.setTotalAmount(electronicInvoice.getSalesAmount().toString());
        message.setAmount(amount);

        return message;
    }

    void sendPayloadToPath(ElectronicInvoice electronicInvoice, EINVPayload payload) {

        try {
            File migFile = null;
            if (payload instanceof C0401) {
                migFile = new File(turnkeyConfigProperties.getB2c().getCreateInvoiceDir(), payload.getInvoiceIdentifier() + ".xml");
            }

            if (migFile != null) {
                LOGGER.info("Copying invoice {} to path {}", electronicInvoice.getInvoiceNumber(), migFile.getAbsolutePath());

                final ParserHelper parserHelper = new ParserHelper();
                final String einvoiceXML = parserHelper.marshalToXML(payload);
                LOGGER.debug("Invoice XML: {}", einvoiceXML);
                FileCopyUtils.copy(einvoiceXML, new FileWriter(migFile));

                LOGGER.info("Copying invoice {} done", electronicInvoice.getInvoiceNumber());
            }
        } catch (Exception e) {
            LOGGER.error("Sending invoice {} error", electronicInvoice.getInvoiceNumber(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
