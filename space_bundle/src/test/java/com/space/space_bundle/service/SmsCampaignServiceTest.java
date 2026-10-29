package com.space.space_bundle.service;

import com.space.space_bundle.dto.SmsSendRequest;
import com.space.space_bundle.entity.SmsCampaign;
import com.space.space_bundle.entity.SmsSenderIdRequest;
import com.space.space_bundle.repository.SmsCampaignRepository;
import com.space.space_bundle.repository.SmsOperationsSettingsRepository;
import com.space.space_bundle.repository.SmsSenderIdRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SmsCampaignServiceTest {
    private SmsCampaignRepository campaignRepository;
    private SmsSenderIdRequestRepository senderRepository;
    private SmsOperationsSettingsRepository settingsRepository;
    private WalletService walletService;
    private MoolreSmsService moolre;
    private SmsCampaignService service;

    @BeforeEach
    void setUp() {
        campaignRepository = mock(SmsCampaignRepository.class);
        senderRepository = mock(SmsSenderIdRequestRepository.class);
        settingsRepository = mock(SmsOperationsSettingsRepository.class);
        walletService = mock(WalletService.class);
        moolre = mock(MoolreSmsService.class);
        service = new SmsCampaignService(campaignRepository, senderRepository, walletService, moolre,
                mock(MongoTemplate.class), settingsRepository);
    }

    @Test
    void insufficientSmsCreditsBlocksSendingWithoutCashDebit() {
        ready();
        approvedSender();
        when(campaignRepository.save(any(SmsCampaign.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletService.atomicSmsDebitOnce(eq("user-1"), eq(1), startsWith("sms-campaign-reserve:"))).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.send("user-1", request(List.of("233241234567"))));
        verifyNoInteractions(moolre);
        verify(walletService, never()).debit(anyString(), any(), anyString(), anyString());
        verify(campaignRepository).delete(any(SmsCampaign.class));
    }

    @Test
    void definiteProviderRejectionKeepsReservedCreditsForAdminRetry() {
        ready();
        approvedSender();
        when(walletService.atomicSmsDebitOnce(eq("user-1"), eq(1), startsWith("sms-campaign-reserve:"))).thenReturn(8);
        when(campaignRepository.save(any(SmsCampaign.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new MoolreSmsService.MoolreSmsException("ASMS_LOW", "Insufficient provider SMS bundle", true))
                .when(moolre).sendBulkSms(anyString(), anyString(), anyList(), anyList());

        SmsCampaign campaign = service.send("user-1", request(List.of("233241234567")));

        assertEquals("FAILED_NEEDS_ADMIN_ACTION", campaign.getStatus());
        assertEquals(1, campaign.getCreditsReserved());
        assertFalse(campaign.isCreditsReleased());
        assertEquals("FAILED", campaign.getDeliveryDetails().get(0).getStatus());
        verify(walletService, never()).atomicSmsCreditOnce(anyString(), anyInt(), anyString());
        verify(walletService, never()).debit(anyString(), any(), anyString(), anyString());
    }

    @Test
    void duplicateRecipientsAreRejectedBeforeCreditsAreReserved() {
        ready();
        approvedSender();
        assertThrows(IllegalArgumentException.class,
                () -> service.send("user-1", request(List.of("233241234567", "233241234567"))));
        verify(walletService, never()).atomicSmsDebitOnce(anyString(), anyInt(), anyString());
        verifyNoInteractions(moolre);
    }

    @Test
    void messageSegmentationCountsGsmExtensionAndUnicodeUnitsCorrectly() {
        assertEquals(2, MoolreSmsService.calculatePages("^".repeat(81)));
        assertEquals(2, MoolreSmsService.calculatePages("£".repeat(161)));
        assertEquals(2, MoolreSmsService.calculatePages("😀".repeat(36)));
    }

    private void ready() {
        when(settingsRepository.findById("sms")).thenReturn(Optional.of(com.space.space_bundle.entity.SmsOperationsSettings.builder()
                .id("sms").sendingEnabled(true).providerBundleReady(true).build()));
    }

    private void approvedSender() {
        when(senderRepository.findByUserIdAndSenderIdAndStatus("user-1", "MYBRAND", "APPROVED"))
                .thenReturn(Optional.of(SmsSenderIdRequest.builder().userId("user-1").senderId("MYBRAND").status("APPROVED").providerStatus("APPROVED").build()));
    }

    private SmsSendRequest request(List<String> recipients) {
        SmsSendRequest request = new SmsSendRequest();
        request.setSenderId("MYBRAND");
        request.setMessage("Hello");
        request.setRecipients(recipients);
        return request;
    }
}
