package io.nextpos.einvoice.einvoicemessage.service;

import com.tradevan.gateway.einv.msg.EINVPayload;
import com.tradevan.gateway.einv.msg.v32.E0402;
import com.tradevan.gateway.einv.msg.v32.E0402Body.BranchTrackBlankItem;
import com.tradevan.gateway.einv.msg.v32.E0402Body.DetailsType;
import com.tradevan.gateway.einv.msg.v32.E0402Body.MainType;
import com.tradevan.gateway.einv.msg.v32.UtilBody.InvoiceTypeEnum;
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoice.ElectronicInvoiceRepository;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueue;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRange;
import io.nextpos.einvoice.common.shared.EInvoiceBaseObject;
import io.nextpos.einvoice.shared.config.TurnkeyConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
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
    public ElectronicInvoice createElectronicInvoiceMIG(PendingEInvoiceQueue pendingEInvoiceQueue) {

        final ElectronicInvoice electronicInvoice = pendingEInvoiceQueue.getElectronicInvoice();

        final EInvoicePayloadUploader uploader = EInvoicePayloadUploader.select(pendingEInvoiceQueue, turnkeyConfigProperties);
        final EINVPayload einvPayload = uploader.buildAndUpload(electronicInvoice, pendingEInvoiceQueue);
        
        electronicInvoice.setInvoiceStatus(ElectronicInvoice.InvoiceStatus.MIG_CREATED);

        this.setInvoiceIdentifier(pendingEInvoiceQueue, einvPayload);

        return electronicInvoiceRepository.save(electronicInvoice);
    }

    @Override
    public InvoiceNumberRange createUnusedInvoiceNumberMIG(InvoiceNumberRange invoiceNumberRange) {

        final List<BranchTrackBlankItem> blankItems = invoiceNumberRange.getNumberRanges().stream()
                .filter(r -> !r.isFinished())
                .map(r -> {
                    final BranchTrackBlankItem blankItem = new BranchTrackBlankItem();
                    blankItem.setInvoiceBeginNo(r.getNextIncrement());
                    blankItem.setInvoiceEndNo(r.getRangeTo());

                    return blankItem;
                }).collect(Collectors.toList());

        return this.createUnusedInvoiceNumberMIGInternal(invoiceNumberRange, blankItems);
    }

    @Override
    public InvoiceNumberRange createUnusedInvoiceNumberMIG(InvoiceNumberRange invoiceNumberRange, String rangeFrom, String rangeTo) {

        final BranchTrackBlankItem branchTrackBlankItem = new BranchTrackBlankItem();
        branchTrackBlankItem.setInvoiceBeginNo(rangeFrom);
        branchTrackBlankItem.setInvoiceEndNo(rangeTo);

        return this.createUnusedInvoiceNumberMIGInternal(invoiceNumberRange, Collections.singletonList(branchTrackBlankItem));
    }

    private InvoiceNumberRange createUnusedInvoiceNumberMIGInternal(InvoiceNumberRange invoiceNumberRange, List<BranchTrackBlankItem> branchTrackBlankItems) {

        final E0402 einvPayload = new E0402();
        MainType main = new MainType();
        main.setHeadBan(invoiceNumberRange.getUbn());
        main.setBranchBan(invoiceNumberRange.getUbn());
        main.setInvoiceType(InvoiceTypeEnum.SixGeneralTaxType);
        main.setYearMonth(invoiceNumberRange.getShortRangeIdentifier());
        main.setInvoiceTrack(invoiceNumberRange.getNumberRanges().get(0).getPrefix());
        einvPayload.setMain(main);

        DetailsType details = new DetailsType();
        branchTrackBlankItems.forEach(details::addBranchTrackBlankItem);
        einvPayload.setDetails(details);

        EINVPayloadCopier.copyPayload(turnkeyConfigProperties.getB2p().getUnusedInvoiceNumberDir(), einvPayload);

        setInvoiceIdentifier(invoiceNumberRange, einvPayload);

        return invoiceNumberRange;
    }

    private void setInvoiceIdentifier(EInvoiceBaseObject eInvoiceBaseObject, EINVPayload einvPayload) {

        final String invoiceIdentifier = einvPayload.getInvoiceIdentifier();
        LOGGER.info("Setting invoice identifier {} on pending e-invoice", invoiceIdentifier);
        eInvoiceBaseObject.setInvoiceIdentifier(invoiceIdentifier);
    }
}
