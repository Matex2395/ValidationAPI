package com.fisa.validationapi.application.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prowidesoftware.swift.model.mx.MxPacs00800108;
import com.prowidesoftware.swift.model.mx.dic.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class Iso20022ValidatorService {

    private final ObjectMapper objectMapper;

    public void validateJsonStructure(String jsonPayload) {
        log.info("Iniciando 'Trial Assembly' con ISO 20022 (pacs.008.001.08)...");

        try {
            JsonNode root = objectMapper.readTree(jsonPayload);
            JsonNode refData = root.path("referenceData");

            if (refData.isMissingNode()) {
                throw new IllegalArgumentException("El bloque 'referenceData' es obligatorio.");
            }

            // Mensaje Raíz
            MxPacs00800108 mx = new MxPacs00800108();

            // structura Principal
            FIToFICustomerCreditTransferV08 creditTransfer = new FIToFICustomerCreditTransferV08();

            // Header
            GroupHeader93 grpHdr = new GroupHeader93();
            grpHdr.setMsgId("VALIDATION-" + System.currentTimeMillis());
            grpHdr.setCreDtTm(OffsetDateTime.now());
            grpHdr.setNbOfTxs("1"); // Número de transacciones (obligatorio en algunos validadores)

            // Settlement Info (Obligatorio para que sea un pacs.008 válido semánticamente)
            SettlementInstruction7 sttlmInf = new SettlementInstruction7();
            sttlmInf.setSttlmMtd(SettlementMethod1Code.CLRG); // Método de liquidación: Clearing
            grpHdr.setSttlmInf(sttlmInf);

            creditTransfer.setGrpHdr(grpHdr);

            // Transacción
            CreditTransferTransaction39 txInfo = new CreditTransferTransaction39();

            // ID de Pago
            PaymentIdentification7 pmtId = new PaymentIdentification7();
            pmtId.setEndToEndId("E2E-" + System.currentTimeMillis());
            pmtId.setTxId("TX-" + System.currentTimeMillis()); // Transaction ID también suele ser requerido
            txInfo.setPmtId(pmtId);

            // Deudor / Debtor (Validación de datos)
            PartyIdentification135 debtor = new PartyIdentification135();

            // --- Validar Nombre ---
            String fullName = refData.path("fullLegalName").asText();
            if (fullName != null && fullName.length() > 140) {
                throw new IllegalArgumentException("ISO Rule Violation: Name exceeds 140 chars");
            }
            debtor.setNm(fullName);

            // --- Validar Dirección ---
            PostalAddress24 address = new PostalAddress24();
            String countryCode = refData.path("countryCode").asText();

            if (countryCode != null) {
                if (!countryCode.matches("^[A-Z]{3}$") && !countryCode.matches("^[A-Z]{2}$")) {
                    throw new IllegalArgumentException("ISO Rule Violation: Invalid Country Code format");
                }
                // Ajuste a 2 letras para Prowide
                address.setCtry(countryCode.substring(0, 2));
            }

            String townName = refData.path("townName").asText();
            address.setTwnNm(townName);
            debtor.setPstlAdr(address);

            txInfo.setDbtr(debtor);

            // Monto
            ActiveCurrencyAndAmount amount = new ActiveCurrencyAndAmount();
            amount.setCcy("USD");
            amount.setValue(BigDecimal.ZERO);
            txInfo.setIntrBkSttlmAmt(amount);

            // Charge Bearer (Quién paga la comisión). Es obligatorio en pacs.008
            txInfo.setChrgBr(ChargeBearerType1Code.DEBT);

            // Ensamblaje Final
            creditTransfer.addCdtTrfTxInf(txInfo);
            mx.setFIToFICstmrCdtTrf(creditTransfer);

            // Generar XML
            String xmlResult = mx.message();

            if (xmlResult == null || xmlResult.isEmpty()) {
                throw new IllegalArgumentException("Fallo interno generando XML ISO.");
            }

            log.info("Compliance ISO 20022 Verificado Correctamente.");

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error ISO 20022: {}", e.getMessage());
            throw new IllegalArgumentException("Datos inválidos ISO 20022: " + e.getMessage());
        }
    }
}