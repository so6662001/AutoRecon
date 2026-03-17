package com.autorecon.domain.listener;

import com.autorecon.domain.dto.BillItemExcelDTO;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 对账单明细读取监听器
 */
@Getter
public class BillItemExcelListener extends AnalysisEventListener<BillItemExcelDTO> {

    private final List<BillItemExcelDTO> dataList = new ArrayList<>();

    @Override
    public void invoke(BillItemExcelDTO data, AnalysisContext context) {
        dataList.add(data);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // no-op
    }
}
