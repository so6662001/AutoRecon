package com.autorecon.service;

import com.autorecon.domain.dto.TemplateCreateDTO;
import com.autorecon.domain.entity.ReconTemplate;
import com.autorecon.mapper.ReconTemplateMapper;
import com.autorecon.service.impl.ReconTemplateServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconTemplateServiceTest {

    @InjectMocks
    private ReconTemplateServiceImpl templateService;

    @Mock
    private ReconTemplateMapper reconTemplateMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(templateService, "baseMapper", reconTemplateMapper);
    }

    @Test
    void test_createTemplate_success() {

        TemplateCreateDTO dto = new TemplateCreateDTO();
        dto.setTemplateName("Test Template");
        dto.setTemplateType(1);
        dto.setIsDefault(0);

        doAnswer(inv -> {
            ReconTemplate t = inv.getArgument(0);
            t.setId(100L);
            return null;
        }).when(reconTemplateMapper).insert(any(ReconTemplate.class));

        Long templateId = templateService.createTemplate(dto);

        assertNotNull(templateId);
        assertEquals(100L, templateId);

        ArgumentCaptor<ReconTemplate> captor = ArgumentCaptor.forClass(ReconTemplate.class);
        verify(reconTemplateMapper).insert(captor.capture());
        assertEquals("Test Template", captor.getValue().getTemplateName());
        assertNotNull(captor.getValue().getEnterpriseId());
    }

    @Test
    void test_deleteTemplate_success() {
        ReconTemplate template = ReconTemplate.builder().id(100L).templateName("Test").build();

        when(reconTemplateMapper.selectById(100L)).thenReturn(template);

        templateService.deleteTemplate(100L);

        verify(reconTemplateMapper).deleteById(100L);
    }

    @Test
    void test_getDefaultTemplate_returnsTemplateWithIsDefault1() {
        ReconTemplate template = ReconTemplate.builder()
                .id(100L)
                .enterpriseId(10L)
                .templateName("Default")
                .isDefault(1)
                .build();

        when(reconTemplateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(template);

        ReconTemplate result = templateService.getDefaultTemplate(10L);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(1, result.getIsDefault());
    }

    @Test
    void test_listTemplates_returnsTemplatesForEnterprise() {
        ReconTemplate t1 = ReconTemplate.builder().id(1L).enterpriseId(10L).build();
        ReconTemplate t2 = ReconTemplate.builder().id(2L).enterpriseId(10L).build();

        when(reconTemplateMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(t1, t2));

        List<ReconTemplate> result = templateService.listTemplates(10L);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }
}
