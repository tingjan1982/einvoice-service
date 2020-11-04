package io.nextpos.einvoice.einvoicemessage.service;

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
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.number.NumberStyleFormatter;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

class CreateEInvoicePayloadUploader extends EInvoicePayloadUploader {

    public CreateEInvoicePayloadUploader(String uploadDirectory) {
        super(uploadDirectory);
    }

    @Override
    public EINVPayload constructEInvoicePayload(ElectronicInvoice electronicInvoice, PendingEInvoiceQueue pendingEInvoiceQueue) {

        final C0401 message = new C0401();
        MainType mainType = new MainType();
        mainType.setInvoiceNumber(electronicInvoice.getInternalInvoiceNumber());
        mainType.setInvoiceDate(electronicInvoice.getInvoiceCreatedDate());
        mainType.setInvoiceTime(electronicInvoice.getInvoiceCreatedDate());

        // invoice metadata
        mainType.setInvoiceType(InvoiceTypeEnum.SixGeneralTaxType); // 7 or 8
        mainType.setRandomNumber(electronicInvoice.getRandomNumber());

        if (electronicInvoice.getCarrierType() != null) {
            String carrierType = electronicInvoice.getCarrierType() == ElectronicInvoice.CarrierType.MOBILE ? "3J0002" : "CQ0001";
            mainType.setCarrierType(carrierType);
            mainType.setCarrierId1(electronicInvoice.getCarrierId());
            mainType.setCarrierId2(electronicInvoice.getCarrierId2());

            mainType.setPrintMark("N");

            if (StringUtils.isNotBlank(electronicInvoice.getBuyerUbn())) {
                mainType.setPrintMark(electronicInvoice.isPrintMark() ? "Y" : "N");
            }
        } else {
            mainType.setPrintMark("Y");
        }

        if (StringUtils.isNotBlank(electronicInvoice.getNpoBan())) {
            mainType.setNPOBAN(electronicInvoice.getNpoBan());
            mainType.setDonateMark(DonateMarkEnum.Donated);
        } else {
            mainType.setDonateMark(DonateMarkEnum.NotDonated);
        }

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
}
