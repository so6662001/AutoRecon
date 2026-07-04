package com.pickupexpress.domain.vo;

import com.pickupexpress.domain.entity.DeliveryConfirm;
import com.pickupexpress.domain.entity.DeliveryPhoto;
import com.pickupexpress.domain.entity.EvidencePackage;
import com.pickupexpress.domain.entity.PickupVerification;
import com.pickupexpress.domain.entity.SettlementOrder;
import lombok.Data;

import java.util.List;

/**
 * 提货单详情 VO
 */
@Data
public class PickupOrderDetailVO extends PickupOrderVO {

    private DeliveryConfirm deliveryConfirm;
    private List<DeliveryPhoto> photos;
    private SettlementOrder settlement;
    private PickupVerification verification;
    private EvidencePackage evidencePackage;
}
