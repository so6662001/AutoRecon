package com.autorecon.service;

import com.autorecon.domain.dto.AgreementConfirmDTO;
import com.autorecon.domain.dto.AgreementPublishDTO;
import com.autorecon.domain.entity.AgreementConfirmation;
import com.autorecon.domain.entity.AgreementVersion;
import com.autorecon.domain.vo.AgreementStatusVO;

import java.util.List;

public interface AgreementService {

    Long publishAgreement(AgreementPublishDTO dto);

    List<AgreementVersion> listVersions(Integer agreementType);

    AgreementVersion getVersion(Long versionId);

    AgreementStatusVO checkAgreementStatus(Long userId);

    void confirmAgreement(Long userId, AgreementConfirmDTO dto, String ip, String userAgent);

    AgreementVersion getCurrentVersion(Integer agreementType);

    List<AgreementConfirmation> getUserConfirmations(Long userId);

    String archiveToOss(Long versionId, String content);
}
