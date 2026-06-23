package com.lqtigee.sparkai.web;

import com.lqtigee.sparkai.dto.CodexSkillDto;
import com.lqtigee.sparkai.service.CodexSkillService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CodexSkillController {

    private final CodexSkillService codexSkillService;

    public CodexSkillController(CodexSkillService codexSkillService) {
        this.codexSkillService = codexSkillService;
    }

    @GetMapping("/api/codex/skills")
    public CodexSkillsResponse listSkills() {
        return new CodexSkillsResponse(codexSkillService.listSkills());
    }

    public record CodexSkillsResponse(List<CodexSkillDto> codexSkills) {
    }
}
